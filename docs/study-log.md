# Spring AI 학습 로그

발표 문서보다 날것의 학습 메모. 주제별 핵심을 정리한다.
기준 버전: Spring AI 1.1.x (현행 GA, 최신 패치 1.1.8 / 2026-06).

---

## 0. 한 줄 정의

Spring AI = **Java/Spring 진영의 LangChain**. Spring 생태계 안에서 Spring답게 AI를 붙이는 표준 추상화. 핵심 철학은 **이식성 + 추상화** — OpenAI/Anthropic/로컬 Ollama를 코드 변경 없이 의존성·설정만으로 교체.

## 1. ChatClient — 모든 것의 중심

- fluent 체인: `prompt().user(message).call().content()`.
- `.call()` = 동기, `.stream()` = SSE 스트리밍(타이핑 효과).
- `.content()` = 응답 텍스트, `.entity(Class)` = 타입 객체.
- 셋업: 의존성 1개 + 설정 2줄 → `ChatClient.Builder`가 자동 빈 등록. `WebClient.Builder` 주입 패턴과 동일.
- 시스템 프롬프트(페르소나): 빌더 `defaultSystem()`에 기본값, 호출 시점 `.system()`으로 오버라이드. `WebClient`의 `defaultHeader()` 계층 구조와 같음.

```groovy
implementation("org.springframework.ai:spring-ai-starter-model-anthropic")
```
```properties
spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY}
spring.ai.anthropic.chat.options.model=claude-sonnet-4-5
```

## 2. Structured Output — 응답을 타입 객체로

- `.content()` 대신 `.entity(Movie::class.java)` 한 줄. 제네릭은 `entity(ParameterizedTypeReference<T>)`.
- 내부 3단계: data class → **스키마 자동 생성 후 프롬프트에 주입** → LLM이 JSON 응답 → `BeanOutputConverter`가 타입 객체로 역직렬화.
- 의미: **LLM을 타입 안전한 함수처럼 사용**. 비정형 입력 → 정형 출력. DTO 변환 레이어가 LLM으로 바뀐 셈.
- 스키마 깨질 때 방어선:
  1. 최신 프론티어 모델은 스키마를 거의 지킨다.
  2. 강제 JSON / 프로바이더 네이티브 강제 출력(`useProviderStructuredOutput()` + `validateSchema()`). 단, `validateSchema` 활성 시 스트리밍 미지원.
  3. 재시도: `StructuredOutputValidationAdvisor`(파싱 실패 시 재요청, `maxRepeatAttempts`). **`.entity()` 자체가 재시도하는 것은 아님** — 별도 Advisor.
  4. 비즈니스 검증(값 범위 등)은 여전히 우리 책임.
- 핵심 태도: **LLM 출력을 신뢰하지 말고 외부 입력처럼 다뤄라.**

## 3. Tool Calling — LLM이 내 함수를 호출

- `@Tool` / `@ToolParam` 어노테이션 + `.tools()`. 함수 스키마 생성·호출 라우팅·결과 재주입 루프를 `ToolCallingAdvisor`가 자동화.
- `@Tool(description=...)`이 "언제 이 도구를 쓸지" 알려주는 힌트 → 가장 중요.
- 철학: **제어의 역전(IoC)**.
  - 기존 백엔드 = 명령형. 개발자가 모든 분기를 미리 짬.
  - LLM + Tools = 위임. "도구를 줄 테니 알아서 목표 달성하라." → LLM이 **오케스트레이터**. 이것이 **에이전트의 본질**.
- 긴장: "확률적 LLM에 흐름 제어를 넘겨도 되나?" → 답: **키를 다 주지 말고 가드레일 친 도구만 쥐여줘라.** 위험 작업은 도구화하지 않거나 사람 승인(human-in-the-loop)을 끼움.

## 4. RAG — 회사 내부 문서에 근거해 답하기

- 문제: LLM은 내부 문서를 모름 → 할루시네이션.
- 아이디어: 답하기 전에 **관련 문서를 먼저 검색해 프롬프트에 주입**. Retrieve → Augment → Generate.
- 핵심: **모델 재학습 없이 지식만 실시간 주입.** 파인튜닝보다 싸고 빠름, 문서 바뀌면 DB만 갱신.
- 백엔드 포인트:
  - **VectorStore 추상화** — pgvector/Redis/Chroma 등 무엇을 쓰든 코드 동일 (이식성).
  - **Advisor** — `QuestionAnswerAdvisor` 하나로 3단계 자동. 서블릿 필터/인터셉터처럼 요청 파이프라인에 끼어듦.
- 비유: **RAG = LLM에게 오픈북 시험을 보게 하는 것.**

## 5. 그래서 백엔드 개발자의 역할

무게중심이 **"흐름을 직접 제어"에서 "위임하되 통제"로** 이동.

- 검증된 도구를 만든다 (`@Tool`).
- 지식을 공급한다 (RAG).
- 가드레일을 친다 (위험 작업 차단 / 사람 승인).
- 출력을 검증한다 (LLM 출력 = 외부 입력).

LLM은 오케스트레이터, 우리는 그것이 안전하게 놀 **운동장과 규칙**을 만든다.

## 그 외 (발표엔 안 넣었지만 알아둘 것)

- Observability: Micrometer 연동으로 토큰 사용량·레이턴시 등 관측.
- 멀티 벤더: OpenAI/Anthropic/Ollama 외 20+ 벤더, 20+ VectorStore 지원.
- 2.0은 마일스톤 단계(2.0.0-M7). 아직 GA 아님 → 발표는 1.1.x 기준.
