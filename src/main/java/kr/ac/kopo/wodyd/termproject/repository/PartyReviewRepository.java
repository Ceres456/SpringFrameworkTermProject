package kr.ac.kopo.wodyd.termproject.repository;

import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Party;
import kr.ac.kopo.wodyd.termproject.domain.PartyReview;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartyReviewRepository extends JpaRepository<PartyReview, Long> {

    boolean existsByPartyAndReviewerAndTarget(Party party, Member reviewer, Member target);

    /** 한 파티에서 오간 리뷰 (작성자/대상 닉네임을 화면에 쓰므로 함께 읽는다). */
    @EntityGraph(attributePaths = {"reviewer", "target"})
    List<PartyReview> findByPartyOrderByCreatedAtDesc(Party party);

    /** 한 회원이 받은 리뷰 (마이페이지용). */
    @EntityGraph(attributePaths = {"reviewer", "party"})
    List<PartyReview> findByTargetOrderByCreatedAtDesc(Member target);
}
