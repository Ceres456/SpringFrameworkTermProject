package kr.ac.kopo.wodyd.termproject.dto;

import kr.ac.kopo.wodyd.termproject.domain.PartyStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 파티 목록 한 건. open-in-view 가 꺼져 있어 뷰에서 지연 로딩을 못 쓰므로 필요한 값만 복사해 둔다. */
@Getter
@Builder
public class PartySummary {
    private final Long id;
    private final String title;
    private final Long gameId;
    private final String gameName;
    /** 게임 이미지 파일 이름(확장자 제외). 없으면 null */
    private final String gameSlug;
    private final String hostNickname;
    private final double hostMannerScore;
    private final int hostReviewCount;
    private final int maxMembers;
    private final long currentMembers;
    private final PartyStatus status;
    private final LocalDateTime createdAt;

    /** 인원 막대의 채움 비율(0~100) */
    public int getFillPercent() {
        if (maxMembers <= 0) {
            return 0;
        }
        return (int) Math.min(100, currentMembers * 100 / maxMembers);
    }
}
