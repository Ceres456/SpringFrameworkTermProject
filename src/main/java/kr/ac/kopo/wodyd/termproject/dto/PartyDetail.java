package kr.ac.kopo.wodyd.termproject.dto;

import kr.ac.kopo.wodyd.termproject.domain.ApplyStatus;
import kr.ac.kopo.wodyd.termproject.domain.PartyStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/** 파티 상세 화면용 데이터 */
@Getter
@Builder
public class PartyDetail {
    private final Long id;
    private final String title;
    private final String content;
    private final String gameName;
    private final String hostNickname;
    private final double hostMannerScore;
    private final int hostReviewCount;
    private final int maxMembers;
    private final long currentMembers;
    private final String contactLink;
    private final PartyStatus status;
    private final LocalDateTime createdAt;

    /** 현재 로그인한 사용자가 방장인지 */
    private final boolean mine;
    /** 연락 링크를 볼 수 있는지 (방장 또는 승인된 참가자) */
    private final boolean canSeeContact;

    /** 현재 사용자의 신청 상태 (신청하지 않았으면 null) */
    private final ApplyStatus myStatus;
    private final boolean canApply;
    private final boolean canCancel;

    /** 승인된 참가자 (방장 포함, 누구나 볼 수 있음) */
    private final List<ParticipantView> participants;
    /** 승인 대기 중인 신청자 (방장에게만 채워짐) */
    private final List<ParticipantView> applicants;

    /** 방장이 파티를 종료할 수 있는지 */
    private final boolean canClose;
    /** 종료된 파티에서 오간 리뷰 (종료 전에는 비어 있음) */
    private final List<ReviewView> reviews;
}
