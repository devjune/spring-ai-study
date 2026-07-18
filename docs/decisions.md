# 스터디 문서 방향 & 결정 로그

`spring-ai-study.md`(청중용 발표 자료)를 만들며 내린 결정과 그 근거를 남긴다.
발표 문서 본문에는 들어가지 않는 "왜 이렇게 썼나"의 기록.

---

## 목표

- 팀 내 AI 스터디 발표. 약 20분 + 라이브 데모.
- 대상: 시니어 백엔드 개발자 (전원).
- 주제: Spring AI.
- 최종 산출물은 위키로 포팅.

## 결정 사항

| 항목 | 결정 | 근거 |
| --- | --- | --- |
| 구성 | Q&A(질문 → 답변) 형식 | 시니어 청중은 "왜"에 반응한다. 질문을 따라가며 핵심을 짚는 구조가 설득력이 높다. |
| 범위 | 핵심 4개 주제만 (ChatClient / Structured Output / Tool Calling / RAG) | 기능을 늘리기보다 관통하는 이야기를 깔끔하게. 부가 기능 나열 배제. |
| 사실성 | 팩트 기반만. 추측/추론 배제 | 모든 기술 서술을 공식 Spring AI 문서·블로그와 대조해 검증. 해석적 프레이밍(LangChain 비유, IoC, 위임 vs 통제)은 "발표용 관점"으로만 사용하고 사실과 구분. |
| 톤 | 평서문 "~한다 / ~할 수 있다". 존댓말 X, 친구톤 X | 청중이 전원 시니어라 친구톤은 부적절. 존대는 과함. 간결한 평서문이 정보 밀도가 높다. |
| 형식 | 마크다운 + mermaid 다이어그램 | 위키 포팅을 고려. 표준 마크다운과 mermaid만 사용(외부 이미지 X). |
| 데모 | 인라인 코드 최소화 + "📺 라이브 데모" 마커 | 코드를 다 싣기보다 실제 동작은 라이브로. 데모는 Claude Code로 진행 예정. |
| 기준 버전 | Spring AI 1.1.x (현행 GA, 최신 패치 1.1.8 / 2026-06) | "2025-11 GA 표기는 늦다"는 판단. 현행 패치 기준으로 통일. |
| 부록 | 버전 히스토리 부록은 최종적으로 제거 | 도약 서사를 표/타임라인으로 여러 번 시도했으나 본문 핵심 가독성을 해쳐 삭제. 본문에 집중. |

## 관통하는 두 테마

발표 전체를 꿰는 축. 개별 기능 설명이 여기로 수렴하도록 배치.

- **이식성(Portability)** — 벤더나 DB를 바꿔도 코드는 유지된다. `JdbcTemplate`이 DB 벤더를 추상화하던 발상.
- **위임 vs 통제** — LLM에 흐름을 얼마나 맡기고, 어디에 가드레일을 칠 것인가.

## 검증된 사실 (공식 출처 대조 완료)

- Spring AI 1.0 GA: 2025-05-20 / 1.1 GA: 2025-11-12 / 최신 패치 1.1.8·1.0.9: 2026-06-12 / 2.0.0-M7 마일스톤: 2026-05-23 (GA 아님).
- Anthropic 스타터: `spring-ai-starter-model-anthropic`, 기본 모델 `claude-sonnet-4-5`.
- `StructuredOutputValidationAdvisor`, `useProviderStructuredOutput()`/`validateSchema()`, `@Tool`/`.tools()`/`ToolCallingAdvisor`, `QuestionAnswerAdvisor`, `VectorStore` 구현체(pgvector·Redis·Chroma 등), Micrometer 연동 — 모두 공식 문서 확인.

## 참고

- Spring AI Reference: https://docs.spring.io/spring-ai/reference/
- 1.0 GA 블로그: https://spring.io/blog/2025/05/20/spring-ai-1-0-GA-released/
- 1.1 GA 블로그: https://spring.io/blog/2025/11/12/spring-ai-1-1-GA-released/
