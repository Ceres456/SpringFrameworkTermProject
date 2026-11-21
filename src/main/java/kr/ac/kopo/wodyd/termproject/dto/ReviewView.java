package kr.ac.kopo.wodyd.termproject.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 파티 상세 화면에 보여줄 리뷰 한 건 */
@Getter
@Builder
public class ReviewView {
    private final String reviewerNickname;
    private final String targetNickname;
    private final int rating;
    private final String comment;
    private final LocalDateTime createdAt;
}
