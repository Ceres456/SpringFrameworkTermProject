-- GP.Hub 데이터베이스 스키마 (MySQL 8)
--
-- 참고: 이 프로젝트는 서버를 켤 때 Hibernate 가 엔티티를 보고 테이블을 자동으로 만든다
--       (spring.jpa.hibernate.ddl-auto=update). 그래서 이 파일을 따로 실행하지 않아도 된다.
--       DB 구조를 SQL 로 확인하거나, 직접 만들어 보고 싶을 때 쓰는 파일이다.
--       엔티티와 같은 구조이므로 먼저 실행해 두어도 서버가 그대로 동작한다.

CREATE DATABASE IF NOT EXISTS gameparty
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE gameparty;

-- 회원
CREATE TABLE IF NOT EXISTS member (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    username     VARCHAR(30)  NOT NULL,                -- 로그인 아이디
    password     VARCHAR(255) NOT NULL,                -- BCrypt 로 암호화한 비밀번호
    nickname     VARCHAR(20)  NOT NULL,                -- 화면에 보이는 이름
    main_game    VARCHAR(50)  NULL,                    -- 주로 하는 게임 (선택)
    role         VARCHAR(10)  NOT NULL,                -- USER / ADMIN
    manner_score DOUBLE       NOT NULL,                -- 받은 리뷰의 평균 매너 점수 (0.0 ~ 5.0)
    review_count INT          NOT NULL,                -- 받은 리뷰 수
    created_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_username (username),
    UNIQUE KEY uk_member_nickname (nickname)
) ENGINE = InnoDB;

-- 게임
CREATE TABLE IF NOT EXISTS game (
    id    BIGINT      NOT NULL AUTO_INCREMENT,
    name  VARCHAR(50) NOT NULL,                        -- 게임 이름
    genre VARCHAR(30) NULL,                            -- 장르
    slug  VARCHAR(50) NULL,                            -- 이미지 파일 이름 (확장자 제외)
    PRIMARY KEY (id),
    UNIQUE KEY uk_game_name (name)
) ENGINE = InnoDB;

-- 파티
CREATE TABLE IF NOT EXISTS party (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    title        VARCHAR(100) NOT NULL,
    content      TEXT         NULL,
    game_id      BIGINT       NOT NULL,                -- 하는 게임
    host_id      BIGINT       NOT NULL,                -- 방장
    max_members  INT          NOT NULL,                -- 방장을 포함한 최대 인원
    contact_link VARCHAR(255) NULL,                    -- 디스코드 / 오픈채팅 링크
    status       VARCHAR(15)  NOT NULL,                -- RECRUITING / FULL / CLOSED
    created_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_party_game FOREIGN KEY (game_id) REFERENCES game (id),
    CONSTRAINT fk_party_host FOREIGN KEY (host_id) REFERENCES member (id)
) ENGINE = InnoDB;

-- 참가 신청 (회원과 파티의 N:M 관계를 풀어낸 테이블)
CREATE TABLE IF NOT EXISTS party_member (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    party_id    BIGINT      NOT NULL,
    member_id   BIGINT      NOT NULL,
    status      VARCHAR(10) NOT NULL,                  -- PENDING / APPROVED / REJECTED
    applied_at  DATETIME(6) NOT NULL,                  -- 신청 시각
    decided_at  DATETIME(6) NULL,                      -- 방장이 승인/거절한 시각
    result_seen BIT(1)      NULL,                      -- 신청자가 결과를 확인했는지 (false 면 '새 소식')
    PRIMARY KEY (id),
    UNIQUE KEY uk_party_member (party_id, member_id),  -- 같은 파티에 중복 신청 불가
    CONSTRAINT fk_party_member_party  FOREIGN KEY (party_id)  REFERENCES party (id),
    CONSTRAINT fk_party_member_member FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE = InnoDB;

-- 매너 리뷰
CREATE TABLE IF NOT EXISTS party_review (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    party_id    BIGINT       NOT NULL,
    reviewer_id BIGINT       NOT NULL,                 -- 평가하는 사람
    target_id   BIGINT       NOT NULL,                 -- 평가받는 사람
    rating      INT          NOT NULL,                 -- 매너 점수 1~5 (범위는 서비스에서 검사)
    comment     VARCHAR(200) NULL,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_party_review (party_id, reviewer_id, target_id),  -- 같은 파티, 같은 사람에게 1회
    CONSTRAINT fk_party_review_party    FOREIGN KEY (party_id)    REFERENCES party (id),
    CONSTRAINT fk_party_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES member (id),
    CONSTRAINT fk_party_review_target   FOREIGN KEY (target_id)   REFERENCES member (id)
) ENGINE = InnoDB;
