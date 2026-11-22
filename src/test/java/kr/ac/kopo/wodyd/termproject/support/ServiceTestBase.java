package kr.ac.kopo.wodyd.termproject.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kr.ac.kopo.wodyd.termproject.domain.Game;
import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Party;
import kr.ac.kopo.wodyd.termproject.domain.PartyMember;
import kr.ac.kopo.wodyd.termproject.domain.Role;
import kr.ac.kopo.wodyd.termproject.dto.PartyForm;
import kr.ac.kopo.wodyd.termproject.repository.GameRepository;
import kr.ac.kopo.wodyd.termproject.repository.MemberRepository;
import kr.ac.kopo.wodyd.termproject.repository.PartyMemberRepository;
import kr.ac.kopo.wodyd.termproject.repository.PartyRepository;
import kr.ac.kopo.wodyd.termproject.service.ParticipationService;
import kr.ac.kopo.wodyd.termproject.service.PartyService;
import kr.ac.kopo.wodyd.termproject.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 서비스 테스트 공통 바탕. 메모리 DB(H2)를 쓰고, 테스트마다 트랜잭션을 되돌려 서로 영향을 주지 않는다.
 * 회원·게임·파티를 빠르게 만드는 도우미를 가진다.
 */
@SpringBootTest
@Transactional
public abstract class ServiceTestBase {

    protected static final String PASSWORD = "secret12";

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired protected MemberRepository memberRepository;
    @Autowired protected GameRepository gameRepository;
    @Autowired protected PartyRepository partyRepository;
    @Autowired protected PartyMemberRepository partyMemberRepository;
    @Autowired protected PartyService partyService;
    @Autowired protected ParticipationService participationService;
    @Autowired protected ReviewService reviewService;
    @Autowired protected PasswordEncoder passwordEncoder;

    @PersistenceContext
    protected EntityManager em;

    /**
     * 지금까지의 변경을 DB에 반영하고 영속성 컨텍스트를 비운다.
     * 실제 요청처럼 '새로 읽은' 상태에서 삭제 같은 작업을 시험할 때 쓴다.
     * (한 트랜잭션 안에서 엔티티를 직접 만들면 파티의 참가 내역 목록이 DB와 어긋나 있을 수 있다.)
     * 비운 뒤에는 이전에 들고 있던 엔티티가 분리되므로, 결과는 repository 로 다시 읽어서 확인한다.
     */
    protected void flushAndClear() {
        em.flush();
        em.clear();
    }

    /** 이 테스트에서만 쓰는 고유 문자열 (아이디/닉네임/제목이 겹치지 않게) */
    protected static String uniq(String prefix) {
        return prefix + SEQ.incrementAndGet();
    }

    protected Member member(String prefix) {
        return member(prefix, Role.USER);
    }

    protected Member member(String prefix, Role role) {
        String name = uniq(prefix);
        return memberRepository.save(Member.builder()
                .username(name)
                .password(passwordEncoder.encode(PASSWORD))
                .nickname(name)
                .role(role)
                .build());
    }

    protected Game game() {
        return gameRepository.save(Game.builder().name(uniq("game")).genre("test").build());
    }

    /** 방장이 파티를 만든다. (방장은 자동으로 '참가 중'이 된다) */
    protected Party party(Member host, int maxMembers) {
        return party(host, maxMembers, uniq("party"));
    }

    protected Party party(Member host, int maxMembers, String title) {
        PartyForm form = new PartyForm();
        form.setGameId(game().getId());
        form.setTitle(title);
        form.setMaxMembers(maxMembers);
        Long id = partyService.create(form, host.getUsername());
        return partyRepository.findById(id).orElseThrow();
    }

    /** 신청 내역 (없으면 실패) */
    protected PartyMember entry(Party party, Member member) {
        return partyMemberRepository.findByPartyAndMember(party, member).orElseThrow();
    }

    /** 신청하고 방장이 승인한 상태로 만든다. */
    protected void join(Party party, Member host, Member guest) {
        participationService.apply(party.getId(), guest.getUsername());
        participationService.approve(party.getId(), entry(party, guest).getId(), host.getUsername());
    }
}
