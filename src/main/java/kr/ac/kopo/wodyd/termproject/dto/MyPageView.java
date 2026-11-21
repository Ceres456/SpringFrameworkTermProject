package kr.ac.kopo.wodyd.termproject.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/** 마이페이지 화면 전체 데이터 */
@Getter
@Builder
public class MyPageView {
    private final String username;
    private final String nickname;
    private final String mainGame;
    private final double mannerScore;
    private final int reviewCount;
    private final LocalDateTime joinedAt;

    private final List<MyPartyView> hostedParties;
    private final List<ApplicationView> applications;
    private final List<ReceivedReviewView> receivedReviews;
}
