package kr.ac.kopo.wodyd.termproject.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 파티 참가 신청 상태 */
@Getter
@RequiredArgsConstructor
public enum ApplyStatus {
    PENDING("승인 대기"),
    APPROVED("참가 중"),
    REJECTED("거절됨");

    private final String label;
}
