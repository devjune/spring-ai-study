# Spring AI 2.0 데모

스터디 문서(`../spring-ai-study.md`)의 예제를 실제로 동작시키는 Spring Boot 앱.
**Spring Boot 4 + Spring AI 2.0.0 + Anthropic(claude-sonnet-4-5)**. 흰/검 단일 페이지 UI에서 각 기능을 눌러 확인한다.

## 요구사항

- JDK 21
- `ANTHROPIC_API_KEY` 환경변수 (API 키는 코드/설정에 넣지 않고 환경변수로만 주입)

## 실행

```bash
export ANTHROPIC_API_KEY=sk-ant-...
./gradlew bootRun
```

브라우저에서 http://localhost:8080 접속.

> 최초 실행 시 RAG용 임베딩 모델(all-MiniLM-L6-v2, ONNX)을 내려받는다(수십 MB, 로컬 실행이라 Anthropic 외 추가 키 불필요).

## 코드 구조

기능 하나가 패키지 하나다. 문서에서 궁금한 기능을 그 이름의 패키지에서 바로 찾을 수 있다.

```
com.example.demo
├── DemoApplication.kt
├── common/         ChatRequest, ConversationRequest (공용 DTO)
├── basic/          ChatClient 동기 호출 · 스트리밍 · 페르소나
├── structured/     .entity() + Movie 타입
├── memory/         MessageChatMemoryAdvisor + ChatMemory 빈
├── tool/           @Tool (WeatherTools)
├── rag/            QuestionAnswerAdvisor + VectorStore 시드
├── toolsearch/     ToolSearchToolCallingAdvisor (ManyTools)
├── mcp/            @McpTool — 이 앱을 MCP 서버로 노출
└── observability/  토큰 사용량 메타데이터
```

## 데모 ↔ 문서 예제 매핑

| UI 섹션 | 엔드포인트 | 문서 예제 |
| --- | --- | --- |
| 1. 기본 채팅 | `POST /api/basic/chat` | ChatClient 동기 호출 |
| 2. 스트리밍 | `GET /api/basic/stream` (SSE) | ChatClient `.stream()` |
| 3. 페르소나 | `POST /api/basic/persona` | 시스템 프롬프트 `.system()` |
| 4. Structured Output | `POST /api/structured/movie` | `.entity(Movie)` |
| 5. 대화 메모리 | `POST /api/memory/chat` | `MessageChatMemoryAdvisor` |
| 6. Tool Calling | `POST /api/tool/chat` | `@Tool` |
| 7. RAG | `POST /api/rag/chat` | `QuestionAnswerAdvisor` + `VectorStore` |
| 8. 관측성 | `POST /api/observability/chat` | 토큰 사용량 메타데이터 |
| ToolSearch | `POST /api/toolsearch/chat` | `ToolSearchToolCallingAdvisor` |
| MCP | `@McpTool`(add·echo) — MCP 서버로 노출 | `@McpTool` |

## 구현 메모

- **API 키** — `application.properties`에서 `${ANTHROPIC_API_KEY}`로 주입. 하드코딩 없음.
- **RAG 임베딩** — 로컬 ONNX(`spring-ai-starter-model-transformers`). 사내 문서 3건(환불 정책·영업시간)을 인메모리 `SimpleVectorStore`에 시드.
- **MCP** — `spring-ai-starter-mcp-server-webmvc`. `@McpTool`로 `add`, `echo`를 MCP 프로토콜로 노출(이 앱이 MCP 서버). 기동 로그의 `McpServerAnnotationScanner...` WARN은 무해한 BeanPostProcessor 경고.
- **ToolSearch** — 문서의 property 방식(`spring.ai.chat.client.tool-search-advisor.enabled`)은 현재 아티팩트만으로는 autoconfigure가 없어, `ToolSearchController`에서 `ToolSearchToolCallingAdvisor`를 regex 인덱스로 **수동 구성**했다.

## 검증 상태

- `./gradlew build` 성공.
- 앱 기동 성공 — 전체 빈 wiring(8개 컨트롤러 + MCP 서버 + ToolSearch + RAG 임베딩) 확인.
- HTML 서빙·엔드포인트 라우팅 확인.
- **실제 LLM 응답은 유효한 `ANTHROPIC_API_KEY`로 실행해 확인한다** (더미 키로는 Anthropic 인증 단계에서 실패).
