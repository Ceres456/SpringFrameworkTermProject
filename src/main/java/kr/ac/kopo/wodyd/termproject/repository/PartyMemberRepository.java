package kr.ac.kopo.wodyd.termproject.repository;

import kr.ac.kopo.wodyd.termproject.domain.ApplyStatus;
import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Party;
import kr.ac.kopo.wodyd.termproject.domain.PartyMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // ---- 알림 ----

    /** 내가 신청한 것 중 아직 확인하지 않은 승인/거절 결과 (최근 처리 순) */
    @EntityGraph(attributePaths = {"party"})
    @Query("select pm from PartyMember pm where pm.member = :member and pm.resultSeen = false " +
            "order by pm.decidedAt desc")
    List<PartyMember> findUnseenResults(@Param("member") Member member);

    @Query("select count(pm) from PartyMember pm where pm.member = :member and pm.resultSeen = false")
    long countUnseenResults(@Param("member") Member member);

    /** 내가 방장인 파티에 들어온, 아직 처리하지 않은 신청 (최근 신청 순) */
    @EntityGraph(attributePaths = {"party", "member"})
    @Query("select pm from PartyMember pm where pm.party.host = :host and pm.status = :status " +
            "order by pm.appliedAt desc")
    List<PartyMember> findIncomingByHost(@Param("host") Member host, @Param("status") ApplyStatus status);

    @Query("select count(pm) from PartyMember pm where pm.party.host = :host and pm.status = :status")
    long countIncomingByHost(@Param("host") Member host, @Param("status") ApplyStatus status);

    /** 받은 결과를 모두 '확인함'으로 표시한다. */
    @Modifying(clearAutomatically = true)
    @Query("update PartyMember pm set pm.resultSeen = true where pm.member = :member and pm.resultSeen = false")
    int markResultsSeen(@Param("member") Member member);
}
