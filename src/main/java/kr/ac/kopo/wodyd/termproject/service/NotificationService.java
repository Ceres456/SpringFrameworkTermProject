package kr.ac.kopo.wodyd.termproject.service;

import kr.ac.kopo.wodyd.termproject.domain.ApplyStatus;
import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.dto.NoticeView;
import kr.ac.kopo.wodyd.termproject.repository.MemberRepository;
import kr.ac.kopo.wodyd.termproject.repository.PartyMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 알림은 따로 저장하지 않고 신청 내역에서 만든다.
 * - 내 신청의 승인/거절 결과: 확인하기 전까지 (PartyMember.resultSeen == false)
 * - 내 파티에 들어온 신청: 방장이 처리하기 전까지 (status == PENDING)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final MemberRepository memberRepository;
    private final PartyMemberRepository partyMemberRepository;

    /** 헤더 배지에 보여 줄 숫자 */
    public long count(String username) {
        return memberRepository.findByUsername(username)
                .map(member -> partyMemberRepository.countUnseenResults(member)
                        + partyMemberRepository.countIncomingByHost(member, ApplyStatus.PENDING))
                .orElse(0L);
    }

    /** 마이페이지 '새 소식' 목록 (최근 것부터) */
    public List<NoticeView> list(String username) {
        Member member = memberRepository.findByUsername(username).orElse(null);
        if (member == null) {
            return List.of();
        }

        List<NoticeView> notices = new ArrayList<>();
        partyMemberRepository.findUnseenResults(member).forEach(pm -> notices.add(NoticeView.builder()
                .kind(pm.getStatus() == ApplyStatus.APPROVED ? NoticeView.Kind.APPROVED : NoticeView.Kind.REJECTED)
                .partyId(pm.getParty().getId())
                .partyTitle(pm.getParty().getTitle())
                .occurredAt(pm.getDecidedAt())
                .build()));
        partyMemberRepository.findIncomingByHost(member, ApplyStatus.PENDING).forEach(pm -> notices.add(NoticeView.builder()
                .kind(NoticeView.Kind.INCOMING)
                .partyId(pm.getParty().getId())
                .partyTitle(pm.getParty().getTitle())
                .applicantNickname(pm.getMember().getNickname())
                .occurredAt(pm.getAppliedAt())
                .build()));

        notices.sort(Comparator.comparing(NoticeView::getOccurredAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return notices;
    }

    /** 받은 승인/거절 결과를 모두 읽음으로 표시한다. (들어온 신청은 처리하면 저절로 사라진다) */
    @Transactional
    public void markResultsSeen(String username) {
        memberRepository.findByUsername(username).ifPresent(partyMemberRepository::markResultsSeen);
    }
}
