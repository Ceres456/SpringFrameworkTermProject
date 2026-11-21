package kr.ac.kopo.wodyd.termproject.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 파티 상세 화면에 보여줄 참가자/신청자 한 명 */
@Getter
@Builder
public class ParticipantView {
    /** PartyMember 의 id (승인/거절/리뷰 요청에 사용) */
    private final Long id;
    private final String nickname;
    private final double mannerScore;
    private final int reviewCount;
    private final String mainGame;
    private final boolean host;
    private final LocalDateTime appliedAt;

    /** 지금 로그인한 사용자가 이 사람에게 리뷰를 남길 수 있는지 */
    private final boolean reviewable;
    /** 지금 로그인한 사용자가 이미 이 사람에게 리뷰를 남겼는지 */
    private final boolean reviewedByMe;
}
