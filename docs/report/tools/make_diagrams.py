# -*- coding: utf-8 -*-
"""GameParty 보고서용 다이어그램(SVG) 생성기. 엔티티/서비스 코드에서 확인한 내용을 그대로 그린다."""
import os
import sys
from xml.sax.saxutils import escape

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)

SANS = "'Malgun Gothic','Apple SD Gothic Neo','Noto Sans KR',sans-serif"
MONO = "Consolas,'D2Coding','Courier New',monospace"
INK = "#1f2430"
MUTED = "#667085"
LINE = "#64748b"
BORDER = "#cbd5e1"


def e(s):
    return escape(s)


def tw(s, size):
    """글자 폭 추정: 한글/기호는 1.0em, 영문·숫자 0.6em, 공백 0.32em, 구두점 0.45em"""
    total = 0.0
    for ch in s:
        o = ord(ch)
        if o >= 0x1100:
            total += 1.0
        elif ch == " ":
            total += 0.32
        elif ch.isalnum():
            total += 0.6
        else:
            total += 0.45
    return total * size


def svg_open(w, h, title):
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" width="{w}" height="{h}" '
        f'font-family="{SANS}">\n'
        f'<title>{e(title)}</title>\n'
        f'<rect width="{w}" height="{h}" fill="#ffffff"/>\n'
        '<defs>\n'
        f'  <marker id="arr" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">'
        f'<path d="M0,0 L10,5 L0,10 z" fill="{LINE}"/></marker>\n'
        '  <marker id="arrD" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">'
        '<path d="M0,0 L10,5 L0,10 z" fill="#dc2626"/></marker>\n'
        '</defs>\n'
    )


def text(x, y, s, size=14, weight="normal", fill=INK, anchor="start", family=None, extra=""):
    fam = f' font-family="{family}"' if family else ""
    return (f'<text x="{x}" y="{y}" font-size="{size}" font-weight="{weight}" fill="{fill}" '
            f'text-anchor="{anchor}"{fam} {extra}>{e(s)}</text>\n')


def label(x, y, s, size=12, anchor="middle", fill=INK, pad=4):
    """흰 배경이 깔린 선 위 글자"""
    w = tw(s, size) * 1.04 + pad * 2 + 4
    if anchor == "middle":
        rx = x - w / 2
    elif anchor == "end":
        rx = x - w
    else:
        rx = x
    return (f'<rect x="{rx:.0f}" y="{y - size - 1}" width="{w:.0f}" height="{size + 7}" rx="3" fill="#ffffff" opacity="0.92"/>\n'
            + text(x, y, s, size, "bold", fill, anchor))


def poly(points, color=LINE, width=2, dashed=False, arrow_end=True, arrow_start=False, marker="arr"):
    pts = " ".join(f"{x},{y}" for x, y in points)
    dash = ' stroke-dasharray="7 5"' if dashed else ""
    ms = f' marker-start="url(#{marker})"' if arrow_start else ""
    me = f' marker-end="url(#{marker})"' if arrow_end else ""
    return (f'<polyline points="{pts}" fill="none" stroke="{color}" stroke-width="{width}" '
            f'stroke-linejoin="round"{dash}{ms}{me}/>\n')


def rbox(x, y, w, h, fill="#ffffff", stroke=BORDER, sw=1.5, r=10):
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{fill}" stroke="{stroke}" stroke-width="{sw}"/>\n'


def tag(x, y, s):
    return (f'<rect x="{x - 10}" y="{y - 13}" width="22" height="19" rx="5" fill="#ffffff" stroke="#94a3b8"/>\n'
            + text(x + 1, y + 1, s, 12, "bold", INK, "middle"))


# ---------------------------------------------------------------- ER 다이어그램
def er():
    W, H = 1240, 960
    s = svg_open(W, H, "GameParty ER 다이어그램")
    s += text(40, 36, "GameParty ER 다이어그램", 22, "bold")
    s += text(40, 58, "JPA 엔티티 5개 · MySQL 테이블 기준", 13, "normal", MUTED)
    # 범례
    lx = 760
    for i, (k, c, t) in enumerate([("PK", "#f59e0b", "기본키"), ("FK", "#3b82f6", "외래키"), ("UK", "#10b981", "유니크")]):
        s += (f'<rect x="{lx + i * 150}" y="24" width="30" height="20" rx="5" fill="{c}"/>\n'
              + text(lx + i * 150 + 15, 39, k, 12, "bold", "#ffffff", "middle")
              + text(lx + i * 150 + 38, 39, t, 13, "normal", INK))
    s += '<g transform="translate(0,64)">\n'

    RH, HH = 26, 36

    def anchor_y(t, i):
        return t["y"] + HH + RH * i + RH // 2

    tables = {
        "member": dict(x=40, y=40, w=330, color="#f97316", ko="회원", rows=[
            ("PK", "id", "BIGINT"), ("UK", "username", "VARCHAR(30)"), ("", "password", "VARCHAR(255) · BCrypt"),
            ("UK", "nickname", "VARCHAR(20)"), ("", "main_game", "VARCHAR(50)"), ("", "role", "USER / ADMIN"),
            ("", "manner_score", "DOUBLE"), ("", "review_count", "INT"), ("", "created_at", "DATETIME")]),
        "party": dict(x=470, y=40, w=330, color="#7c3aed", ko="파티", rows=[
            ("PK", "id", "BIGINT"), ("", "title", "VARCHAR(100)"), ("", "content", "TEXT"),
            ("FK", "game_id", "BIGINT → game"), ("FK", "host_id", "BIGINT → member"), ("", "max_members", "INT"),
            ("", "contact_link", "VARCHAR(255)"), ("", "status", "RECRUITING / FULL / CLOSED"),
            ("", "created_at", "DATETIME")]),
        "game": dict(x=880, y=40, w=340, color="#0d9488", ko="게임", rows=[
            ("PK", "id", "BIGINT"), ("UK", "name", "VARCHAR(50)"), ("", "genre", "VARCHAR(30)"),
            ("", "slug", "VARCHAR(50)")]),
        "party_member": dict(x=470, y=470, w=330, color="#2563eb", ko="참가 신청", rows=[
            ("PK", "id", "BIGINT"), ("FK", "party_id", "BIGINT → party"), ("FK", "member_id", "BIGINT → member"),
            ("", "status", "PENDING / APPROVED / REJECTED"), ("", "applied_at", "DATETIME"),
            ("", "decided_at", "DATETIME · 승인/거절 시각"), ("", "result_seen", "BOOLEAN · 결과 확인 여부"),
            ("UK", "(party_id, member_id)", "복합 유니크")]),
        "party_review": dict(x=880, y=470, w=340, color="#db2777", ko="매너 리뷰", rows=[
            ("PK", "id", "BIGINT"), ("FK", "party_id", "BIGINT → party"), ("FK", "reviewer_id", "BIGINT → member"),
            ("FK", "target_id", "BIGINT → member"), ("", "rating", "INT (1~5)"), ("", "comment", "VARCHAR(200)"),
            ("", "created_at", "DATETIME"), ("UK", "(party_id, reviewer_id, target_id)", "복합 유니크")]),
    }
    M, P, G, PM, PR = (tables[k] for k in ("member", "party", "game", "party_member", "party_review"))

    # 관계선 (표보다 먼저 그려 표가 위에 오게 한다)
    rel = ""
    # 1) member 1 : N party (방장)
    ya, yb = anchor_y(M, 0), anchor_y(P, 4)
    rel += poly([(370, ya), (420, ya), (420, yb), (470, yb)], arrow_end=False)
    # 2) game 1 : N party
    ya, yb = anchor_y(G, 0), anchor_y(P, 3)
    rel += poly([(880, ya), (840, ya), (840, yb), (800, yb)], arrow_end=False)
    # 3) party 1 : N party_member
    rel += poly([(635, 40 + HH + RH * 9), (635, 470)], arrow_end=False)
    # 4) member 1 : N party_member (신청자)
    yb = anchor_y(PM, 2)
    rel += poly([(250, 40 + HH + RH * 9), (250, yb), (470, yb)], arrow_end=False)
    # 5) party 1 : N party_review
    ya, yb = anchor_y(P, 7), anchor_y(PR, 1)
    rel += poly([(800, ya), (840, ya), (840, yb), (880, yb)], arrow_end=False)
    # 6) member 1 : N party_review (작성자)
    rel += poly([(180, 40 + HH + RH * 9), (180, 820), (1000, 820), (1000, 470 + HH + RH * 8)], arrow_end=False)
    # 7) member 1 : N party_review (받는 사람)
    rel += poly([(120, 40 + HH + RH * 9), (120, 860), (1100, 860), (1100, 470 + HH + RH * 8)], arrow_end=False)
    s += rel

    # 표
    for name, t in tables.items():
        h = HH + RH * len(t["rows"])
        s += f'<rect x="{t["x"]}" y="{t["y"]}" width="{t["w"]}" height="{h}" rx="8" fill="#ffffff" stroke="#94a3b8" stroke-width="1.6"/>\n'
        s += (f'<path d="M{t["x"]},{t["y"] + HH} L{t["x"]},{t["y"] + 8} Q{t["x"]},{t["y"]} {t["x"] + 8},{t["y"]} '
              f'L{t["x"] + t["w"] - 8},{t["y"]} Q{t["x"] + t["w"]},{t["y"]} {t["x"] + t["w"]},{t["y"] + 8} '
              f'L{t["x"] + t["w"]},{t["y"] + HH} Z" fill="{t["color"]}"/>\n')
        s += text(t["x"] + 14, t["y"] + 24, name, 16, "bold", "#ffffff", family=MONO)
        s += text(t["x"] + t["w"] - 14, t["y"] + 24, t["ko"], 13, "bold", "#ffffff", "end")
        for i, (key, col, typ) in enumerate(t["rows"]):
            ry = t["y"] + HH + RH * i
            if i % 2 == 1:
                s += f'<rect x="{t["x"] + 1}" y="{ry}" width="{t["w"] - 2}" height="{RH}" fill="#f8fafc"/>\n'
            if key:
                c = {"PK": "#f59e0b", "FK": "#3b82f6", "UK": "#10b981"}[key]
                s += (f'<rect x="{t["x"] + 10}" y="{ry + 4}" width="28" height="18" rx="4" fill="{c}"/>\n'
                      + text(t["x"] + 24, ry + 17, key, 11, "bold", "#ffffff", "middle"))
            small = col.startswith("(")
            s += text(t["x"] + 48, ry + 18, col, 11 if small else 13, "bold" if key == "PK" else "normal", INK, family=MONO)
            s += text(t["x"] + t["w"] - 12, ry + 18, typ, 11, "normal", MUTED, "end")

    # 카디널리티 / 관계 이름
    s += tag(382, 83, "1") + tag(452, 187, "N") + label(420, 140, "방장", 12)
    s += tag(868, 83, "1") + tag(814, 161, "N") + label(840, 125, "게임", 12)
    s += tag(648, 330, "1") + tag(648, 462, "N") + label(635, 400, "참가 신청", 12)
    s += tag(262, 330, "1") + tag(452, 565, "N") + label(250, 450, "신청자", 12)
    s += tag(812, 265, "1") + tag(868, 539, "N") + label(840, 410, "파티 리뷰", 12)
    s += tag(194, 330, "1") + tag(1013, 735, "N") + label(590, 814, "리뷰 작성자 (reviewer)", 12)
    s += tag(134, 330, "1") + tag(1113, 735, "N") + label(590, 854, "리뷰 받는 사람 (target)", 12)

    # 참고
    s += '<g transform="translate(470,730)">\n'
    s += rbox(0, 0, 330, 56, "#f8fafc", BORDER, 1.2, 8)
    s += text(12, 22, "party_member : member 와 party 의 N:M 을 푼 연결 테이블", 11, "normal", MUTED)
    s += text(12, 42, "방장도 '참가 중'으로 함께 등록 (리뷰 대상에 포함, 알림 없음)", 11, "normal", MUTED)
    s += '</g>\n</g>\n</svg>\n'
    return s


# ---------------------------------------------------------------- 시스템 구조도
def chip(x, y, s, color, fg="#ffffff", size=13):
    w = max(70, int(tw(s, size) * 1.06 + 28))
    return (f'<rect x="{x}" y="{y}" width="{w}" height="28" rx="14" fill="{color}"/>\n'
            + text(x + w / 2, y + 19, s, size, "bold", fg, "middle")), w


def layer(y, h, color, name, sub, chips, x=40, w=860):
    out = rbox(x, y, w, h, "#ffffff", color, 2, 12)
    out += (f'<rect x="{x}" y="{y}" width="150" height="{h}" rx="12" fill="{color}"/>\n'
            f'<rect x="{x + 100}" y="{y}" width="50" height="{h}" fill="{color}"/>\n')
    out += text(x + 75, y + h / 2 - 2, name, 16, "bold", "#ffffff", "middle")
    out += text(x + 75, y + h / 2 + 18, sub, 11, "normal", "#ffffff", "middle")
    cx, cy = x + 168, y + 14
    for c in chips:
        # 줄바꿈
        approx = max(70, int(tw(c, 13) * 1.06 + 28))
        if cx + approx > x + w - 14:
            cx, cy = x + 168, cy + 38
        svg, cw = chip(cx, cy, c, color)
        out += svg
        cx += cw + 10
    return out


def arch():
    W, H = 1240, 860
    s = svg_open(W, H, "GameParty 시스템 구조도")
    s += text(40, 36, "GameParty 시스템 구조도", 22, "bold")
    s += text(40, 58, "계층형 구조 (Spring MVC + Thymeleaf · Spring Data JPA · MySQL)", 13, "normal", MUTED)

    L = [
        (80, 84, "#475569", "사용자 브라우저", "HTML · CSS · JS",
         ["메인", "로그인 · 회원가입", "파티 찾기 · 상세 · 만들기", "마이페이지 · 새 소식 · 정보 수정 · 탈퇴", "관리자 페이지", "확인창 · 게임 선택 · 화이트 모드"]),
        (200, 84, "#b45309", "보안 계층", "Spring Security",
         ["폼 로그인 (세션)", "BCrypt 비밀번호", "CSRF 토큰", "ROLE_USER / ROLE_ADMIN", "/admin/** = ADMIN만"]),
        (320, 112, "#7c3aed", "Controller", "Spring MVC",
         ["HomeController", "MemberController", "PartyController", "MyPageController", "AdminController", "NotificationAdvice"]),
        (468, 126, "#2563eb", "Service", "비즈니스 로직 · 트랜잭션",
         ["MemberService", "PartyService", "ParticipationService", "ReviewService", "MyPageService", "AdminService",
          "NotificationService", "WithdrawalService", "MemberEditValidator"]),
        (630, 112, "#0d9488", "Repository · Entity", "Spring Data JPA · Hibernate",
         ["MemberRepository", "GameRepository", "PartyRepository", "PartyMemberRepository", "PartyReviewRepository",
          "Entity 5개 + enum 3개"]),
        (780, 60, "#be123c", "Database", "MySQL 8",
         ["gameparty 스키마 : member · game · party · party_member · party_review"]),
    ]
    for (y, h, c, n, sub, chips) in L:
        s += layer(y, h, c, n, sub, chips)

    # 계층 사이 화살표 (양방향)
    arrows = [(164, 200, "HTTP 요청 · 응답 (폼 제출 · 페이지)"),
              (284, 320, "인증 · 권한 확인 후 요청 전달"),
              (432, 468, "모델(DTO)을 담아 Thymeleaf 화면으로 응답 ↔ 서비스 호출"),
              (594, 630, "엔티티 저장 · 조회 (JPQL · 파생 쿼리)"),
              (742, 780, "SQL (Hibernate · JDBC)")]
    for (y1, y2, t) in arrows:
        s += poly([(470, y1 + 2), (470, y2 - 2)], arrow_start=True)
        s += text(486, (y1 + y2) / 2 + 4, t, 12, "normal", MUTED)

    # 오른쪽 상자들
    def side(y, h, title, lines, color):
        o = rbox(930, y, 270, h, "#f8fafc", color, 1.8, 10)
        o += f'<rect x="930" y="{y}" width="270" height="30" rx="10" fill="{color}"/>\n<rect x="930" y="{y + 18}" width="270" height="12" fill="{color}"/>\n'
        o += text(945, y + 21, title, 14, "bold", "#ffffff")
        for i, ln in enumerate(lines):
            o += text(945, y + 54 + i * 21, ln, 12, "normal", INK)
        return o

    s += side(80, 214, "기술 스택", [
        "• Java 21 · Gradle", "• Spring Boot 4.2", "• Spring MVC + Thymeleaf", "• Spring Data JPA (Hibernate)",
        "• Spring Security (BCrypt · CSRF)", "• MySQL 8", "• Lombok", "• 순수 CSS · 바닐라 JS (프레임워크 없음)"], "#475569")
    s += side(314, 150, "서버 시작 시 실행", [
        "• GameDataInitializer : 게임 39종 등록", "• AdminAccountInitializer : 관리자 계정",
        "• DummyDataInitializer : 시연용 더미", "   (--app.seed-dummy=true 일 때만)"], "#b45309")
    s += side(484, 356, "설계 포인트", [
        "• open-in-view=false → 화면에는 DTO만",
        "• EntityGraph/fetch join 으로 N+1 방지",
        "• 정원 동시 변경은 비관적 락으로 보호",
        "• 승인 인원에 따라 모집중↔인원마감 전환",
        "• 종료된 파티에서만 참가자끼리 1회 리뷰",
        "• 리뷰·회원·파티 삭제 시 점수 재계산",
        "• 알림은 신청 내역에서 계산 (테이블 없음)",
        "• 공용 검증기로 수정 규칙 통일",
        "• 규칙은 서버에서 검증",
        "• 관리자 비밀번호는 Git 제외 파일에 보관"], "#0d9488")
    s += '</svg>\n'
    return s


# ---------------------------------------------------------------- 실행 흐름도 (스윔레인)
def flow():
    W, H = 1120, 1090
    s = svg_open(W, H, "GameParty 실행 흐름도")
    s += text(40, 34, "GameParty 실행 흐름도", 22, "bold")
    lanes = [(20, "회원 · 참가자", "#2563eb", "#eff6ff"), (380, "방장", "#7c3aed", "#f5f3ff"),
             (740, "시스템 · 관리자", "#0d9488", "#f0fdfa")]
    for (lx, name, c, bg) in lanes:
        s += f'<rect x="{lx}" y="56" width="360" height="1010" fill="{bg}" stroke="{BORDER}"/>\n'
        s += f'<rect x="{lx}" y="56" width="360" height="40" fill="{c}"/>\n'
        s += text(lx + 180, 82, name, 16, "bold", "#ffffff", "middle")

    BW, BH = 280, 82
    cx = {"A": 200, "B": 560, "C": 920}
    col = {"A": "#2563eb", "B": "#7c3aed", "C": "#0d9488"}
    steps = [  # (key, lane, y, no, title, sub1, sub2)
        (1, "A", 120, "1", "회원가입", "/signup · 아이디 · 비밀번호 · 닉네임", "주 게임 선택(드래그/검색) · BCrypt 저장"),
        (2, "A", 225, "2", "로그인", "/login · Spring Security 세션", "관리자는 ADMIN 권한으로 로그인"),
        (3, "B", 330, "3", "파티 만들기", "/parties/new · 게임 · 제목 · 인원", "방장은 '참가 중'으로 자동 등록"),
        (4, "A", 435, "4", "파티 찾기", "/parties · 게임 · 제목 · 모집중만 보기", "정렬(최신/자리/마감 임박) · 12개씩 페이지"),
        (5, "A", 540, "5", "참가 신청", "모집중 파티에만 · 상태 '승인 대기'", "방장에게 '새 소식' 알림 · 중복 신청 불가"),
        (6, "B", 645, "6", "신청 승인 / 거절", "신청자에게 '새 소식'(승인/거절) 알림", "정원이 차면 자동 마감 · 비관적 락 보호"),
        (7, "B", 750, "7", "파티 종료", "수정 · 삭제 · 신청 처리 잠금", "참가자끼리 리뷰 작성 가능해짐"),
        (8, "A", 855, "8", "매너 리뷰 작성", "별점 1~5 + 한줄평 · 본인 제외", "같은 파티 · 같은 사람에게 1회"),
        (9, "C", 960, "9", "매너 점수 갱신", "받은 리뷰 평균(소수 1자리)", "목록 · 상세 · 마이페이지에 표시"),
    ]
    admin = [  # (y, title, sub1, sub2)
        (225, "관리자 로그인", "/admin · ADMIN 권한만 접근", "일반 회원은 403 으로 차단"),
        (330, "관리자 페이지", "대시보드 통계 · 회원/파티/리뷰 관리", "회원 정보 조회·수정 · 권한 부여"),
        (435, "삭제 · 강제 종료", "회원 · 파티 · 리뷰 삭제, 파티 강제 종료", "딸린 참가 내역과 리뷰까지 함께 정리"),
    ]

    pos = {}
    for (k, lane, y, no, title, a, b) in steps:
        x = cx[lane] - BW // 2
        pos[k] = (lane, y)
        s += rbox(x, y, BW, BH, "#ffffff", col[lane], 2, 12)
        s += f'<circle cx="{x + 24}" cy="{y + 26}" r="14" fill="{col[lane]}"/>\n'
        s += text(x + 24, y + 31, no, 14, "bold", "#ffffff", "middle")
        s += text(x + 48, y + 31, title, 16, "bold")
        s += text(x + 14, y + 56, a, 12, "normal", INK)
        s += text(x + 14, y + 74, b, 11.5, "normal", MUTED)

    for i, (y, title, a, b) in enumerate(admin):
        x = cx["C"] - BW // 2
        s += rbox(x, y, BW, BH, "#ffffff", "#0d9488", 2, 12)
        s += f'<rect x="{x + 10}" y="{y + 12}" width="34" height="20" rx="5" fill="#0d9488"/>\n'
        s += text(x + 27, y + 27, "관리", 11, "bold", "#ffffff", "middle")
        s += text(x + 54, y + 31, title, 16, "bold")
        s += text(x + 14, y + 56, a, 12, "normal", INK)
        s += text(x + 14, y + 74, b, 11.5, "normal", MUTED)

    # 화살표: 단계 i -> i+1
    for i in range(1, 9):
        (l1, y1), (l2, y2) = pos[i], pos[i + 1]
        x1, x2 = cx[l1], cx[l2]
        ya = y1 + BH
        if x1 == x2:
            s += poly([(x1, ya), (x2, y2)])
        else:
            mid = ya + (y2 - ya) / 2
            s += poly([(x1, ya), (x1, mid), (x2, mid), (x2, y2)])
    # 관리자 흐름
    s += poly([(cx["C"], 225 + BH), (cx["C"], 330)])
    s += poly([(cx["C"], 330 + BH), (cx["C"], 435)])
    # 관리자 삭제 -> 점수 재계산 (점선)
    s += poly([(cx["C"] + 100, 435 + BH), (cx["C"] + 100, 960)], color="#dc2626", dashed=True, marker="arrD")
    s += label(cx["C"] + 100, 700, "삭제·탈퇴 시 점수 재계산", 12, "middle", "#dc2626")
    # 로그인 -> 관리자 로그인 (참고선)
    s += poly([(340, 266), (780, 266)], color="#94a3b8", dashed=True)
    s += label(560, 258, "관리자 계정으로 로그인", 11, "middle", MUTED)
    s += '</svg>\n'
    return s


# ---------------------------------------------------------------- 상태 전이도
def state_box(x, y, w, h, title, sub, color, final=False):
    o = rbox(x, y, w, h, "#ffffff", color, 2.2, 14)
    if final:
        o += rbox(x + 5, y + 5, w - 10, h - 10, "none", color, 1.2, 10)
    o += text(x + w / 2, y + h / 2 - 2, title, 16, "bold", color, "middle")
    o += text(x + w / 2, y + h / 2 + 17, sub, 11, "normal", MUTED, "middle", MONO)
    return o


def status():
    W, H = 1240, 560
    s = svg_open(W, H, "GameParty 상태 전이도")
    s += text(40, 36, "GameParty 상태 전이도", 22, "bold")

    # ---- 파티 상태
    s += rbox(30, 60, 590, 470, "#faf5ff", "#c4b5fd", 1.5, 14)
    s += text(50, 90, "파티 상태 (PartyStatus)", 17, "bold", "#6d28d9")
    s += '<circle cx="70" cy="200" r="9" fill="#475569"/>\n'
    s += poly([(80, 200), (140, 200)])
    s += text(70, 232, "파티 만들기", 11.5, "normal", MUTED, "middle")
    s += state_box(140, 168, 170, 64, "모집중", "RECRUITING", "#16a34a")
    s += state_box(420, 168, 170, 64, "인원마감", "FULL", "#d97706")
    s += state_box(280, 380, 170, 64, "종료", "CLOSED", "#475569", final=True)
    s += poly([(310, 186), (420, 186)])
    s += label(365, 176, "승인 인원 = 정원", 11.5)
    s += poly([(420, 216), (310, 216)])
    s += label(365, 246, "참가 취소 · 정원 늘림", 11.5)
    s += poly([(225, 232), (315, 380)])
    s += label(258, 312, "방장 종료 · 강제 종료", 11.5, "end")
    s += poly([(505, 232), (415, 380)])
    s += label(466, 312, "방장 종료 · 강제 종료", 11.5, "start")
    s += text(365, 478, "종료 후: 참가자끼리 매너 리뷰 작성 가능 (같은 사람에게 1회)", 12, "bold", INK, "middle")
    s += text(365, 498, "종료된 파티는 수정 · 삭제 · 신청 · 신청 처리가 모두 막힘", 12, "normal", MUTED, "middle")

    # ---- 참가 신청 상태
    s += rbox(650, 60, 560, 470, "#eff6ff", "#93c5fd", 1.5, 14)
    s += text(670, 90, "참가 신청 상태 (ApplyStatus)", 17, "bold", "#1d4ed8")
    s += '<circle cx="690" cy="200" r="9" fill="#475569"/>\n'
    s += poly([(700, 200), (750, 200)])
    s += text(690, 232, "참가 신청", 11.5, "normal", MUTED, "middle")
    s += state_box(750, 168, 160, 64, "승인 대기", "PENDING", "#d97706")
    s += state_box(1010, 168, 160, 64, "참가 중", "APPROVED", "#16a34a")
    s += state_box(750, 340, 160, 64, "거절됨", "REJECTED", "#dc2626")
    s += poly([(910, 200), (1010, 200)])
    s += label(960, 190, "방장 승인", 11.5)
    s += poly([(830, 232), (830, 340)])
    s += label(838, 292, "방장 거절", 11.5, "start")
    # 취소 (끝 상태)
    s += '<circle cx="1090" cy="372" r="13" fill="none" stroke="#475569" stroke-width="2"/>\n<circle cx="1090" cy="372" r="7" fill="#475569"/>\n'
    s += poly([(1090, 232), (1090, 358)])
    s += label(1100, 300, "본인 취소 (종료 전)", 11.5, "start")
    s += text(1090, 408, "신청 내역 삭제", 11.5, "normal", MUTED, "middle")
    s += text(670, 468, "• 방장은 파티를 만들 때 자동으로 '참가 중'으로 등록됩니다.", 12, "normal", INK)
    s += text(670, 489, "• 승인 대기 상태에서도 본인이 취소할 수 있습니다.", 12, "normal", INK)
    s += text(670, 510, "• 승인은 정원에 여유가 있을 때만 가능합니다 (동시 처리는 비관적 락).", 12, "normal", INK)
    s += '</svg>\n'
    return s


for name, fn in (("er-diagram", er), ("system-architecture", arch), ("execution-flow", flow), ("status-transitions", status)):
    path = os.path.join(OUT, name + ".svg")
    with open(path, "w", encoding="utf-8") as f:
        f.write(fn())
    print("wrote", path, os.path.getsize(path), "bytes")
