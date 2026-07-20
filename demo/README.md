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
├── observability/  토큰 사용량 메타데이터
└── advisor/        커스텀 Advisor (BaseAdvisor) — 카드번호 마스킹 가드레일
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
| 8. ToolSearch | `POST /api/toolsearch/chat` | `ToolSearchToolCallingAdvisor` |
| 9. 관측성 | `POST /api/observability/chat` | 토큰 사용량 메타데이터 |
| 10. MCP | `@McpTool`(add·echo) — MCP 서버로 노출 | `@McpTool` |
| 11. 커스텀 Advisor | `POST /api/advisor/chat` | `BaseAdvisor` (before/after) |

## 구현 메모

- **API 키** — `application.properties`에서 `${ANTHROPIC_API_KEY}`로 주입. 하드코딩 없음.
- **RAG 임베딩** — 로컬 ONNX(`spring-ai-starter-model-transformers`). 사내 문서 3건(환불 정책·영업시간)을 인메모리 `SimpleVectorStore`에 시드.
- **대화 메모리 영속화** — `spring-ai-starter-model-chat-memory-repository-jdbc` + H2 **파일** 모드(`./data/chatmemory`). 도커 없이 실행하려고 H2를 썼고, 운영이라면 같은 코드에 Postgres 설정만 바꾸면 된다. 스키마는 starter 내장 `schema-h2.sql`을 `initialize-schema=always`로 생성한다. `data/`는 gitignore 대상.
- **MCP** — `spring-ai-starter-mcp-server-webmvc`. `@McpTool`로 `add`, `echo`를 MCP 프로토콜로 노출(이 앱이 MCP 서버). 기동 로그의 `McpServerAnnotationScanner...` WARN은 무해한 BeanPostProcessor 경고.

### MCP 시연 방법 (10번 섹션)

10번만 화면에 버튼이 없다. MCP는 브라우저가 아니라 **다른 에이전트가 붙는** 프로토콜이고, 그게 이 섹션의 요지다. Claude Code를 클라이언트로 붙여 시연한다.

```bash
claude mcp add --transport http demo http://localhost:8080/mcp
claude mcp list                    # demo ... ✔ Connected
# 대화창에서: "demo 서버의 add 로 7 더하기 5 해줘"
claude mcp remove demo             # 정리
```

**API 키가 없어도 동작한다.** 도구 목록·실행은 LLM을 거치지 않으므로, `ANTHROPIC_API_KEY`가 잘못돼 다른 섹션이 401로 실패하는 상황에서도 이 섹션은 정상이다.

터미널 없이 확인하려면 JSON-RPC를 직접 호출한다. `initialize` 응답 헤더의 `Mcp-Session-Id`를 이후 요청에 실어야 한다.

```bash
curl -s -D /tmp/h.txt -X POST http://localhost:8080/mcp \
  -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"curl","version":"1"}}}'

SID=$(tr -d '\r' < /tmp/h.txt | awk 'tolower($1)=="mcp-session-id:"{print $2}')
curl -s -X POST http://localhost:8080/mcp \
  -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' \
  -H "Mcp-Session-Id: $SID" -d '{"jsonrpc":"2.0","id":2,"method":"tools/list"}'
```
- **ToolSearch** — `ToolSearchController`에서 `ToolSearchToolCallingAdvisor`를 regex 인덱스로 수동 구성했다.
  property 방식(`spring.ai.chat.client.tool-search-advisor.enabled`)도 실재하지만, 켜면 기본 `ToolCallingAdvisor`가 **전역** 교체돼 도구를 쓰지 않는 엔드포인트(1번 기본 채팅, 7번 RAG 등)까지 세션 ID를 요구하며 실패한다. 섹션별로 독립 실행돼야 하는 데모라 수동 구성을 택했다. 자세한 사정은 스터디 문서 "6. ToolSearch" 참고.

## 검증 상태

- `./gradlew build` 성공.
- 앱 기동 성공 — 전체 빈 wiring(8개 컨트롤러 + MCP 서버 + ToolSearch + RAG 임베딩) 확인.
- HTML 서빙·엔드포인트 라우팅 확인.
- **실제 LLM 응답은 유효한 `ANTHROPIC_API_KEY`로 실행해 확인한다** (더미 키로는 Anthropic 인증 단계에서 실패).
