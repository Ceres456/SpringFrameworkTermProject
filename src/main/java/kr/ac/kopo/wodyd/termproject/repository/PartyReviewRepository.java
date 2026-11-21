package kr.ac.kopo.wodyd.termproject.repository;

import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Party;
import kr.ac.kopo.wodyd.termproject.domain.PartyReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartyReviewRepository extends JpaRepository<PartyReview, Long> {

    boolean existsByPartyAndReviewerAndTarget(Party party, Member reviewer, Member target);

    List<PartyReview> findByTargetOrderByCreatedAtDesc(Member target);

    List<PartyReview> findByParty(Party party);
}
