package kr.ac.kopo.wodyd.termproject.repository;

import kr.ac.kopo.wodyd.termproject.domain.ApplyStatus;
import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Party;
import kr.ac.kopo.wodyd.termproject.domain.PartyMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartyMemberRepository extends JpaRepository<PartyMember, Long> {

    boolean existsByPartyAndMember(Party party, Member member);

    Optional<PartyMember> findByPartyAndMember(Party party, Member member);

    /** 신청 순서대로, 회원 정보까지 한 번에 읽는다 (뷰에서 지연 로딩을 쓰지 않으므로). */
    @EntityGraph(attributePaths = "member")
    List<PartyMember> findByPartyOrderByAppliedAtAsc(Party party);

    /** 마이페이지: 내가 신청/참가한 내역 (최신순). 파티와 게임까지 함께 읽는다. */
    @EntityGraph(attributePaths = {"party", "party.game"})
    List<PartyMember> findByMemberOrderByAppliedAtDesc(Member member);

    long countByPartyAndStatus(Party party, ApplyStatus status);
}
