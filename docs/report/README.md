# GameParty 보고서 자료

보고서에 넣을 자료를 모아 둔 폴더입니다.

| 파일 | 내용 |
| --- | --- |
| `GameParty-report-materials.pdf` | 아래 자료를 한 문서로 묶은 PDF (22쪽) |
| `GameParty-report-draft.pdf` | 보고서 초안 PDF (문서 \"GameParty 텀프로젝트 보고서 (초안)\"을 내보낸 것, 25쪽) |
| `report-materials.html` | 위 PDF의 원본 페이지 (브라우저에서 열어 `Ctrl+P` 로 다시 PDF 로 저장 가능) |
| `diagrams/er-diagram.svg` | ER 다이어그램 (엔티티 5개) |
| `diagrams/system-architecture.svg` | 시스템 구조도 (계층 구조) |
| `diagrams/execution-flow.svg` | 실행 흐름도 (회원가입 ~ 리뷰, 관리자 흐름 포함) |
| `diagrams/status-transitions.svg` | 파티 상태 · 참가 신청 상태 전이도 |
| `screenshots/*.png` | 대표 화면 16장 (1440px 폭) |
| `tools/make_diagrams.py` | 다이어그램(SVG) 생성 스크립트 |
| `tools/capture.ps1` | 스크린샷 자동 촬영 스크립트 (Chrome DevTools 사용) |

SVG 는 벡터라서 확대해도 깨지지 않습니다. 워드/한글에 넣을 때는 브라우저(Chrome 등)에서 SVG 를 열어 캡처하거나 PDF 로 저장해서 쓰면 됩니다.

## 다시 만들기

```powershell
# 다이어그램
python -X utf8 docs\report\tools\make_diagrams.py docs\report\diagrams

# 스크린샷 (서버를 8081 포트로 띄운 상태, 더미 데이터가 들어 있어야 함)
.\docs\report\tools\capture.ps1 -Base http://localhost:8081 -Out docs\report\screenshots -AdminPw '<관리자 비밀번호>'
```
