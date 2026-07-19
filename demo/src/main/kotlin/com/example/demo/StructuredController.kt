package com.example.demo

import org.springframework.ai.chat.client.ChatClient
import org.springframework.web.bind.annotation.*

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
            .entity(Movie::class.java)!!
}
