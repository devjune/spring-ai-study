# spring-ai-study

팀 내 AI 스터디 **학습 자료**. 주제는 **Spring AI 2.0**.
대상은 Java/Spring 백엔드 개발자 (Spring AI를 처음 접하는 사람 포함). 기준 버전 Spring AI 2.0.0 GA (2026-06).

## 목표

Spring AI 2.0을 기준으로 핵심 개념과 사용법을 처음부터 정리한다. 다 읽으면 (1) "Spring AI로 무엇을 만들 수 있나"에 정확히 답하고, (2) 직접 간단한 AI 기능을 붙일 수 있는 것이 목표.

## 문서

| 파일 | 성격 | 내용 |
| --- | --- | --- |
| [spring-ai-study.md](./spring-ai-study.md) | 학습 자료 (본문) | Spring AI 2.0 개념부터 에이전트까지. 시작하기 → 핵심 기능 9개(ChatClient·Structured Output·ChatMemory·Tool Calling·RAG·ToolSearch·MCP·Advisor·관측성) → 직접 만들어 보기. 위키 포팅 대상 |
| [docs/decisions.md](./docs/decisions.md) | 방향·결정 로그 | 문서를 왜 이렇게 구성했나 — 목표·구성·톤·기준버전 등 결정과 근거 |
| [docs/study-log.md](./docs/study-log.md) | 학습 로그 | 주제별 날것의 학습 메모 |

## 관통하는 두 테마

- **이식성(Portability)** — 벤더나 DB를 바꿔도 코드는 유지된다. 2.0에서는 도구·에이전트 상호운용(MCP)으로까지 확장.
- **위임 vs 통제** — LLM에 흐름을 얼마나 맡기고, 어디에 가드레일을 칠 것인가. 에이전트가 커질수록 더 중요해진다.
