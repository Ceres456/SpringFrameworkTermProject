package kr.ac.kopo.wodyd.termproject.service;

import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Party;
import kr.ac.kopo.wodyd.termproject.support.ServiceTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("매너 리뷰")
class ReviewServiceTest extends ServiceTestBase {

    /** 방장 + 참가자 2명이 모두 승인된 상태의 파티를 만든다. */
    private Party partyWith(Member host, Member a, Member b) {
        Party party = party(host, 3);
        join(party, host, a);
        join(party, host, b);
        return party;
    }

    @Test
    @DisplayName("파티가 종료되기 전에는 리뷰를 남길 수 없다")
    void cannotReviewBeforeClosed() {
        Member host = member("host");
        Member a = member("a");
        Member b = member("b");
        Party party = partyWith(host, a, b);

        assertThatThrownBy(() -> reviewService.write(party.getId(), a.getUsername(), entry(party, host).getId(), 5, "굿"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("종료된 뒤");
    }

    @Test
    @DisplayName("종료 후 참가자가 다른 참가자에게 리뷰를 남기면 매너 점수가 갱신된다")
    void reviewUpdatesMannerScore() {
        Member host = member("host");
        Member a = member("a");
        Member b = member("b");
        Party party = partyWith(host, a, b);
        partyService.close(party.getId(), host.getUsername());

        reviewService.write(party.getId(), a.getUsername(), entry(party, host).getId(), 5, "좋아요");
        assertThat(host.getMannerScore()).isEqualTo(5.0);
        assertThat(host.getReviewCount()).isEqualTo(1);

        reviewService.write(party.getId(), b.getUsername(), entry(party, host).getId(), 4, null);
        assertThat(host.getMannerScore()).isEqualTo(4.5); // (5 + 4) / 2
        assertThat(host.getReviewCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("평균은 소수 첫째 자리까지 반올림한다")
    void averageIsRoundedToOneDecimal() {
        Member host = member("host");
        Member a = member("a");
        Member b = member("b");
        Party party = party(host, 4);
        Member c = member("c");
        join(party, host, a);
        join(party, host, b);
        join(party, host, c);
        partyService.close(party.getId(), host.getUsername());

        reviewService.write(party.getId(), a.getUsername(), entry(party, host).getId(), 5, null);
        reviewService.write(party.getId(), b.getUsername(), entry(party, host).getId(), 4, null);
        reviewService.write(party.getId(), c.getUsername(), entry(party, host).getId(), 4, null);

        assertThat(host.getMannerScore()).isEqualTo(4.3); // 13 / 3 = 4.33...
    }

    @Test
    @DisplayName("자기 자신은 평가할 수 없다")
    void cannotReviewSelf() {
        Member host = member("host");
        Member a = member("a");
        Member b = member("b");
        Party party = partyWith(host, a, b);
        partyService.close(party.getId(), host.getUsername());

        assertThatThrownBy(() -> reviewService.write(party.getId(), a.getUsername(), entry(party, a).getId(), 5, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("자기 자신");
    }

    @Test
    @DisplayName("파티에 참가하지 않은 사람은 리뷰를 남길 수 없다")
    void outsiderCannotReview() {
        Member host = member("host");
        Member a = member("a");
        Member b = member("b");
        Member outsider = member("outsider");
        Party party = partyWith(host, a, b);
        partyService.close(party.getId(), host.getUsername());

        assertThatThrownBy(() ->
                reviewService.write(party.getId(), outsider.getUsername(), entry(party, host).getId(), 5, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("참가한 사람");
    }

    @Test
    @DisplayName("승인되지 않은(거절·대기) 신청자는 평가 대상이 될 수 없다")
    void cannotReviewNonParticipant() {
        Member host = member("host");
        Member a = member("a");
        Member waiting = member("waiting");
        Party party = party(host, 4);
        join(party, host, a);
        participationService.apply(party.getId(), waiting.getUsername()); // 대기 상태로 둔다
        partyService.close(party.getId(), host.getUsername());

        assertThatThrownBy(() ->
                reviewService.write(party.getId(), a.getUsername(), entry(party, waiting).getId(), 5, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("리뷰 대상");
    }

    @Test
    @DisplayName("같은 파티에서 같은 사람에게는 한 번만 평가할 수 있다")
    void cannotReviewSamePersonTwice() {
        Member host = member("host");
        Member a = member("a");
        Member b = member("b");
        Party party = partyWith(host, a, b);
        partyService.close(party.getId(), host.getUsername());
        reviewService.write(party.getId(), a.getUsername(), entry(party, host).getId(), 5, null);

        assertThatThrownBy(() -> reviewService.write(party.getId(), a.getUsername(), entry(party, host).getId(), 3, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 리뷰");
        assertThat(host.getReviewCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("별점은 1~5점, 한줄평은 200자 이내만 허용한다")
    void validatesRatingAndComment() {
        Member host = member("host");
        Member a = member("a");
        Member b = member("b");
        Party party = partyWith(host, a, b);
        partyService.close(party.getId(), host.getUsername());
        Long hostEntry = entry(party, host).getId();

        assertThatThrownBy(() -> reviewService.write(party.getId(), a.getUsername(), hostEntry, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reviewService.write(party.getId(), a.getUsername(), hostEntry, 6, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reviewService.write(party.getId(), a.getUsername(), hostEntry, 5, "가".repeat(201)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("200자");
        assertThat(host.getReviewCount()).isZero();
    }
}
