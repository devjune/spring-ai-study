package com.example.demo.structured

import com.example.demo.common.ChatRequest
import org.springframework.ai.chat.client.ChatClient
import org.springframework.web.bind.annotation.*

/** LLM 응답을 담을 타입. 필드 이름·타입이 그대로 JSON 스키마가 된다. */
data class Movie(val title: String, val year: Int, val director: String)

/**
 * Structured Output — .entity()로 응답을 타입 객체로 받는다.
 * 문서 "핵심 기능 2. Structured Output" 예제에 대응.
 */
@RestController
@RequestMapping("/api/structured")
class StructuredController(builder: ChatClient.Builder) {

    private val chatClient = builder.build()

    @PostMapping("/movie")
    fun movie(@RequestBody req: ChatRequest): Movie =
        chatClient.prompt()
            .user(req.message)
            .call()
            .entity(Movie::class.java)
            ?: error("Failed to convert model response to Movie")
}
