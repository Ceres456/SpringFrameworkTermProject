package kr.ac.kopo.wodyd.termproject.service;

import kr.ac.kopo.wodyd.termproject.domain.ApplyStatus;
import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Party;
import kr.ac.kopo.wodyd.termproject.domain.PartyMember;
import kr.ac.kopo.wodyd.termproject.domain.PartyStatus;
import kr.ac.kopo.wodyd.termproject.repository.MemberRepository;
import kr.ac.kopo.wodyd.termproject.repository.PartyMemberRepository;
import kr.ac.kopo.wodyd.termproject.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 파티 참가 신청 흐름: 신청 -> (방장) 승인/거절, 신청자 취소.
 * 업무 규칙을 어기면 IllegalStateException 으로 사유를 알린다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ParticipationService {

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final MemberRepository memberRepository;

    /** 참가 신청 (PENDING 으로 등록) */
    public void apply(Long partyId, String username) {
        Party party = findParty(partyId);
        Member member = findMember(username);

        if (party.getStatus() != PartyStatus.RECRUITING) {
            throw new IllegalStateException("모집 중인 파티에만 신청할 수 있습니다.");
        }
        if (party.getHost().getId().equals(member.getId())) {
            throw new IllegalStateException("자신이 만든 파티에는 신청할 수 없습니다.");
        }
        if (partyMemberRepository.existsByPartyAndMember(party, member)) {
            throw new IllegalStateException("이미 신청한 파티입니다.");
        }

        partyMemberRepository.save(PartyMember.builder().party(party).member(member).build());
    }

    /** 신청자 본인이 신청/참가를 취소한다. 정원이 비면 다시 모집중으로 돌린다. */
    public void cancel(Long partyId, String username) {
        Party party = findPartyForUpdate(partyId);
        Member member = findMember(username);

        if (party.getHost().getId().equals(member.getId())) {
            throw new IllegalStateException("방장은 참가를 취소할 수 없습니다. 파티를 삭제해 주세요.");
        }
        if (party.getStatus() == PartyStatus.CLOSED) {
            throw new IllegalStateException("종료된 파티는 취소할 수 없습니다.");
        }

        PartyMember application = partyMemberRepository.findByPartyAndMember(party, member)
                .orElseThrow(() -> new IllegalStateException("신청 내역이 없습니다."));

        long approved = countApproved(party);
        if (application.getStatus() == ApplyStatus.APPROVED) {
            approved--;
        }
        partyMemberRepository.delete(application);
        refreshStatus(party, approved);
    }

    /** 방장이 신청을 승인한다. 정원이 차면 인원마감으로 바꾼다. */
    public void approve(Long partyId, Long applicationId, String username) {
        Party party = findPartyForUpdate(partyId);
        validateHost(party, username);
        PartyMember application = findPendingApplication(party, applicationId);

        long approved = countApproved(party);
        if (approved >= party.getMaxMembers()) {
            throw new IllegalStateException("모집 인원이 가득 찼습니다.");
        }

        application.approve();
        refreshStatus(party, approved + 1);
    }

    /** 방장이 신청을 거절한다. */
    public void reject(Long partyId, Long applicationId, String username) {
        Party party = findPartyForUpdate(partyId);
        validateHost(party, username);
        findPendingApplication(party, applicationId).reject();
    }

    // ---------- helpers ----------

    private PartyMember findPendingApplication(Party party, Long applicationId) {
        if (party.getStatus() == PartyStatus.CLOSED) {
            throw new IllegalStateException("종료된 파티의 신청은 처리할 수 없습니다.");
        }
        PartyMember application = partyMemberRepository.findById(applicationId)
                .filter(pm -> pm.getParty().getId().equals(party.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 신청입니다."));
        if (application.getStatus() != ApplyStatus.PENDING) {
            throw new IllegalStateException("대기 중인 신청만 처리할 수 있습니다.");
        }
        return application;
    }

    /** 승인된 인원 수에 맞춰 모집중/인원마감을 맞춘다 (종료된 파티는 건드리지 않음). */
    private void refreshStatus(Party party, long approvedCount) {
        if (party.getStatus() == PartyStatus.CLOSED) {
            return;
        }
        party.changeStatus(approvedCount >= party.getMaxMembers() ? PartyStatus.FULL : PartyStatus.RECRUITING);
    }

    private long countApproved(Party party) {
        return partyMemberRepository.countByPartyAndStatus(party, ApplyStatus.APPROVED);
    }

    private void validateHost(Party party, String username) {
        if (!party.getHost().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "방장만 처리할 수 있습니다.");
        }
    }

    private Party findParty(Long id) {
        return partyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 파티입니다."));
    }

    private Party findPartyForUpdate(Long id) {
        return partyRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 파티입니다."));
    }

    private Member findMember(String username) {
        return memberRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
    }
}
