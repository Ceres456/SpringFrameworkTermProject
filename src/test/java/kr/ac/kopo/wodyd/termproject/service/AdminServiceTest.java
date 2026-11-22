package kr.ac.kopo.wodyd.termproject.service;

import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Party;
import kr.ac.kopo.wodyd.termproject.domain.PartyReview;
import kr.ac.kopo.wodyd.termproject.domain.PartyStatus;
import kr.ac.kopo.wodyd.termproject.domain.Role;
import kr.ac.kopo.wodyd.termproject.repository.PartyReviewRepository;
import kr.ac.kopo.wodyd.termproject.support.ServiceTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("관리자 기능")
class AdminServiceTest extends ServiceTestBase {

    @Autowired
    private AdminService adminService;
    @Autowired
    private PartyReviewRepository partyReviewRepository;

    @Test
    @DisplayName("권한 부여: 다른 회원을 관리자로 올리고 내릴 수 있다")
    void promoteAndDemote() {
        Member admin = member("admin", Role.ADMIN);
        Member user = member("user");

        adminService.changeRole(user.getId(), Role.ADMIN, admin.getUsername());
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);

        adminService.changeRole(user.getId(), Role.USER, admin.getUsername());
        assertThat(user.getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("자기 자신의 권한은 바꿀 수 없다")
    void cannotChangeOwnRole() {
        Member admin = member("admin", Role.ADMIN);

        assertThatThrownBy(() -> adminService.changeRole(admin.getId(), Role.USER, admin.getUsername()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("자기 자신");
    }

    @Test
    @DisplayName("마지막 관리자는 일반 회원으로 내릴 수 없다")
    void cannotDemoteLastAdmin() {
        Member onlyAdmin = member("admin", Role.ADMIN);
        Member actor = member("actor");

        assertThatThrownBy(() -> adminService.changeRole(onlyAdmin.getId(), Role.USER, actor.getUsername()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("마지막 관리자");
        assertThat(onlyAdmin.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("관리자 계정과 자기 자신은 회원 삭제로 지울 수 없다")
    void cannotDeleteAdminOrSelf() {
        Member admin = member("admin", Role.ADMIN);
        Member otherAdmin = member("admin2", Role.ADMIN);

        assertThatThrownBy(() -> adminService.deleteMember(otherAdmin.getId(), admin.getUsername()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> adminService.deleteMember(admin.getId(), admin.getUsername()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("회원을 지우면 그 회원의 파티와 참가 내역도 함께 지워진다")
    void deleteMemberCascades() {
        Member admin = member("admin", Role.ADMIN);
        Member host = member("host");
        Member guest = member("guest");
        Party party = party(host, 4);
        join(party, host, guest);
        Long partyId = party.getId();
        flushAndClear();

        adminService.deleteMember(host.getId(), admin.getUsername());

        assertThat(memberRepository.existsByUsername(host.getUsername())).isFalse();
        assertThat(partyRepository.findById(partyId)).isEmpty();
        assertThat(memberRepository.existsByUsername(guest.getUsername())).isTrue();
    }

    @Test
    @DisplayName("파티 강제 종료: 방장이 아니어도 종료할 수 있고, 이미 종료된 파티는 거부한다")
    void forceClose() {
        Member host = member("host");
        Party party = party(host, 4);

        adminService.forceClose(party.getId());
        assertThat(party.getStatus()).isEqualTo(PartyStatus.CLOSED);

        assertThatThrownBy(() -> adminService.forceClose(party.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 종료");
    }

    @Test
    @DisplayName("리뷰를 지우면 받은 사람의 매너 점수가 남은 리뷰로 다시 계산된다")
    void deleteReviewRecalculatesScore() {
        Member host = member("host");
        Member a = member("a");
        Member b = member("b");
        Party party = party(host, 3);
        join(party, host, a);
        join(party, host, b);
        partyService.close(party.getId(), host.getUsername());
        reviewService.write(party.getId(), a.getUsername(), entry(party, host).getId(), 5, null);
        reviewService.write(party.getId(), b.getUsername(), entry(party, host).getId(), 2, null);
        assertThat(host.getMannerScore()).isEqualTo(3.5);
        PartyReview fiveStar = partyReviewRepository.findByTargetOrderByCreatedAtDesc(host).stream()
                .filter(r -> r.getRating() == 5).findFirst().orElseThrow();

        adminService.deleteReview(fiveStar.getId());

        assertThat(host.getReviewCount()).isEqualTo(1);
        assertThat(host.getMannerScore()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("리뷰를 모두 지우면 매너 점수는 0점, 평가 수는 0명이 된다")
    void deletingLastReviewResetsScore() {
        Member host = member("host");
        Member a = member("a");
        Party party = party(host, 2);
        join(party, host, a);
        partyService.close(party.getId(), host.getUsername());
        reviewService.write(party.getId(), a.getUsername(), entry(party, host).getId(), 4, null);
        PartyReview review = partyReviewRepository.findByTargetOrderByCreatedAtDesc(host).get(0);

        adminService.deleteReview(review.getId());

        assertThat(host.getReviewCount()).isZero();
        assertThat(host.getMannerScore()).isZero();
    }

    @Test
    @DisplayName("파티를 지우면 그 파티에서 오간 리뷰도 지워지고 점수가 다시 계산된다")
    void deletePartyRemovesReviews() {
        Member host = member("host");
        Member a = member("a");
        Party party = party(host, 2);
        join(party, host, a);
        partyService.close(party.getId(), host.getUsername());
        reviewService.write(party.getId(), a.getUsername(), entry(party, host).getId(), 5, null);
        Long partyId = party.getId();
        flushAndClear();

        adminService.deleteParty(partyId);
        flushAndClear();

        assertThat(partyRepository.findById(partyId)).isEmpty();
        Member reloaded = memberRepository.findById(host.getId()).orElseThrow();
        assertThat(reloaded.getReviewCount()).isZero();
        assertThat(reloaded.getMannerScore()).isZero();
    }
}
