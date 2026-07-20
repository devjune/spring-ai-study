package com.example.demo.observability

import com.example.demo.common.ChatRequest
import org.springframework.ai.chat.client.ChatClient
import org.springframework.web.bind.annotation.*

/**
 * 관측성 — 응답 메타데이터에서 토큰 사용량을 읽는다.
 * 문서 "핵심 기능 8. 관측성" 예제에 대응.
 */
@RestController
@RequestMapping("/api/observability")
class ObservabilityController(builder: ChatClient.Builder) {

    private val chatClient = builder.build()

    @PostMapping("/chat")
    fun chat(@RequestBody req: ChatRequest): Map<String, Any?> {
        val response = chatClient.prompt().user(req.message).call().chatResponse()!!
        val usage = response.metadata.usage
        return mapOf(
            "reply" to response.result?.output?.text,
            "promptTokens" to usage.promptTokens,
            "completionTokens" to usage.completionTokens,
            "totalTokens" to usage.totalTokens,
        )
    }
}
