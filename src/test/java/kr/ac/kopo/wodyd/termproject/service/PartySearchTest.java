package kr.ac.kopo.wodyd.termproject.service;

import kr.ac.kopo.wodyd.termproject.domain.Member;
import kr.ac.kopo.wodyd.termproject.domain.Party;
import kr.ac.kopo.wodyd.termproject.domain.PartyStatus;
import kr.ac.kopo.wodyd.termproject.dto.PartySort;
import kr.ac.kopo.wodyd.termproject.dto.PartySummary;
import kr.ac.kopo.wodyd.termproject.support.ServiceTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("파티 찾기: 모집중만 보기와 정렬")
class PartySearchTest extends ServiceTestBase {

    /**
     * 같은 키워드를 제목에 넣은 파티 5개를 만든다.
     * 남은 자리(정원 - 승인 인원): big=7, mid=4, small=2, full=0(인원마감), closed=5(종료)
     */
    private record Fixture(String keyword, Party big, Party mid, Party small, Party full, Party closed) { }

    private Fixture fixture() {
        String kw = uniq("kw");
        Member host = member("host");
        Party big = party(host, 8, kw + " big");
        Party mid = party(host, 5, kw + " mid");
        Party small = party(host, 3, kw + " small");

        Party full = party(host, 2, kw + " full");
        join(full, host, member("guest"));

        Party closed = party(host, 6, kw + " closed");
        partyService.close(closed.getId(), host.getUsername());
        return new Fixture(kw, big, mid, small, full, closed);
    }

    private List<Long> ids(Fixture f, boolean openOnly, PartySort sort) {
        return partyService.search(null, f.keyword(), openOnly, sort, 0).getContent().stream()
                .map(PartySummary::getId)
                .toList();
    }

    @Test
    @DisplayName("모집중만 보기를 켜면 인원마감·종료 파티가 빠진다")
    void openOnlyHidesFullAndClosed() {
        Fixture f = fixture();

        assertThat(partyService.search(null, f.keyword(), false, PartySort.LATEST, 0).getTotalElements()).isEqualTo(5);

        List<Long> open = ids(f, true, PartySort.LATEST);
        assertThat(open).containsExactlyInAnyOrder(f.big().getId(), f.mid().getId(), f.small().getId());
    }

    @Test
    @DisplayName("자리 많은 순: 남은 자리가 많은 모집중 파티부터, 인원마감·종료는 뒤로")
    void mostSlotsFirst() {
        Fixture f = fixture();

        List<Long> ids = ids(f, false, PartySort.MOST_SLOTS);

        assertThat(ids.subList(0, 3)).containsExactly(f.big().getId(), f.mid().getId(), f.small().getId());
        assertThat(ids.subList(3, 5)).containsExactlyInAnyOrder(f.full().getId(), f.closed().getId());
    }

    @Test
    @DisplayName("마감 임박순: 남은 자리가 적은 모집중 파티부터, 인원마감·종료는 뒤로")
    void endingSoonFirst() {
        Fixture f = fixture();

        List<Long> ids = ids(f, false, PartySort.ENDING_SOON);

        assertThat(ids.subList(0, 3)).containsExactly(f.small().getId(), f.mid().getId(), f.big().getId());
        assertThat(ids.subList(3, 5)).containsExactlyInAnyOrder(f.full().getId(), f.closed().getId());
    }

    @Test
    @DisplayName("모집중만 보기와 정렬을 함께 쓸 수 있다")
    void openOnlyWithSort() {
        Fixture f = fixture();

        assertThat(ids(f, true, PartySort.ENDING_SOON))
                .containsExactly(f.small().getId(), f.mid().getId(), f.big().getId());
        assertThat(ids(f, true, PartySort.MOST_SLOTS))
                .containsExactly(f.big().getId(), f.mid().getId(), f.small().getId());
    }

    @Test
    @DisplayName("승인 인원이 늘면 남은 자리가 줄어 정렬 순서가 바뀐다")
    void orderFollowsApprovedCount() {
        Fixture f = fixture();
        Member host = f.big().getHost();
        // big(정원 8) 에 6명이 승인되면 남은 자리는 1 -> 가장 임박한 파티가 된다
        for (int i = 0; i < 6; i++) {
            join(f.big(), host, member("g"));
        }

        List<Long> ids = ids(f, true, PartySort.ENDING_SOON);

        assertThat(ids.get(0)).isEqualTo(f.big().getId());
    }

    @Test
    @DisplayName("제목 키워드와 게임으로 거를 수 있다")
    void filtersByKeywordAndGame() {
        Fixture f = fixture();

        assertThat(partyService.search(null, f.keyword() + " mid", false, PartySort.LATEST, 0).getContent())
                .extracting(PartySummary::getId).containsExactly(f.mid().getId());
        assertThat(partyService.search(f.small().getGame().getId(), f.keyword(), false, PartySort.LATEST, 0).getContent())
                .extracting(PartySummary::getId).containsExactly(f.small().getId());
    }

    @Test
    @DisplayName("한 페이지에는 12개씩 보여 준다")
    void pageSizeIsTwelve() {
        String kw = uniq("page");
        Member host = member("host");
        for (int i = 0; i < 14; i++) {
            party(host, 4, kw + " " + i);
        }

        var first = partyService.search(null, kw, false, PartySort.LATEST, 0);
        var second = partyService.search(null, kw, false, PartySort.LATEST, 1);

        assertThat(first.getTotalElements()).isEqualTo(14);
        assertThat(first.getContent()).hasSize(12);
        assertThat(second.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("PartySort.fromCode: 모르는 값이나 빈 값은 최신순")
    void sortCodeFallsBackToLatest() {
        assertThat(PartySort.fromCode("slots")).isEqualTo(PartySort.MOST_SLOTS);
        assertThat(PartySort.fromCode("ending")).isEqualTo(PartySort.ENDING_SOON);
        assertThat(PartySort.fromCode(null)).isEqualTo(PartySort.LATEST);
        assertThat(PartySort.fromCode("")).isEqualTo(PartySort.LATEST);
        assertThat(PartySort.fromCode("bogus")).isEqualTo(PartySort.LATEST);
    }

    @Test
    @DisplayName("현재 인원은 승인된 인원만 센다")
    void summaryCountsApprovedOnly() {
        Member host = member("host");
        Party party = party(host, 5, uniq("count"));
        join(party, host, member("a"));
        participationService.apply(party.getId(), member("waiting").getUsername());

        PartySummary summary = partyService.search(null, party.getTitle(), false, PartySort.LATEST, 0)
                .getContent().get(0);

        assertThat(summary.getCurrentMembers()).isEqualTo(2); // 방장 + 승인된 1명 (대기 1명은 제외)
        assertThat(summary.getStatus()).isEqualTo(PartyStatus.RECRUITING);
    }
}
