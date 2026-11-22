package kr.ac.kopo.wodyd.termproject.dto;

import lombok.Getter;

/** 파티 찾기 정렬 기준 */
@Getter
public enum PartySort {

    LATEST("latest", "최신순"),
    MOST_SLOTS("slots", "자리 많은 순"),
    ENDING_SOON("ending", "마감 임박순");

    private final String code;
    private final String label;

    PartySort(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /** 주소의 sort 값을 해석한다. 비었거나 모르는 값이면 최신순. */
    public static PartySort fromCode(String code) {
        for (PartySort sort : values()) {
            if (sort.code.equals(code)) {
                return sort;
            }
        }
        return LATEST;
    }
}
