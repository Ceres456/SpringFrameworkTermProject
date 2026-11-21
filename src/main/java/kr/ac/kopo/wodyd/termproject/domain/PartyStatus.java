package kr.ac.kopo.wodyd.termproject.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 파티 상태: 모집중 -> 인원마감 -> 종료(종료된 뒤에만 파티원 리뷰 가능) */
@Getter
@RequiredArgsConstructor
public enum PartyStatus {
    RECRUITING("모집중"),
    FULL("인원마감"),
    CLOSED("종료");

    private final String label;
}
