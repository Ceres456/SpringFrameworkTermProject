package kr.ac.kopo.wodyd.termproject.dto;

import kr.ac.kopo.wodyd.termproject.domain.ApplyStatus;
import kr.ac.kopo.wodyd.termproject.domain.PartyStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 마이페이지 '내 신청 현황' 한 줄 */
@Getter
@Builder
public class ApplicationView {
    private final Long partyId;
    private final String partyTitle;
    private final String gameName;
    private final PartyStatus partyStatus;
    private final ApplyStatus applyStatus;
    private final LocalDateTime appliedAt;
}
