# GP.Hub

같이 게임할 사람을 찾는 **파티 모집 게시판**입니다. 파티 만들기, 참가 신청과 승인, 파티 종료 후 매너 리뷰, 마이페이지, 관리자 페이지가 있습니다.

> GP = Game Player + Game Party, Hub = 그 둘을 모으는 곳

프레임워크 실습 기말과제(텀 프로젝트) 제출물입니다.

## 기술 스택

Java 21 · Spring Boot 4.2 · Spring MVC + Thymeleaf · Spring Data JPA · Spring Security · MySQL 8 · Gradle

## 실행 방법

### 1. 준비

- JDK 21
- MySQL 8 (`localhost:3306` 에서 켜져 있어야 합니다)

`gameparty` 데이터베이스는 없으면 서버가 자동으로 만들고, 테이블 5개와 게임 목록도 자동으로 넣습니다.
SQL 로 구조를 보고 싶다면 [`schema.sql`](schema.sql) 을 참고하세요. (실행하지 않아도 됩니다.)

### 2. MySQL 계정 설정

기본값은 아이디 `root`, 비밀번호 없음입니다. 비밀번호가 있다면 아래 둘 중 하나로 알려 주세요.

**방법 A. 설정 파일** — `local-db.properties.example` 을 복사해 `local-db.properties` 로 바꾸고 값을 채웁니다.

```powershell
Copy-Item local-db.properties.example local-db.properties
```

**방법 B. 환경변수** — `DB_USERNAME`, `DB_PASSWORD`

```powershell
$env:DB_PASSWORD = "MySQL비밀번호"
```

### 3. 서버 켜기

```powershell
.\gradlew.bat bootRun
```

macOS / Linux 는 `./gradlew bootRun` 입니다. 켜지면 브라우저에서 <http://localhost:8080> 으로 접속합니다.

### 4. 관리자 계정

관리자 비밀번호는 소스에 두지 않습니다. `local-db.properties` 에 `app.admin.password` 를 적거나 환경변수 `APP_ADMIN_PASSWORD` 로 지정하면, 서버를 켤 때 관리자 계정(기본 아이디 `admin`)이 만들어집니다. 지정하지 않으면 관리자 계정만 만들어지지 않고 나머지는 정상 동작합니다. 관리자 페이지는 `/admin` 입니다.

### 5. 시연용 더미 데이터 (선택)

회원, 파티, 리뷰가 들어 있는 화면을 보고 싶을 때만 켭니다.

```powershell
.\gradlew.bat bootRun --args='--app.seed-dummy=true'
```

## 테스트

```powershell
.\gradlew.bat test
```

테스트는 MySQL 없이 메모리 DB(H2)로 실행되며, 서비스와 화면 요청 단위 테스트 64개가 있습니다.

## 폴더 구조

```
src/main/java/.../termproject
├─ controller   화면 요청 처리
├─ service      업무 규칙, 트랜잭션
├─ repository   DB 조회 (Spring Data JPA)
├─ domain       엔티티, 상태 값(enum)
├─ dto          화면에 넘기는 데이터, 입력 폼
├─ config       보안 설정, 초기 데이터
src/main/resources
├─ templates    Thymeleaf 화면
└─ static       CSS, JS, 게임 이미지
```
