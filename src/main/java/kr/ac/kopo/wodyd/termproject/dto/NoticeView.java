package kr.ac.kopo.wodyd.termproject.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 마이페이지 '새 소식' 한 줄 */
@Getter
@Builder
public class NoticeView {

    public enum Kind {
        /** 내 신청이 승인됨 */
        APPROVED,
        /** 내 신청이 거절됨 */
        REJECTED,
        /** 내 파티에 새 신청이 들어와 처리를 기다림 */
        INCOMING
    }

    private final Kind kind;
    private final Long partyId;
    private final String partyTitle;
    /** INCOMING 일 때 신청한 사람의 닉네임, 그 외에는 null */
    private final String applicantNickname;
    private final LocalDateTime occurredAt;
}
