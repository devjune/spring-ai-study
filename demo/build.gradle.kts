plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:2.0.0")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.ai:spring-ai-starter-model-anthropic")
    // RAG용 로컬 임베딩(ONNX) — 추가 API 키 없이 임베딩 생성
    implementation("org.springframework.ai:spring-ai-starter-model-transformers")
    // SimpleVectorStore(인메모리 벡터 저장소)
    implementation("org.springframework.ai:spring-ai-vector-store")
    // QuestionAnswerAdvisor (RAG)
    implementation("org.springframework.ai:spring-ai-vector-store-advisor")
    // ToolSearch — 대규모 도구에서 관련 도구만 선별 노출
    implementation("org.springframework.ai:spring-ai-tool-search-advisor")
    implementation("org.springframework.ai:spring-ai-tool-search-tool")
    // MCP 서버 (@McpTool) — MVC 기반
    implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")
    // 대화 메모리 영속화 — JDBC 저장소.
    // 운영에선 Postgres 등을 쓰지만, 데모는 설치 없이 돌리려고 H2 파일 모드를 쓴다.
    implementation("org.springframework.ai:spring-ai-starter-model-chat-memory-repository-jdbc")
    runtimeOnly("com.h2database:h2")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}
