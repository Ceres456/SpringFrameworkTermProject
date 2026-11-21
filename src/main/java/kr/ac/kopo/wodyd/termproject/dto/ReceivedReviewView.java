package kr.ac.kopo.wodyd.termproject.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 마이페이지 '받은 리뷰' 한 줄 */
@Getter
@Builder
public class ReceivedReviewView {
    private final Long partyId;
    private final String partyTitle;
    private final String reviewerNickname;
    private final int rating;
    private final String comment;
    private final LocalDateTime createdAt;
}
