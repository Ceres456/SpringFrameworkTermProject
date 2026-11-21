package kr.ac.kopo.wodyd.termproject.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 파티 모집글 작성/수정 폼 */
@Getter
@Setter
public class PartyForm {

    @NotNull(message = "게임을 선택하세요.")
    private Long gameId;

    @NotBlank(message = "제목을 입력하세요.")
    @Size(min = 2, max = 100, message = "제목은 2~100자입니다.")
    private String title;

    @Size(max = 2000, message = "내용은 2000자 이내입니다.")
    private String content;

    @NotNull(message = "모집 인원을 입력하세요.")
    @Min(value = 2, message = "모집 인원은 방장 포함 2명 이상입니다.")
    @Max(value = 20, message = "모집 인원은 20명 이하입니다.")
    private Integer maxMembers = 5;

    /** 디스코드/오픈채팅 링크. javascript: 같은 스킴을 막기 위해 http(s) 만 허용 */
    @Size(max = 255, message = "링크는 255자 이내입니다.")
    @Pattern(regexp = "^$|^https?://\\S+$", message = "http:// 또는 https:// 로 시작하는 링크를 입력하세요.")
    private String contactLink;
}
