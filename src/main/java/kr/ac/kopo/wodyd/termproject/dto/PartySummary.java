package kr.ac.kopo.wodyd.termproject.dto;

import kr.ac.kopo.wodyd.termproject.domain.PartyStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 파티 목록 한 줄. open-in-view 가 꺼져 있어 뷰에서 지연 로딩을 못 쓰므로 필요한 값만 복사해 둔다. */
@Getter
@Builder
public class PartySummary {
    private final Long id;
    private final String title;
    private final String gameName;
    private final String hostNickname;
    private final int maxMembers;
    private final long currentMembers;
    private final PartyStatus status;
    private final LocalDateTime createdAt;
}
