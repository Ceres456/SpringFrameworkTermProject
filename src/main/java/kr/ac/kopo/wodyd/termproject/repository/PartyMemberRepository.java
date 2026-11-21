package kr.ac.kopo.wodyd.termproject.repository;

import kr.ac.kopo.wodyd.termproject.domain.ApplyStatus;
import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Party;
import kr.ac.kopo.wodyd.termproject.domain.PartyMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartyMemberRepository extends JpaRepository<PartyMember, Long> {

    boolean existsByPartyAndMember(Party party, Member member);

    Optional<PartyMember> findByPartyAndMember(Party party, Member member);

    List<PartyMember> findByParty(Party party);

    List<PartyMember> findByMember(Member member);

    long countByPartyAndStatus(Party party, ApplyStatus status);
}
