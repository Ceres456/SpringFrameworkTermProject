package kr.ac.kopo.wodyd.termproject.service;

import kr.ac.kopo.wodyd.termproject.domain.ApplyStatus;
import kr.ac.kopo.wodyd.termproject.domain.Game;
import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Party;
import kr.ac.kopo.wodyd.termproject.domain.PartyMember;
import kr.ac.kopo.wodyd.termproject.domain.PartyReview;
import kr.ac.kopo.wodyd.termproject.domain.PartyStatus;
import kr.ac.kopo.wodyd.termproject.dto.ParticipantView;
import kr.ac.kopo.wodyd.termproject.dto.PartyDetail;
import kr.ac.kopo.wodyd.termproject.dto.PartyForm;
import kr.ac.kopo.wodyd.termproject.dto.PartySummary;
import kr.ac.kopo.wodyd.termproject.dto.ReviewView;
import kr.ac.kopo.wodyd.termproject.repository.GameRepository;
import kr.ac.kopo.wodyd.termproject.repository.MemberRepository;
import kr.ac.kopo.wodyd.termproject.repository.PartyMemberRepository;
import kr.ac.kopo.wodyd.termproject.repository.PartyRepository;
import kr.ac.kopo.wodyd.termproject.repository.PartyReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyService {

    private static final int PAGE_SIZE = 10;

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final PartyReviewRepository partyReviewRepository;
    private final GameRepository gameRepository;
    private final MemberRepository memberRepository;

    public List<Game> findGames() {
        // 등록 순서(= 인기 있는 게임부터 넣은 순서)대로 보여준다
        return gameRepository.findAll(Sort.by("id"));
    }

    /** 게임(선택)과 제목 키워드로 검색한 파티 목록 (최신순, 10개씩) */
    public Page<PartySummary> search(Long gameId, String keyword, int page) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        String kw = keyword == null ? "" : keyword;
        return partyRepository.search(gameId, kw, pageable).map(this::toSummary);
    }

    public PartyDetail getDetail(Long id, String username) {
        Party party = findParty(id);
        Member host = party.getHost();
        boolean mine = username != null && host.getUsername().equals(username);

        List<PartyMember> members = partyMemberRepository.findByPartyOrderByAppliedAtAsc(party);

        // 현재 로그인한 사용자의 신청 상태 (없으면 null)
        ApplyStatus myStatus = members.stream()
                .filter(pm -> pm.getMember().getUsername().equals(username))
                .map(PartyMember::getStatus)
                .findFirst()
                .orElse(null);

        boolean closed = party.getStatus() == PartyStatus.CLOSED;
        boolean viewerIsParticipant = myStatus == ApplyStatus.APPROVED;

        // 종료된 파티에만 리뷰가 있다. 내가 이미 평가한 대상은 다시 평가할 수 없다.
        List<PartyReview> partyReviews = closed
                ? partyReviewRepository.findByPartyOrderByCreatedAtDesc(party)
                : List.of();
        Set<Long> reviewedByMe = partyReviews.stream()
                .filter(r -> r.getReviewer().getUsername().equals(username))
                .map(r -> r.getTarget().getId())
                .collect(Collectors.toSet());

        List<ParticipantView> participants = members.stream()
                .filter(pm -> pm.getStatus() == ApplyStatus.APPROVED)
                .map(pm -> {
                    boolean self = pm.getMember().getUsername().equals(username);
                    boolean alreadyReviewed = reviewedByMe.contains(pm.getMember().getId());
                    return toParticipant(pm, host,
                            closed && viewerIsParticipant && !self && !alreadyReviewed,
                            !self && alreadyReviewed);
                })
                .toList();
        // 승인 대기 목록은 방장에게만 보여준다
        List<ParticipantView> applicants = mine
                ? members.stream()
                        .filter(pm -> pm.getStatus() == ApplyStatus.PENDING)
                        .map(pm -> toParticipant(pm, host, false, false))
                        .toList()
                : List.of();
        List<ReviewView> reviews = partyReviews.stream().map(this::toReviewView).toList();
        boolean canClose = mine && !closed;

        boolean canApply = username != null && !mine && myStatus == null
                && party.getStatus() == PartyStatus.RECRUITING;
        boolean canCancel = !mine && !closed
                && (myStatus == ApplyStatus.PENDING || myStatus == ApplyStatus.APPROVED);
        boolean canSeeContact = mine || myStatus == ApplyStatus.APPROVED;

        return PartyDetail.builder()
                .id(party.getId())
                .title(party.getTitle())
                .content(party.getContent())
                .gameName(party.getGame().getName())
                .hostNickname(host.getNickname())
                .hostMannerScore(host.getMannerScore())
                .hostReviewCount(host.getReviewCount())
                .maxMembers(party.getMaxMembers())
                .currentMembers(participants.size())
                .contactLink(party.getContactLink())
                .status(party.getStatus())
                .createdAt(party.getCreatedAt())
                .mine(mine)
                .canSeeContact(canSeeContact)
                .myStatus(myStatus)
                .canApply(canApply)
                .canCancel(canCancel)
                .participants(participants)
                .applicants(applicants)
                .canClose(canClose)
                .reviews(reviews)
                .build();
    }

    /** 수정 화면에 채울 기존 값. 방장만, 종료되지 않은 파티만 가능 */
    public PartyForm getForm(Long id, String username) {
        Party party = findParty(id);
        validateEditable(party, username);

        PartyForm form = new PartyForm();
        form.setGameId(party.getGame().getId());
        form.setTitle(party.getTitle());
        form.setContent(party.getContent());
        form.setMaxMembers(party.getMaxMembers());
        form.setContactLink(party.getContactLink());
        return form;
    }

    @Transactional
    public Long create(PartyForm form, String username) {
        Member host = findMember(username);
        Game game = findGame(form.getGameId());

        Party party = Party.builder()
                .title(form.getTitle().trim())
                .content(form.getContent())
                .game(game)
                .host(host)
                .maxMembers(form.getMaxMembers())
                .contactLink(blankToNull(form.getContactLink()))
                .build();
        partyRepository.save(party);

        // 방장도 참가자로 등록한다 (종료 후 리뷰 대상에 포함되도록)
        PartyMember hostMember = PartyMember.builder().party(party).member(host).build();
        hostMember.approve();
        partyMemberRepository.save(hostMember);

        return party.getId();
    }

    @Transactional
    public void update(Long id, PartyForm form, String username) {
        Party party = findParty(id);
        validateEditable(party, username);

        long approved = countApproved(party);
        if (form.getMaxMembers() < approved) {
            throw new IllegalArgumentException("현재 참가 인원(" + approved + "명)보다 적게 설정할 수 없습니다.");
        }

        Game game = findGame(form.getGameId());
        party.update(form.getTitle().trim(), form.getContent(), game, form.getMaxMembers(),
                blankToNull(form.getContactLink()));
        party.changeStatus(approved >= party.getMaxMembers() ? PartyStatus.FULL : PartyStatus.RECRUITING);
    }

    /**
     * 방장이 파티를 종료한다. 종료되면 수정/삭제/신청 처리가 막히고,
     * 승인된 참가자끼리 리뷰를 남길 수 있게 된다. (승인/취소와 겹치지 않도록 잠금)
     */
    @Transactional
    public void close(Long id, String username) {
        Party party = partyRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 파티입니다."));
        if (username == null || !party.getHost().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "방장만 종료할 수 있습니다.");
        }
        if (party.getStatus() == PartyStatus.CLOSED) {
            throw new IllegalStateException("이미 종료된 파티입니다.");
        }
        party.changeStatus(PartyStatus.CLOSED);
    }

    @Transactional
    public void delete(Long id, String username) {
        Party party = findParty(id);
        validateEditable(party, username);
        partyRepository.delete(party);
    }

    // ---------- helpers ----------

    private PartySummary toSummary(Party party) {
        return PartySummary.builder()
                .id(party.getId())
                .title(party.getTitle())
                .gameName(party.getGame().getName())
                .hostNickname(party.getHost().getNickname())
                .maxMembers(party.getMaxMembers())
                .currentMembers(countApproved(party))
                .status(party.getStatus())
                .createdAt(party.getCreatedAt())
                .build();
    }

    private long countApproved(Party party) {
        return partyMemberRepository.countByPartyAndStatus(party, ApplyStatus.APPROVED);
    }

    private ParticipantView toParticipant(PartyMember pm, Member host, boolean reviewable, boolean reviewedByMe) {
        Member member = pm.getMember();
        return ParticipantView.builder()
                .id(pm.getId())
                .nickname(member.getNickname())
                .mannerScore(member.getMannerScore())
                .reviewCount(member.getReviewCount())
                .mainGame(member.getMainGame())
                .host(member.getId().equals(host.getId()))
                .appliedAt(pm.getAppliedAt())
                .reviewable(reviewable)
                .reviewedByMe(reviewedByMe)
                .build();
    }

    private ReviewView toReviewView(PartyReview review) {
        return ReviewView.builder()
                .reviewerNickname(review.getReviewer().getNickname())
                .targetNickname(review.getTarget().getNickname())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }

    /** 방장 본인만, 그리고 종료되지 않은 파티만 수정/삭제할 수 있다. */
    private void validateEditable(Party party, String username) {
        if (username == null || !party.getHost().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "방장만 수정/삭제할 수 있습니다.");
        }
        if (party.getStatus() == PartyStatus.CLOSED) {
            throw new IllegalStateException("종료된 파티는 수정/삭제할 수 없습니다.");
        }
    }

    private Party findParty(Long id) {
        return partyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 파티입니다."));
    }

    private Game findGame(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게임입니다."));
    }

    private Member findMember(String username) {
        return memberRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
