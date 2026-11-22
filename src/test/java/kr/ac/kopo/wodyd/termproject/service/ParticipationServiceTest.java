package kr.ac.kopo.wodyd.termproject.service;

import kr.ac.kopo.wodyd.termproject.domain.ApplyStatus;
import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Party;
import kr.ac.kopo.wodyd.termproject.domain.PartyStatus;
import kr.ac.kopo.wodyd.termproject.support.ServiceTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("참가 신청 · 승인 · 거절 · 취소")
class ParticipationServiceTest extends ServiceTestBase {

    @Test
    @DisplayName("방장은 파티를 만들면 '참가 중'으로 자동 등록된다")
    void hostJoinsOwnParty() {
        Member host = member("host");
        Party party = party(host, 4);

        assertThat(entry(party, host).getStatus()).isEqualTo(ApplyStatus.APPROVED);
        assertThat(party.getStatus()).isEqualTo(PartyStatus.RECRUITING);
    }

    @Test
    @DisplayName("신청하면 '승인 대기'로 등록된다")
    void applyCreatesPendingEntry() {
        Member host = member("host");
        Member guest = member("guest");
        Party party = party(host, 4);

        participationService.apply(party.getId(), guest.getUsername());

        assertThat(entry(party, guest).getStatus()).isEqualTo(ApplyStatus.PENDING);
    }

    @Test
    @DisplayName("자기 파티에는 신청할 수 없다")
    void hostCannotApplyToOwnParty() {
        Member host = member("host");
        Party party = party(host, 4);

        assertThatThrownBy(() -> participationService.apply(party.getId(), host.getUsername()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("자신이 만든 파티");
    }

    @Test
    @DisplayName("같은 파티에 두 번 신청할 수 없다")
    void cannotApplyTwice() {
        Member host = member("host");
        Member guest = member("guest");
        Party party = party(host, 4);
        participationService.apply(party.getId(), guest.getUsername());

        assertThatThrownBy(() -> participationService.apply(party.getId(), guest.getUsername()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 신청");
    }

    @Test
    @DisplayName("모집 중이 아닌 파티(인원마감·종료)에는 신청할 수 없다")
    void cannotApplyWhenNotRecruiting() {
        Member host = member("host");
        Member first = member("first");
        Member late = member("late");
        Party party = party(host, 2);
        join(party, host, first); // 정원이 차서 인원마감

        assertThat(party.getStatus()).isEqualTo(PartyStatus.FULL);
        assertThatThrownBy(() -> participationService.apply(party.getId(), late.getUsername()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("모집 중인 파티");

        partyService.close(party.getId(), host.getUsername());
        assertThatThrownBy(() -> participationService.apply(party.getId(), late.getUsername()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("승인 인원이 정원이 되면 자동으로 인원마감이 된다")
    void approvalFillsParty() {
        Member host = member("host");
        Member a = member("a");
        Member b = member("b");
        Party party = party(host, 3); // 방장 포함 3명

        join(party, host, a);
        assertThat(party.getStatus()).isEqualTo(PartyStatus.RECRUITING);

        join(party, host, b);
        assertThat(party.getStatus()).isEqualTo(PartyStatus.FULL);
    }

    @Test
    @DisplayName("정원이 찬 뒤에는 대기 중이던 신청도 승인할 수 없다 (정원 초과 방지)")
    void cannotApproveBeyondCapacity() {
        Member host = member("host");
        Member a = member("a");
        Member b = member("b");
        Party party = party(host, 2); // 자리는 1개
        participationService.apply(party.getId(), a.getUsername());
        participationService.apply(party.getId(), b.getUsername()); // 둘 다 모집 중일 때 신청
        participationService.approve(party.getId(), entry(party, a).getId(), host.getUsername());

        assertThatThrownBy(() ->
                participationService.approve(party.getId(), entry(party, b).getId(), host.getUsername()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("가득");
        assertThat(entry(party, b).getStatus()).isEqualTo(ApplyStatus.PENDING);
    }

    @Test
    @DisplayName("참가를 취소하면 자리가 나서 다시 모집중이 된다")
    void cancelReopensParty() {
        Member host = member("host");
        Member guest = member("guest");
        Party party = party(host, 2);
        join(party, host, guest);
        assertThat(party.getStatus()).isEqualTo(PartyStatus.FULL);

        participationService.cancel(party.getId(), guest.getUsername());

        assertThat(party.getStatus()).isEqualTo(PartyStatus.RECRUITING);
        assertThat(partyMemberRepository.existsByPartyAndMember(party, guest)).isFalse();
    }

    @Test
    @DisplayName("방장은 참가를 취소할 수 없다")
    void hostCannotCancel() {
        Member host = member("host");
        Party party = party(host, 3);

        assertThatThrownBy(() -> participationService.cancel(party.getId(), host.getUsername()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("거절하면 '거절됨'이 되고 정원에는 영향이 없다")
    void rejectKeepsCapacity() {
        Member host = member("host");
        Member guest = member("guest");
        Party party = party(host, 2);
        participationService.apply(party.getId(), guest.getUsername());

        participationService.reject(party.getId(), entry(party, guest).getId(), host.getUsername());

        assertThat(entry(party, guest).getStatus()).isEqualTo(ApplyStatus.REJECTED);
        assertThat(party.getStatus()).isEqualTo(PartyStatus.RECRUITING);
    }

    @Test
    @DisplayName("방장이 아닌 사람은 승인·거절할 수 없다 (403)")
    void onlyHostCanDecide() {
        Member host = member("host");
        Member guest = member("guest");
        Member stranger = member("stranger");
        Party party = party(host, 4);
        participationService.apply(party.getId(), guest.getUsername());
        Long applicationId = entry(party, guest).getId();

        assertThatThrownBy(() -> participationService.approve(party.getId(), applicationId, stranger.getUsername()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("방장만");
        assertThatThrownBy(() -> participationService.reject(party.getId(), applicationId, guest.getUsername()))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("종료된 파티의 신청은 처리할 수 없다")
    void closedPartyLocksDecisions() {
        Member host = member("host");
        Member guest = member("guest");
        Party party = party(host, 4);
        participationService.apply(party.getId(), guest.getUsername());
        partyService.close(party.getId(), host.getUsername());

        assertThatThrownBy(() ->
                participationService.approve(party.getId(), entry(party, guest).getId(), host.getUsername()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("종료된 파티");
    }

    @Test
    @DisplayName("이미 처리한 신청은 다시 처리할 수 없다")
    void cannotDecideTwice() {
        Member host = member("host");
        Member guest = member("guest");
        Party party = party(host, 4);
        participationService.apply(party.getId(), guest.getUsername());
        Long applicationId = entry(party, guest).getId();
        participationService.reject(party.getId(), applicationId, host.getUsername());

        assertThatThrownBy(() -> participationService.approve(party.getId(), applicationId, host.getUsername()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("대기 중인 신청");
    }
}
