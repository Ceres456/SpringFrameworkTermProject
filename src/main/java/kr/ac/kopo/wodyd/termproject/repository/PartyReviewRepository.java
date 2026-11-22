package kr.ac.kopo.wodyd.termproject.repository;

import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Party;
import kr.ac.kopo.wodyd.termproject.domain.PartyReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PartyReviewRepository extends JpaRepository<PartyReview, Long> {

    boolean existsByPartyAndReviewerAndTarget(Party party, Member reviewer, Member target);

    /** 한 파티에서 오간 리뷰 (작성자/대상 닉네임을 화면에 쓰므로 함께 읽는다). */
    @EntityGraph(attributePaths = {"reviewer", "target"})
    List<PartyReview> findByPartyOrderByCreatedAtDesc(Party party);

    /** 한 회원이 받은 리뷰 (마이페이지용). */
    @EntityGraph(attributePaths = {"reviewer", "party"})
    List<PartyReview> findByTargetOrderByCreatedAtDesc(Member target);

    /** 관리자: 한 회원이 남긴 리뷰 (회원을 지울 때 상대방 점수를 다시 계산하려고 쓴다). */
    @EntityGraph(attributePaths = "target")
    List<PartyReview> findByReviewer(Member reviewer);

    /** 관리자: 한 회원이 남긴 리뷰 수 */
    long countByReviewer(Member reviewer);

    /** 관리자: 모든 리뷰 (최신순). 작성자·대상·파티를 함께 읽는다. */
    @Query(value = "select r from PartyReview r join fetch r.party join fetch r.reviewer join fetch r.target " +
            "order by r.createdAt desc",
            countQuery = "select count(r) from PartyReview r")
    Page<PartyReview> findAllForAdmin(Pageable pageable);
}
