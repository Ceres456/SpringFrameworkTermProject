package kr.ac.kopo.wodyd.termproject.service;

import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Party;
import kr.ac.kopo.wodyd.termproject.dto.NoticeView;
import kr.ac.kopo.wodyd.termproject.support.ServiceTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("알림 (새 소식)")
class NotificationServiceTest extends ServiceTestBase {

    @Autowired
    private NotificationService notificationService;

    @Test
    @DisplayName("파티를 만든 것만으로는 알림이 생기지 않는다 (방장 자동 참가는 조용히 처리)")
    void creatingPartyMakesNoNotice() {
        Member host = member("host");
        party(host, 4);

        assertThat(notificationService.count(host.getUsername())).isZero();
        assertThat(notificationService.list(host.getUsername())).isEmpty();
    }

    @Test
    @DisplayName("새 신청이 들어오면 방장에게 알림이 오고, 처리하면 사라진다")
    void hostSeesIncomingApplicationsUntilDecided() {
        Member host = member("host");
        Member a = member("a");
        Member b = member("b");
        Party party = party(host, 4);

        participationService.apply(party.getId(), a.getUsername());
        participationService.apply(party.getId(), b.getUsername());

        assertThat(notificationService.count(host.getUsername())).isEqualTo(2);
        List<NoticeView> notices = notificationService.list(host.getUsername());
        assertThat(notices).hasSize(2)
                .allSatisfy(n -> {
                    assertThat(n.getKind()).isEqualTo(NoticeView.Kind.INCOMING);
                    assertThat(n.getPartyId()).isEqualTo(party.getId());
                });
        assertThat(notices).extracting(NoticeView::getApplicantNickname)
                .containsExactlyInAnyOrder(a.getNickname(), b.getNickname());

        participationService.approve(party.getId(), entry(party, a).getId(), host.getUsername());
        participationService.reject(party.getId(), entry(party, b).getId(), host.getUsername());

        assertThat(notificationService.count(host.getUsername())).isZero();
    }

    @Test
    @DisplayName("승인되면 신청자에게 '승인' 알림이 오고, 읽음 처리하면 사라진다")
    void applicantSeesApprovalUntilMarkedRead() {
        Member host = member("host");
        Member guest = member("guest");
        Party party = party(host, 4);
        participationService.apply(party.getId(), guest.getUsername());
        assertThat(notificationService.count(guest.getUsername())).isZero(); // 신청만으로는 아직 없음

        participationService.approve(party.getId(), entry(party, guest).getId(), host.getUsername());

        assertThat(notificationService.count(guest.getUsername())).isEqualTo(1);
        List<NoticeView> notices = notificationService.list(guest.getUsername());
        assertThat(notices).singleElement().satisfies(n -> {
            assertThat(n.getKind()).isEqualTo(NoticeView.Kind.APPROVED);
            assertThat(n.getPartyId()).isEqualTo(party.getId());
            assertThat(n.getOccurredAt()).isNotNull();
        });

        notificationService.markResultsSeen(guest.getUsername());

        assertThat(notificationService.count(guest.getUsername())).isZero();
        assertThat(notificationService.list(guest.getUsername())).isEmpty();
    }

    @Test
    @DisplayName("거절되면 신청자에게 '거절' 알림이 온다")
    void applicantSeesRejection() {
        Member host = member("host");
        Member guest = member("guest");
        Party party = party(host, 4);
        participationService.apply(party.getId(), guest.getUsername());

        participationService.reject(party.getId(), entry(party, guest).getId(), host.getUsername());

        assertThat(notificationService.list(guest.getUsername()))
                .singleElement()
                .satisfies(n -> assertThat(n.getKind()).isEqualTo(NoticeView.Kind.REJECTED));
    }

    @Test
    @DisplayName("읽음 처리는 본인의 알림에만 적용된다")
    void markingReadAffectsOnlyOwnNotices() {
        Member host = member("host");
        Member a = member("a");
        Member b = member("b");
        Party party = party(host, 4);
        participationService.apply(party.getId(), a.getUsername());
        participationService.apply(party.getId(), b.getUsername());
        participationService.approve(party.getId(), entry(party, a).getId(), host.getUsername());
        participationService.approve(party.getId(), entry(party, b).getId(), host.getUsername());

        notificationService.markResultsSeen(a.getUsername());

        assertThat(notificationService.count(a.getUsername())).isZero();
        assertThat(notificationService.count(b.getUsername())).isEqualTo(1);
    }
}
