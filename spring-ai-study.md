# Spring AI 2.0 — 개념부터 에이전트까지

> 팀 내 AI 스터디 학습 자료
> 대상: Java/Spring 백엔드 개발자 (Spring AI를 처음 접하는 사람 포함)
> 기준 버전: Spring AI 2.0.0 GA (2026-06-12 릴리스)

이 문서는 Spring AI 2.0을 기준으로 핵심 개념과 사용법을 정리합니다. Spring AI를 처음 접하는 사람도 따라올 수 있도록 시작 설정부터 실제 조합 예제까지 다룹니다.

이 문서의 예제는 [`demo/`](demo/)에서 실제로 실행할 수 있습니다. `ANTHROPIC_API_KEY`만 있으면 되고, 브라우저 화면에서 기능별로 눌러 확인합니다.

---

## 들어가며

### Spring AI란

**Java/Spring 생태계에서 AI 모델을 통합하는 표준 추상화입니다.** Python 진영의 LangChain에 대응합니다.

LLM 앱 생태계는 대부분 Python으로 쏠렸지만, 엔터프라이즈 백엔드는 여전히 Java/Spring 위에서 동작합니다. Spring AI는 그 위에서 Spring 방식으로 AI를 통합합니다. 핵심 철학은 **이식성**입니다 — OpenAI든 Anthropic이든 로컬 Ollama든, 코드는 그대로 두고 의존성·설정만 바꿔 모델을 교체할 수 있습니다. `JdbcTemplate`이 DB 벤더를 추상화하던 발상과 같습니다.

### 1.x에서 2.0으로

- **1.0 (2025-05)** — 기본 추상화 등장: `ChatClient`, Structured Output, Tool Calling, RAG.
- **1.1 (2025-11)** — 안정화, Advisor 체계 정비.
- **2.0 (2026-06)** — Spring Boot 4 기반으로 토대를 다시 세우고, 무게중심이 **에이전트(Agent)** 로 이동. Tool Calling 표준화, ToolSearch, MCP가 핵심.

이 문서는 2.0을 기준으로 설명하며, 1.x와 달라진 부분을 함께 표시합니다.

---

## 무엇을, 누가 만들 수 있나

Spring AI로 만들 수 있는 대표적인 유스케이스와 대상은 다음과 같습니다.

### 무엇을 (유스케이스)

| 유스케이스 | 설명 | 핵심 기능 |
| --- | --- | --- |
| **챗봇** | 대화 맥락을 기억하는 대화형 응답 | ChatClient + ChatMemory |
| **구조화 추출·분류** | 비정형 텍스트 → 타입 객체 (감정 분류, 필드 추출) | Structured Output |
| **사내 문서 Q&A (RAG)** | 회사 문서에 근거해 답하기 | RAG + VectorStore |
| **에이전트** | LLM이 실제 함수·시스템을 호출해 일을 처리 | Tool Calling |
| **도구·서비스 상호운용** | 외부/타 팀 도구를 표준 프로토콜로 연결 | MCP |
| **멀티모달** | 이미지·오디오 입출력 | 모델별 기능 |

### 누가

- **Java/Spring 백엔드 개발자** — 기존 Spring 앱에 AI 기능을 붙이려는 사람.
- **엔터프라이즈 팀** — 관측성·이식성·트랜잭션 등 운영 요건이 중요한 조직.
- Python 서비스를 따로 띄우지 않고 **한 스택에서** AI를 다루고 싶은 팀.

### 지원 범위

- **모델 종류** — Chat, Embedding, Text-to-Image, Audio Transcription, Text-to-Speech, Moderation.
- **벤더** — Anthropic, OpenAI, Azure(Microsoft), Amazon Bedrock, Google, Ollama, Mistral, DeepSeek 등.
- **VectorStore** — pgvector, Redis, Chroma 등 다수.

---

## 큰 그림

```mermaid
graph LR
    App["Spring Boot 4 App"] --> CC["ChatClient"]
    CC --> SO["Structured Output"]
    CC --> Mem["ChatMemory<br/>(대화 기억)"]
    CC --> Adv["Advisors"]
    Adv --> RAG["RAG<br/>(QuestionAnswerAdvisor)"]
    Adv --> Tools["Tool Calling<br/>(+ ToolSearch)"]
    Tools --> MCP["MCP<br/>(외부 도구)"]
    CC --> CM["ChatModel<br/>(벤더 구현 · 교체 지점)"]
    CM --> LLM["LLM<br/>(Anthropic / OpenAI / Ollama ...)"]
```

**`ChatClient`는 모든 LLM 호출이 통과하는 인터페이스입니다.** RAG·Tool·메모리 같은 기능은 그 위에 **Advisor**로 얹히고, 실제 벤더 호출은 그 아래 **`ChatModel`** 구현체가 맡습니다.

이 두 층으로 나뉘어 있어서 이식성이 성립합니다. 벤더를 바꾼다는 것은 `ChatModel` 구현체가 바뀐다는 뜻이고, 그 위의 호출 코드는 그대로입니다. 앞서 든 `JdbcTemplate` 비유가 여기에 해당합니다 — 인터페이스는 그대로 두고 드라이버만 갈아 끼우는 구조입니다.

각 요소는 아래에서 하나씩 다룹니다.

---

## 시작하기

**의존성 1개 + 설정 2줄이면 첫 호출을 할 수 있습니다.**

### 1. 의존성

```kotlin
implementation("org.springframework.ai:spring-ai-starter-model-anthropic")
```

> 벤더를 바꾸려면 스타터만 교체합니다. OpenAI는 `spring-ai-starter-model-openai`, 로컬은 `spring-ai-starter-model-ollama`. **코드는 그대로입니다.**

### 2. 설정

```properties
spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY}
spring.ai.anthropic.chat.options.model=claude-sonnet-4-5
```

### 3. 첫 호출

`ChatClient.Builder`가 자동으로 빈 등록됩니다. 이를 주입받아 사용합니다.

```kotlin
@RestController
class ChatController(builder: ChatClient.Builder) {
    private val chatClient = builder.build()

    @GetMapping("/ai")
    fun ai(@RequestParam message: String): String =
        chatClient.prompt()
            .user(message)
            .call()
            .content() ?: ""      // 2.0의 응답 메서드는 nullable
}
```

`prompt().user(...).call().content()` — 이 fluent 체인이 Spring AI의 심장입니다. `WebClient.Builder`를 주입받아 커스터마이징하던 패턴과 동일합니다.

---

## 핵심 기능 (2.0 기준 상세)

각 기능을 개념, 예제, 1.x 대비 변화 순으로 정리합니다.

### 1. ChatClient — LLM 호출의 중심

**모든 LLM 호출이 통과하는 fluent API입니다.** 호출 방식·응답 형태·시스템 프롬프트를 여기서 다룹니다.

#### 응답 받는 세 가지 방법

```kotlin
// 1) 텍스트
val text: String? = chatClient.prompt().user(msg).call().content()

// 2) 전체 응답 (토큰 사용량 등 메타데이터 포함)
val resp: ChatResponse? = chatClient.prompt().user(msg).call().chatResponse()
val tokens = resp?.metadata?.usage?.totalTokens

// 3) 타입 객체 (뒤 Structured Output 참고)
val movie: Movie? = chatClient.prompt().user(msg).call().entity(Movie::class.java)
```

> `.call()` 자체는 모델을 호출하지 않습니다. `.content()` / `.chatResponse()` / `.entity()`를 호출하는 순간 실제 요청이 나갑니다.

**세 메서드 모두 반환 타입이 nullable입니다.** 2.0은 JSpecify로 널 안전성을 API에 명시했고(`@Nullable`), Kotlin에서는 이것이 `String?` · `ChatResponse?` · `Movie?`로 그대로 드러납니다. 호출부에서 처리해야 합니다.

```kotlin
.content() ?: ""                                  // 기본값
.chatResponse() ?: error("Model returned no response")   // 실패로 처리
```

Java에서는 컴파일 오류가 나지 않지만 `@Nullable` 계약은 동일하므로, null 처리는 여전히 호출부의 책임입니다.

#### 스트리밍

LLM 응답은 수 초가 걸립니다. 다 만들어질 때까지 기다렸다 한 번에 주면 사용자는 멈춘 화면을 보게 되므로, 토큰이 오는 대로 흘려보내 첫 글자까지의 시간을 줄입니다(타이핑 효과).

`.call()` 대신 `.stream()`을 쓰면 반환 타입이 `Flux`가 됩니다.

```kotlin
val flux: Flux<String> = chatClient.prompt().user(msg).stream().content()
```

이 `Flux`를 브라우저까지 내보내려면 컨트롤러에서 **SSE**(Server-Sent Events)로 반환합니다. `produces`를 지정하는 것이 핵심입니다.

```kotlin
@GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
fun stream(@RequestParam message: String): Flux<String> =
    chatClient.prompt().user(message).stream().content()
```

- **WebFlux가 필수는 아닙니다.** Spring MVC도 리액티브 반환 타입을 처리하므로 `spring-boot-starter-web`만으로 동작합니다.
- 브라우저 쪽은 `EventSource`로 받습니다.

```javascript
const es = new EventSource('/api/basic/stream?message=' + encodeURIComponent(msg));
es.onmessage = e => out.textContent += e.data;
es.onerror = () => es.close();
```

> 스트리밍에는 제약이 따릅니다. `.entity()`(Structured Output)처럼 응답 전체가 모여야 성립하는 기능은 함께 쓸 수 없고, 토큰 사용량 같은 메타데이터도 스트림이 끝나야 확정됩니다.

#### 시스템 프롬프트 (페르소나)

빌더의 `defaultSystem()`에 기본 페르소나를 박아두고, 호출 시점 `.system()`으로 덮어씁니다. `WebClient`의 `defaultHeader()`와 같은 계층 구조입니다.

```kotlin
@Bean
fun chatClient(builder: ChatClient.Builder): ChatClient =
    builder.defaultSystem("너는 친절한 고객 지원 상담원이다.").build()
```

### 2. Structured Output — 응답을 타입 객체로

**`.content()` 대신 `.entity()` 한 줄로, LLM 응답을 타입 객체로 받습니다.** LLM을 타입 안전한 함수처럼 쓰는 것입니다.

```kotlin
data class Movie(val title: String, val year: Int, val director: String)

val movie: Movie = chatClient.prompt()
    .user("SF 영화 하나 추천해줘")
    .call()
    .entity(Movie::class.java)
    ?: error("Failed to convert model response to Movie")
```

- 내부 동작: data class로 **스키마 자동 생성 → 프롬프트에 주입 → LLM이 JSON 응답 → `BeanOutputConverter`가 역직렬화**.
- 컬렉션은 `entity(object : ParameterizedTypeReference<List<Movie>>() {})`로 받습니다.
- `.entity()` 자체는 재시도하지 않습니다. 재시도는 `StructuredOutputValidationAdvisor`가 별도로 담당합니다.
- 일부 벤더는 스키마를 강제하는 네이티브 모드를 지원합니다(`AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT`).

> **LLM 출력을 신뢰하지 말고 외부 입력처럼 다룹니다.** 객체로 받았어도 값 검증(범위·필수값)은 애플리케이션의 책임입니다.

### 3. 대화 메모리 (ChatMemory) — 맥락을 기억하기

**LLM은 기본적으로 이전 대화를 기억하지 못합니다.** 매 호출이 독립적입니다. 대화형 챗봇을 만들려면 이전 메시지를 다시 넣어줘야 합니다. `ChatMemory` + `MessageChatMemoryAdvisor`가 이를 자동화합니다.

```kotlin
// builder: ChatClient.Builder, chatMemory: ChatMemory 를 주입받는다
val chatClient = builder
    .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
    .build()

// 호출 시 대화 ID를 넘긴다 (세션 구분)
chatClient.prompt()
    .user("방금 뭐라고 했지?")
    .advisors { it.param(ChatMemory.CONVERSATION_ID, "user-42") }
    .call()
    .content()
```

> **1.x → 2.0 변화** — 대화 ID를 빌더의 `.conversationId()`에 넣던 방식이 사라지고, **호출 시점에 `ChatMemory.CONVERSATION_ID`로 넘기는 방식이 필수**가 됐습니다. 누락하면 예외가 납니다.

#### 대화를 어디에 저장할 것인가

위 예제의 `MessageWindowChatMemory`는 기본적으로 **메모리에만** 담습니다. 서버를 재시작하면 대화가 사라지고, 인스턴스를 2대로 늘리면 어느 서버로 붙느냐에 따라 맥락이 갈립니다. 운영에서는 저장소를 붙여야 합니다.

역할이 둘로 나뉘어 있습니다.

- **`ChatMemory`** — 무엇을 기억할지 정하는 **정책** (예: 최근 N개 메시지만 유지)
- **`ChatMemoryRepository`** — 그것을 어디에 둘지 정하는 **저장소** (JDBC, Redis, MongoDB, Cassandra, Neo4j)

```kotlin
@Bean
fun chatMemory(chatMemoryRepository: ChatMemoryRepository): ChatMemory =
    MessageWindowChatMemory.builder()
        .chatMemoryRepository(chatMemoryRepository)   // 저장소만 갈아 끼운다
        .build()
```

저장소는 스타터로 선택합니다. 스타터와 설정만 바꾸면 되고 **위 코드는 그대로**입니다.

```kotlin
implementation("org.springframework.ai:spring-ai-starter-model-chat-memory-repository-jdbc")
```

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/app
spring.ai.chat.memory.repository.jdbc.initialize-schema=always
```

JDBC 저장소는 플랫폼별 스키마(postgresql, mysql, mariadb, oracle, sqlserver, h2 등)를 내장하고 있고, `JdbcChatMemoryRepositoryProperties`가 Spring Boot의 `DatabaseInitializationProperties`를 상속하므로 `initialize-schema` 같은 **기존 Boot의 스키마 초기화 방식을 그대로** 씁니다. 애플리케이션의 `DataSource`를 공유하는 구조입니다.

> 데모 앱은 설치 없이 실행되도록 같은 JDBC 저장소를 H2 파일 모드(`jdbc:h2:file:./data/chatmemory`)로 씁니다. 앱을 재시작해도 대화가 유지되는 것을 확인할 수 있습니다.

### 4. Tool Calling — LLM이 함수를 호출한다

Tool Calling은 LLM에게 함수 목록을 제공하고, 필요할 때 LLM이 그 함수를 호출하게 하는 기능입니다.

```mermaid
sequenceDiagram
    participant U as 사용자
    participant App as Spring AI
    participant LLM as LLM
    participant Fn as 내 함수 (@Tool)
    U->>App: "서울 날씨 어때?"
    App->>LLM: 질문 + 사용 가능한 도구 목록
    LLM->>App: getWeather("서울") 호출 요청
    App->>Fn: 실제 함수 실행
    Fn->>App: "맑음, 22도"
    App->>LLM: 함수 결과 전달
    LLM->>App: "서울은 맑고 22도입니다"
    App->>U: 최종 답변
```

도구는 `@Tool` 애노테이션 하나로 정의합니다. `description`이 LLM에게 "언제 이 도구를 쓸지" 알려주는 힌트라 가장 중요합니다.

```kotlin
class WeatherTools {
    @Tool(description = "특정 도시의 현재 날씨를 조회한다")
    fun getWeather(@ToolParam(description = "도시 이름") city: String): String {
        // 실제 조회 로직 (DB, 외부 API 등)
        return "$city: 맑음, 22도"
    }
}

chatClient.prompt()
    .user("서울 날씨 어때?")
    .tools(WeatherTools())   // @Tool 메서드를 가진 객체를 넘긴다
    .call()
    .content()
```

2.0에서는 함수 등록 방식이 하나로 정리됐습니다. 1.x에서는 등록 방식이 여러 갈래(`Function` 빈, `FunctionCallback`, `@Tool`)로 나뉘어 있었습니다.

| | 1.x | 2.0 |
| --- | --- | --- |
| 등록 방식 | `Function` 빈 · `FunctionCallback` · `@Tool` 혼재 | `ToolCallback`로 일원화 (`FunctionCallback` 제거) |
| 실행 루프 | `internalToolExecutionEnabled` 수동 토글 | `ToolCallingAdvisor` 1급 컴포넌트 + 자동 등록 |

`.tools()`만 넘기면 Advisor가 "호출 요청 → 함수 실행 → 결과 재주입" 루프를 자동 처리합니다.

> **제어의 역전(IoC).** 기존 백엔드는 개발자가 모든 분기를 작성했습니다(`if A else B`). Tool Calling은 "도구를 제공할 테니 목표를 달성하라"고 **위임**하는 방식입니다. LLM이 오케스트레이터가 되고, 개발자는 검증된 도구만 제공합니다. 이것이 **에이전트의 본질**입니다.

### 5. RAG — 회사 내부 문서에 근거해 답하기

**LLM은 특정 조직의 내부 문서를 알지 못합니다.** 내부 정책을 물으면 사실과 다른 내용을 생성합니다(할루시네이션). RAG는 답하기 전에 관련 문서를 검색해 프롬프트에 끼워 넣는 방식으로, 모델은 그대로 두고 지식만 주입합니다.

- **Retrieve(검색)** — 질문과 의미가 가까운 문서를 `VectorStore`에서 찾습니다.
- **Augment(증강)** — 찾은 문서를 질문과 함께 프롬프트에 붙입니다.
- **Generate(생성)** — LLM이 그 문서를 근거로 답합니다.

Spring AI에서는 `QuestionAnswerAdvisor` 하나만 붙이면 이 흐름이 자동으로 돕니다.

```kotlin
chatClient.prompt()
    .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())  // 이 한 줄이 RAG
    .user("우리 환불 정책이 뭐야?")
    .call()
    .content()
```

증강(Augment)의 실체는 **검색한 문서를 프롬프트 텍스트에 붙이는 것**입니다. LLM이 실제로 받는 프롬프트는 이렇게 바뀝니다.

```
아래 컨텍스트를 근거로 답하라.
[컨텍스트: "환불은 구매 후 7일 이내… 디지털 상품은 제외…"]
[질문] 우리 환불 정책이 뭐야?
```

#### VectorStore — 검색을 담당하는 부품

`VectorStore`는 문서를 **임베딩(의미를 담은 벡터)** 으로 저장하고 유사도로 검색하는 **공통 인터페이스**입니다.

```kotlin
vectorStore.add(documents)                    // 문서 저장 (임베딩 자동 생성)
vectorStore.similaritySearch("환불 정책")       // 의미가 가까운 문서 top-N 검색
```

실제 저장소(pgvector, Redis, Chroma 등)는 구현체를 갈아 끼우는 식이라, 어느 것을 쓰든 앱 코드는 그대로입니다. 의존성만 추가하면 됩니다(`spring-ai-autoconfigure-vector-store-pgvector` 등).

> **RAG = 모델 재학습 없이 지식만 실시간 주입.** 파인튜닝보다 싸고 빠르며, 문서가 바뀌면 DB만 갱신하면 됩니다.

### 6. ToolSearch — 도구가 수백 개일 때 (2.0 신규)

**도구가 수백 개여도, 질문에 맞는 것만 골라 노출합니다.** 도구를 전부 프롬프트에 넣으면 토큰이 커지고 LLM의 선택 정확도가 떨어집니다. 2.0의 `ToolSearchToolCallingAdvisor`는 이 문제를 점진적 노출(progressive tool disclosure)로 풉니다.

Advisor를 직접 구성해 필요한 호출에만 붙입니다.

```kotlin
private val toolSearchAdvisor = ToolSearchToolCallingAdvisor.builder()
    .toolIndex(RegexToolIndex())                     // regex / lucene / vector
    .toolCallingManager(ToolCallingManager.builder().build())
    .build()

chatClient.prompt()
    .advisors(toolSearchAdvisor)
    // 세션별 도구 인덱스를 캐시하므로 세션 ID가 필요하다
    .advisors { it.param(ChatMemory.CONVERSATION_ID, "toolsearch-demo") }
    .tools(ManyTools())
    .user("사용자 준의 최근 주문 상태 알려줘")
    .call()
    .content()
```

RAG가 문서를 검색해 주입하듯, 도구를 검색해 주입하는 셈입니다.

`spring.ai.chat.client.tool-search-advisor.enabled=true` 프로퍼티로 자동 구성하는 경로도 있습니다. 다만 2.0.0 기준으로 아래 세 가지를 감안해야 합니다.

- **적용 범위가 전역입니다.** 이 프로퍼티는 기본 `ToolCallingAdvisor`를 앱 전체에서 교체합니다. 켜는 순간 도구를 쓰지 않는 호출까지 세션 ID를 요구하므로(`chat_memory_conversation_id`), 앱 전체가 ToolSearch를 쓸 때만 적합합니다.
- **starter가 별도이고 BOM에 없습니다.** autoconfigure는 `spring-ai-starter-tool-search-advisor`에 들어 있는데(라이브러리인 `spring-ai-tool-search-advisor`에는 없습니다), `spring-ai-bom:2.0.0`이 이 starter를 관리하지 않아 버전을 직접 지정해야 합니다.
- **조용히 건너뛸 수 있습니다.** Advisor 빈은 `@ConditionalOnBean(ToolCallingManager, ToolIndex)` 조건인데, 이 autoconfigure가 `ToolCallingAutoConfiguration`보다 먼저 평가되어 `ToolCallingManager`를 아직 못 찾는 경우가 있습니다. 프로퍼티는 켜졌는데 Advisor는 등록되지 않고 오류도 나지 않습니다. `ToolCallingManager`를 직접 빈으로 선언하면(사용자 빈이 먼저 등록됩니다) 해결됩니다.

### 7. MCP — 도구의 상호운용 표준 (2.0 핵심)

**애플리케이션 밖에 흩어진 도구까지 표준 프로토콜로 연결합니다.**

앞에서 다룬 도구는 모두 애플리케이션 내부 함수였습니다. 실제 도구는 여러 곳에 흩어져 있습니다(자체 서비스, 다른 팀 서비스, 외부 SaaS). MCP(Model Context Protocol)는 도구·리소스·프롬프트를 노출하는 표준 프로토콜로, 규격만 맞추면 제공자·소비자가 서로의 구현을 몰라도 연결됩니다.

```mermaid
graph LR
    Agent["내 에이전트<br/>(MCP Client)"] --> P((MCP 프로토콜))
    P --> S1["우리 팀 서비스<br/>(MCP Server)"]
    P --> S2["옆 팀 서비스<br/>(MCP Server)"]
    P --> S3["외부 SaaS<br/>(MCP Server)"]
```

Spring AI 2.0은 MCP를 1급으로 지원합니다.

- **서버 애노테이션** — `@McpTool` / `@McpResource` / `@McpPrompt` / `@McpComplete`. `@Tool`처럼 선언만 하면 함수가 MCP 서버로 노출되고, JSON 스키마는 자동 생성됩니다.
- **클라이언트** — 다른 곳에서 만든 MCP 서버를 내 에이전트의 도구로 가져다 씁니다.
- **전송 편입** — 기존에 MCP Java SDK에 있던 전송(webflux/webmvc)이 Spring AI 프로젝트로 이동했습니다(그룹 id `io.modelcontextprotocol.sdk` → `org.springframework.ai`, breaking change). 기본 전송은 Streamable HTTP입니다.

```kotlin
@Component
class CalculatorTools {
    @McpTool(name = "add", description = "두 수를 더한다")
    fun add(
        @McpToolParam(description = "첫 번째 수") a: Int,
        @McpToolParam(description = "두 번째 수") b: Int,
    ): Int = a + b
}
```

> 1.0의 이식성은 "벤더를 바꿔도 코드 유지"였습니다. MCP는 이식성을 **"도구와 에이전트가 벤더·언어를 넘어 연결된다"** 로 확장합니다. 한 서비스가 다른 에이전트의 도구가 되고, 반대로 다른 도구가 그 에이전트의 수단이 됩니다.

### 8. Advisor 직접 만들기

**RAG, 대화 메모리, ToolSearch는 모두 Advisor였습니다.** `QuestionAnswerAdvisor`, `MessageChatMemoryAdvisor`, `ToolSearchToolCallingAdvisor` — 앞에서 `.advisors(...)`로 붙인 것들이 전부 같은 확장점입니다. 직접 만들 수도 있습니다.

Advisor는 `ChatClient` 호출을 가로채는 체인입니다. **서블릿 필터나 `HandlerInterceptor`와 같은 자리**로, 요청이 모델로 나가기 전과 응답이 돌아온 후에 개입합니다.

```mermaid
graph LR
    App["ChatClient 호출"] --> A1["Advisor 1<br/>before"]
    A1 --> A2["Advisor 2<br/>before"]
    A2 --> LLM["LLM"]
    LLM --> B2["Advisor 2<br/>after"]
    B2 --> B1["Advisor 1<br/>after"]
    B1 --> Res["응답"]
```

`BaseAdvisor`를 구현하면 `before`/`after` 두 개만 채우면 됩니다.

```kotlin
// CARD_NUMBER(정규식) · MASKED_PROMPT(컨텍스트 키) 상수 정의는 생략. 전체는 demo/ 참고
class PiiMaskingAdvisor(private val order: Int = 0) : BaseAdvisor {

    // 모델로 나가기 전 — 카드번호를 마스킹한다
    override fun before(request: ChatClientRequest, chain: AdvisorChain): ChatClientRequest {
        val original = request.prompt().userMessage.text ?: return request
        val masked = CARD_NUMBER.replace(original, "****-****-****-****")
        if (masked == original) return request

        return request.mutate()
            .prompt(request.prompt().augmentUserMessage { it.mutate().text(masked).build() })
            .context(MASKED_PROMPT, masked)   // 호출부가 확인할 수 있게 기록
            .build()
    }

    // 모델에서 돌아온 후 — 여기선 통과
    override fun after(response: ChatClientResponse, chain: AdvisorChain) = response

    override fun getOrder(): Int = order
}
```

```kotlin
chatClient.prompt()
    .advisors(PiiMaskingAdvisor(order = 0), SimpleLoggerAdvisor.builder().build())
    .user(message)
    .call()
    .chatClientResponse()
```

- **실행 순서** — `getOrder()`가 작은 쪽이 먼저 `before`를 실행합니다. `after`는 역순입니다.
- **컨텍스트** — `ChatClientRequest`/`ChatClientResponse`는 `context` 맵을 함께 나릅니다. Advisor가 남긴 값을 호출부에서 `chatClientResponse().context()`로 읽을 수 있습니다.
- **기본 제공** — `SimpleLoggerAdvisor`는 요청·응답을 로그로 남깁니다(DEBUG). 별도 구현 없이 붙이기만 하면 됩니다.

이 확장점이 있는 덕분에, 감사 로그·마스킹·비용 측정·금칙어 필터 같은 **횡단 관심사를 비즈니스 코드 밖에서** 처리할 수 있습니다. 문서 마지막에서 이야기하는 "가드레일"이 실제로 구현되는 자리이기도 합니다.

### 9. 관측성

운영 환경에서는 토큰 사용량과 비용 추적이 중요합니다. Spring AI는 Micrometer와 연동해 토큰 사용량·레이턴시를 관측하며, 토큰·캐시 지표를 통합 API로 제공합니다.

토큰 사용량은 `.content()` 대신 `.chatResponse()`로 받아 메타데이터에서 읽습니다.

```kotlin
val response = chatClient.prompt().user(msg).call().chatResponse()
    ?: error("Model returned no response")

val usage = response.metadata.usage
usage.promptTokens                // 입력 토큰
usage.completionTokens            // 출력 토큰
usage.totalTokens                 // 합계
usage.cacheReadInputTokens        // 캐시로 읽은 토큰 (2.0 통합 지표)
```

---

## 직접 만들어 보기 — 사내 문서 Q&A 에이전트

앞의 기능들을 하나의 `ChatClient`에 결합한 예시입니다. RAG(사내 문서), 대화 메모리, 실제 조회 도구(Tool)를 함께 얹으면 에이전트가 됩니다.

```kotlin
@Bean
fun supportAgent(
    builder: ChatClient.Builder,
    vectorStore: VectorStore,
    chatMemory: ChatMemory,
): ChatClient =
    builder
        .defaultSystem("너는 사내 고객지원 상담원이다. 문서에 근거해 답하라.")
        .defaultAdvisors(
            QuestionAnswerAdvisor.builder(vectorStore).build(),      // RAG: 사내 문서 근거
            MessageChatMemoryAdvisor.builder(chatMemory).build(),    // 대화 맥락 유지
        )
        .defaultTools(OrderTools())                                  // 실제 주문 조회 도구
        .build()
```

이 하나의 에이전트가 이렇게 동작합니다.

1. 사용자가 "지난주 주문 환불돼요?"라고 묻는다.
2. **RAG** — 환불 정책 문서를 검색해 근거로 붙인다.
3. **Tool** — LLM이 `getOrder(userId)`를 호출해 실제 주문 상태를 조회한다.
4. **Memory** — 이전 대화("지난주 주문"이 무엇인지)를 기억한다.
5. 정책 + 실제 데이터에 근거한 답을 만든다.

개발자가 만드는 것은 검증된 도구와 신뢰할 수 있는 지식이며, 그것들을 언제 어떻게 조합할지는 LLM에 위임합니다. 이것이 에이전트의 본질입니다.

---

## 1.x → 2.0 한눈에

| 영역           | 1.x                | 2.0                                         |
| ------------ | ------------------ | ------------------------------------------- |
| 런타임          | Spring Boot 3      | **Spring Boot 4 / Framework 7 / Jackson 3** |
| 널 안전성        | 명시 없음              | **JSpecify `@Nullable`** (Kotlin에서 `String?`로 드러남) |
| Tool Calling | 등록 방식 혼재           | `ToolCallback` 일원화, Advisor 자동              |
| 대규모 도구       | —                  | **ToolSearch** (점진적 노출)                     |
| MCP          | SDK 별도             | **Spring AI 1급 편입** (`@McpTool`, 전송 내장)     |
| 대화 메모리       | 빌더에 conversationId | 호출 시점 `CONVERSATION_ID` 필수                  |
| 토큰·캐시 지표     | 벤더별 제각각            | 통합 `Usage` API                              |
| 벤더           | 변형 다수              | 정리 (예: OpenAI 3종 → 1종)                      |

Spring AI는 **"LLM 호출 라이브러리"에서 "에이전트 플랫폼"으로** 이동했습니다.

---

## 정리 — 백엔드 개발자의 역할

AI를 도입하면 백엔드 개발자의 역할이 바뀝니다. 더 이상 모든 분기를 직접 작성하지 않습니다.

- **검증된 도구를 만든다** — LLM이 호출할 안전한 함수(`@Tool`), 필요하면 `@McpTool`로 표준 노출까지.
- **지식을 공급한다** — RAG로 신뢰할 수 있는 데이터를 주입.
- **가드레일을 친다** — 위험 작업은 도구로 만들지 않거나, 노출을 통제(ToolSearch)하거나, 사람 승인(human-in-the-loop)을 끼운다.
- **출력을 검증한다** — LLM 출력을 외부 입력처럼 다룬다.

무게중심이 **"흐름을 직접 제어"에서 "위임하되 통제"로** 이동합니다. LLM이 오케스트레이터라면, 백엔드는 그것이 안전하게 동작할 **범위와 규칙**을 정의합니다.

**관통하는 두 테마**

- **이식성** — 벤더/DB 교체를 넘어, 이제 도구·에이전트의 상호운용(MCP)까지.
- **위임 vs 통제** — LLM에 얼마나 맡기고, 어디에 가드레일을 칠 것인가. 에이전트가 커질수록 이 질문이 더 중요해집니다.

---

## 참고

- Spring AI Reference (2.0): https://docs.spring.io/spring-ai/reference/2.0/
- Spring AI 2.0 업그레이드 노트: https://docs.spring.io/spring-ai/reference/upgrade-notes.html#upgrading-to-2-0-0
- Spring AI 2.0.0 GA 블로그: https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now
- Spring AI 1.0 GA 블로그: https://spring.io/blog/2025/05/20/spring-ai-1-0-GA-released/
- Spring AI 1.1 GA 블로그: https://spring.io/blog/2025/11/12/spring-ai-1-1-GA-released/
- 기준 버전: Spring AI 2.0.0 GA (이 자료의 예제는 Anthropic 스타터에 `claude-sonnet-4-5`를 지정해 사용)
