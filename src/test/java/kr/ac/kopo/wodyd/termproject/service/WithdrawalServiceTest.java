package kr.ac.kopo.wodyd.termproject.service;

import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Party;
import kr.ac.kopo.wodyd.termproject.domain.Role;
import kr.ac.kopo.wodyd.termproject.support.ServiceTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("회원 탈퇴")
class WithdrawalServiceTest extends ServiceTestBase {

    @Autowired
    private WithdrawalService withdrawalService;

    @Test
    @DisplayName("비밀번호가 틀리면 탈퇴되지 않는다")
    void wrongPasswordKeepsAccount() {
        Member user = member("user");

        assertThatThrownBy(() -> withdrawalService.withdraw(user.getUsername(), "wrong-password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비밀번호");
        assertThatThrownBy(() -> withdrawalService.withdraw(user.getUsername(), ""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(memberRepository.existsByUsername(user.getUsername())).isTrue();
    }

    @Test
    @DisplayName("관리자 계정은 탈퇴할 수 없다")
    void adminCannotWithdraw() {
        Member admin = member("admin", Role.ADMIN);

        assertThatThrownBy(() -> withdrawalService.withdraw(admin.getUsername(), PASSWORD))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("관리자");
        assertThat(memberRepository.existsByUsername(admin.getUsername())).isTrue();
    }

    @Test
    @DisplayName("탈퇴하면 계정과 참가 내역이 지워지고, 남긴 리뷰만큼 상대의 매너 점수가 다시 계산된다")
    void withdrawalRemovesEntriesAndRecalculatesScores() {
        Member host = member("host");
        Member leaver = member("leaver");
        Member other = member("other");
        Party party = party(host, 3);
        join(party, host, leaver);
        join(party, host, other);
        partyService.close(party.getId(), host.getUsername());
        reviewService.write(party.getId(), leaver.getUsername(), entry(party, host).getId(), 5, null);
        reviewService.write(party.getId(), other.getUsername(), entry(party, host).getId(), 3, null);
        assertThat(host.getMannerScore()).isEqualTo(4.0);

        withdrawalService.withdraw(leaver.getUsername(), PASSWORD);

        assertThat(memberRepository.existsByUsername(leaver.getUsername())).isFalse();
        assertThat(partyMemberRepository.existsByPartyAndMember(party, leaver)).isFalse();
        // 5점 리뷰가 사라졌으니 남은 리뷰(3점) 기준으로 다시 계산된다
        assertThat(host.getReviewCount()).isEqualTo(1);
        assertThat(host.getMannerScore()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("방장이 탈퇴하면 만든 파티가 함께 지워지고, 그 파티에서 받은 점수가 정리된다")
    void hostWithdrawalDeletesPartyAndItsReviews() {
        Member host = member("host");
        Member a = member("a");
        Member b = member("b");
        Party party = party(host, 3);
        join(party, host, a);
        join(party, host, b);
        partyService.close(party.getId(), host.getUsername());
        reviewService.write(party.getId(), a.getUsername(), entry(party, b).getId(), 5, null);
        reviewService.write(party.getId(), b.getUsername(), entry(party, a).getId(), 4, null);
        Long partyId = party.getId();
        assertThat(a.getReviewCount()).isEqualTo(1);
        flushAndClear();

        withdrawalService.withdraw(host.getUsername(), PASSWORD);
        flushAndClear();

        assertThat(partyRepository.findById(partyId)).isEmpty();
        Member reloadedA = memberRepository.findById(a.getId()).orElseThrow();
        Member reloadedB = memberRepository.findById(b.getId()).orElseThrow();
        assertThat(reloadedA.getReviewCount()).isZero();
        assertThat(reloadedA.getMannerScore()).isZero();
        assertThat(reloadedB.getReviewCount()).isZero();
        assertThat(reloadedB.getMannerScore()).isZero();
    }
}
