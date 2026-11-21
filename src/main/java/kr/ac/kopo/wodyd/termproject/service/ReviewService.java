package kr.ac.kopo.wodyd.termproject.service;

import kr.ac.kopo.wodyd.termproject.domain.ApplyStatus;
import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Party;
import kr.ac.kopo.wodyd.termproject.domain.PartyMember;
import kr.ac.kopo.wodyd.termproject.domain.PartyReview;
import kr.ac.kopo.wodyd.termproject.domain.PartyStatus;
import kr.ac.kopo.wodyd.termproject.repository.MemberRepository;
import kr.ac.kopo.wodyd.termproject.repository.PartyMemberRepository;
import kr.ac.kopo.wodyd.termproject.repository.PartyRepository;
import kr.ac.kopo.wodyd.termproject.repository.PartyReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 종료된 파티에서 승인된 참가자끼리 남기는 매너 리뷰.
 * 규칙: 종료 후에만 / 참가자끼리만 / 자기 자신 제외 / 같은 파티·같은 대상에 1회.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private static final int MAX_COMMENT_LENGTH = 200;

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final PartyReviewRepository partyReviewRepository;
    private final MemberRepository memberRepository;

    /**
     * @param participantId 평가 대상의 PartyMember id (같은 파티의 승인된 참가자여야 함)
     */
    public void write(Long partyId, String reviewerUsername, Long participantId, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("별점은 1~5점 중에서 선택하세요.");
        }
        String trimmedComment = comment == null || comment.isBlank() ? null : comment.trim();
        if (trimmedComment != null && trimmedComment.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("한줄평은 " + MAX_COMMENT_LENGTH + "자 이내로 입력하세요.");
        }

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 파티입니다."));
        if (party.getStatus() != PartyStatus.CLOSED) {
            throw new IllegalStateException("파티가 종료된 뒤에만 리뷰를 남길 수 있습니다.");
        }

        Member reviewer = memberRepository.findByUsername(reviewerUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
        boolean reviewerJoined = partyMemberRepository.findByPartyAndMember(party, reviewer)
                .map(pm -> pm.getStatus() == ApplyStatus.APPROVED)
                .orElse(false);
        if (!reviewerJoined) {
            throw new IllegalStateException("이 파티에 참가한 사람만 리뷰를 남길 수 있습니다.");
        }

        PartyMember targetEntry = partyMemberRepository.findById(participantId)
                .filter(pm -> pm.getParty().getId().equals(partyId))
                .filter(pm -> pm.getStatus() == ApplyStatus.APPROVED)
                .orElseThrow(() -> new IllegalArgumentException("리뷰 대상이 올바르지 않습니다."));
        if (targetEntry.getMember().getId().equals(reviewer.getId())) {
            throw new IllegalStateException("자기 자신은 평가할 수 없습니다.");
        }

        // 같은 사람에게 동시에 리뷰가 들어와도 평균이 덮어써지지 않도록 대상 회원 행을 잠근다.
        Member target = memberRepository.findByIdForUpdate(targetEntry.getMember().getId())
                .orElseThrow(() -> new IllegalArgumentException("리뷰 대상이 올바르지 않습니다."));

        if (partyReviewRepository.existsByPartyAndReviewerAndTarget(party, reviewer, target)) {
            throw new IllegalStateException("이미 리뷰를 남긴 파티원입니다.");
        }

        partyReviewRepository.save(PartyReview.builder()
                .party(party)
                .reviewer(reviewer)
                .target(target)
                .rating(rating)
                .comment(trimmedComment)
                .build());
        target.applyReview(rating);
    }
}
