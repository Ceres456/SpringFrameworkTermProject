package kr.ac.kopo.wodyd.termproject.dto;

import lombok.Builder;
import lombok.Getter;

/** 관리자 대시보드에 보여줄 전체 통계 */
@Getter
@Builder
public class AdminStats {
    private final long memberCount;
    private final long adminCount;
    private final long gameCount;
    private final long partyCount;
    private final long recruitingCount;
    private final long fullCount;
    private final long closedCount;
    private final long reviewCount;
}
