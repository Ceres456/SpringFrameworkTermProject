package kr.ac.kopo.wodyd.termproject.dto;

import kr.ac.kopo.wodyd.termproject.domain.PartyStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 마이페이지 '내가 만든 파티' 한 줄 */
@Getter
@Builder
public class MyPartyView {
    private final Long id;
    private final String title;
    private final String gameName;
    private final PartyStatus status;
    private final long currentMembers;
    private final int maxMembers;
    /** 방장이 아직 처리하지 않은 신청 수 */
    private final long pendingCount;
    private final LocalDateTime createdAt;
}
