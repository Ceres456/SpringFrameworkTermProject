package kr.ac.kopo.wodyd.termproject.service;

import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Party;
import kr.ac.kopo.wodyd.termproject.domain.PartyMember;
import kr.ac.kopo.wodyd.termproject.domain.PartyReview;
import kr.ac.kopo.wodyd.termproject.domain.PartyStatus;
import kr.ac.kopo.wodyd.termproject.domain.Role;
import kr.ac.kopo.wodyd.termproject.dto.AdminStats;
import kr.ac.kopo.wodyd.termproject.repository.GameRepository;
import kr.ac.kopo.wodyd.termproject.repository.MemberRepository;
import kr.ac.kopo.wodyd.termproject.repository.PartyMemberRepository;
import kr.ac.kopo.wodyd.termproject.repository.PartyRepository;
import kr.ac.kopo.wodyd.termproject.repository.PartyReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 관리자 기능: 통계, 회원/파티/리뷰 관리.
 * 회원이나 파티를 지우면 그에 딸린 참가 내역·리뷰도 함께 지우고,
 * 리뷰를 잃은 사람의 매너 점수는 남은 리뷰 기준으로 다시 계산한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private static final int MEMBER_PAGE_SIZE = 15;
    private static final int REVIEW_PAGE_SIZE = 15;

    private final MemberRepository memberRepository;
    private final GameRepository gameRepository;
    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final PartyReviewRepository partyReviewRepository;

    // ---------- 대시보드 ----------

    public AdminStats stats() {
        return AdminStats.builder()
                .memberCount(memberRepository.count())
                .adminCount(memberRepository.countByRole(Role.ADMIN))
                .gameCount(gameRepository.count())
                .partyCount(partyRepository.count())
                .recruitingCount(partyRepository.countByStatus(PartyStatus.RECRUITING))
                .fullCount(partyRepository.countByStatus(PartyStatus.FULL))
                .closedCount(partyRepository.countByStatus(PartyStatus.CLOSED))
                .reviewCount(partyReviewRepository.count())
                .build();
    }

    public List<Member> recentMembers() {
        return memberRepository.findTop5ByOrderByCreatedAtDesc();
    }

    // ---------- 회원 ----------

    /** 아이디 또는 닉네임에 검색어가 들어간 회원 (최근 가입순) */
    public Page<Member> members(String keyword, int page) {
        String kw = keyword == null ? "" : keyword.trim();
        return memberRepository.findByUsernameContainingOrNicknameContaining(kw, kw,
                PageRequest.of(Math.max(page, 0), MEMBER_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /** 회원 한 명의 기본 정보 (상세 화면용). 비밀번호는 화면에 쓰지 않는다. */
    public Member member(Long id) {
        return findMember(id);
    }

    /** 그 회원이 남긴 리뷰 수 */
    public long writtenReviewCount(Member member) {
        return partyReviewRepository.countByReviewer(member);
    }

    /** 권한을 바꾼다. 자기 자신은 바꿀 수 없고, 마지막 관리자는 내릴 수 없다. */
    @Transactional
    public Member changeRole(Long memberId, Role role, String actingUsername) {
        Member member = findMember(memberId);
        if (member.getUsername().equals(actingUsername)) {
            throw new IllegalStateException("자기 자신의 권한은 바꿀 수 없습니다.");
        }
        if (member.getRole() == Role.ADMIN && role != Role.ADMIN
                && memberRepository.countByRole(Role.ADMIN) <= 1) {
            throw new IllegalStateException("마지막 관리자는 일반 회원으로 내릴 수 없습니다.");
        }
        member.changeRole(role);
        return member;
    }

    /**
     * 회원을 지운다. 그 회원이 만든 파티(와 그 파티의 리뷰), 참가 내역, 남긴/받은 리뷰도 함께 지운다.
     * 관리자 계정과 자기 자신은 지울 수 없다.
     */
    @Transactional
    public String deleteMember(Long memberId, String actingUsername) {
        Member member = findMember(memberId);
        if (member.getUsername().equals(actingUsername)) {
            throw new IllegalStateException("자기 자신은 삭제할 수 없습니다.");
        }
        if (member.getRole() == Role.ADMIN) {
            throw new IllegalStateException("관리자 계정은 먼저 일반 회원으로 내린 뒤에 삭제할 수 있습니다.");
        }
        return purgeMember(member);
    }

    /**
     * 회원과 그에 딸린 데이터(만든 파티와 그 리뷰, 참가 내역, 남긴/받은 리뷰)를 모두 지우고,
     * 리뷰를 잃은 다른 회원의 매너 점수를 다시 계산한다. 관리자 삭제와 본인 탈퇴가 함께 쓴다.
     * 누구를 지워도 되는지(자기 자신, 관리자 등)는 호출하는 쪽에서 먼저 검사한다.
     */
    @Transactional
    public String purgeMember(Member member) {
        String nickname = member.getNickname();

        // 1) 이 회원이 만든 파티: 파티의 리뷰와 참가 내역까지 정리
        Set<Member> affected = new LinkedHashSet<>();
        for (Party party : partyRepository.findByHostOrderByCreatedAtDesc(member)) {
            removeParty(party, affected);
        }
        // 2) 다른 파티에 신청/참가한 내역
        List<PartyMember> entries = partyMemberRepository.findByMemberOrderByAppliedAtDesc(member);
        partyMemberRepository.deleteAll(entries);
        // 3) 이 회원이 남긴 리뷰 (받은 사람의 점수가 바뀐다)
        List<PartyReview> written = partyReviewRepository.findByReviewer(member);
        written.forEach(r -> affected.add(r.getTarget()));
        partyReviewRepository.deleteAll(written);
        // 4) 이 회원이 받은 리뷰는 회원과 함께 사라진다
        partyReviewRepository.deleteAll(partyReviewRepository.findByTargetOrderByCreatedAtDesc(member));
        partyReviewRepository.flush();

        memberRepository.delete(member);
        affected.remove(member);
        affected.forEach(this::recalculateManner);
        return nickname;
    }

    // ---------- 파티 ----------

    /** 방장이 아니어도 파티를 강제로 종료한다. 이미 종료된 파티는 그대로 둔다. */
    @Transactional
    public String forceClose(Long partyId) {
        Party party = findParty(partyId);
        if (party.getStatus() == PartyStatus.CLOSED) {
            throw new IllegalStateException("이미 종료된 파티입니다.");
        }
        party.changeStatus(PartyStatus.CLOSED);
        return party.getTitle();
    }

    /** 상태와 상관없이 파티를 지운다. 그 파티에서 오간 리뷰도 함께 지우고 점수를 다시 계산한다. */
    @Transactional
    public String deleteParty(Long partyId) {
        Party party = findParty(partyId);
        String title = party.getTitle();
        Set<Member> affected = new LinkedHashSet<>();
        removeParty(party, affected);
        partyReviewRepository.flush();
        affected.forEach(this::recalculateManner);
        return title;
    }

    // ---------- 리뷰 ----------

    public Page<PartyReview> reviews(int page) {
        return partyReviewRepository.findAllForAdmin(PageRequest.of(Math.max(page, 0), REVIEW_PAGE_SIZE));
    }

    /** 리뷰 한 건을 지우고 받은 사람의 매너 점수를 다시 계산한다. */
    @Transactional
    public String deleteReview(Long reviewId) {
        PartyReview review = partyReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 리뷰입니다."));
        Member target = review.getTarget();
        String nickname = target.getNickname();
        partyReviewRepository.delete(review);
        partyReviewRepository.flush();
        recalculateManner(target);
        return nickname;
    }

    // ---------- helpers ----------

    /** 파티의 리뷰를 지우고 파티를 지운다 (참가 내역은 Party 의 cascade 로 함께 지워진다). */
    private void removeParty(Party party, Set<Member> affected) {
        List<PartyReview> reviews = partyReviewRepository.findByPartyOrderByCreatedAtDesc(party);
        reviews.forEach(r -> affected.add(r.getTarget()));
        partyReviewRepository.deleteAll(reviews);
        partyReviewRepository.flush();
        partyRepository.delete(party);
        partyRepository.flush();
    }

    /** 남아 있는 리뷰로 평균 매너 점수와 평가 수를 다시 계산한다 (applyReview 와 같은 반올림). */
    private void recalculateManner(Member target) {
        List<PartyReview> remaining = partyReviewRepository.findByTargetOrderByCreatedAtDesc(target);
        if (remaining.isEmpty()) {
            target.resetReviewStats(0.0, 0);
            return;
        }
        double average = remaining.stream().mapToInt(PartyReview::getRating).average().orElse(0.0);
        target.resetReviewStats(Math.round(average * 10) / 10.0, remaining.size());
    }

    private Member findMember(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."));
    }

    private Party findParty(Long id) {
        return partyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 파티입니다."));
    }
}
