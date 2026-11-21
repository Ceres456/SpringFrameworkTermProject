package kr.ac.kopo.wodyd.termproject.domain;

/** 파티 상태: 모집중 -> 인원마감 -> 종료(종료된 뒤에만 파티원 리뷰 가능) */
public enum PartyStatus {
    RECRUITING,
    FULL,
    CLOSED
}
