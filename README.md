# spring-ai-study

팀 내 AI 스터디 발표(약 20분 + 라이브 데모) 자료. 주제는 **Spring AI**.
대상은 시니어 백엔드 개발자. 기준 버전 Spring AI 1.1.x (현행 GA).

## 문서

| 파일 | 성격 | 내용 |
| --- | --- | --- |
| [spring-ai-study.md](./spring-ai-study.md) | 발표 자료 (청중용) | Q&A 6개로 따라가는 Spring AI 핵심. ChatClient / Structured Output / Tool Calling / RAG. 위키 포팅 대상 |
| [docs/decisions.md](./docs/decisions.md) | 방향·결정 로그 | 문서를 왜 이렇게 구성했나 — 톤·형식·범위·기준버전 등 결정과 근거 |
| [docs/study-log.md](./docs/study-log.md) | 학습 로그 | 주제별 날것의 학습 메모. 발표에 안 실은 배경까지 |

## 관통하는 두 테마

- **이식성(Portability)** — 벤더나 DB를 바꿔도 코드는 유지된다.
- **위임 vs 통제** — LLM에 흐름을 얼마나 맡기고, 어디에 가드레일을 칠 것인가.
