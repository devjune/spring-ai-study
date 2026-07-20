package com.example.demo.memory

import com.example.demo.common.ConversationRequest
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.web.bind.annotation.*

/**
 * 대화 메모리 — MessageChatMemoryAdvisor로 대화 맥락 유지.
 * 2.0에서는 대화 ID를 호출 시점에 CONVERSATION_ID 로 넘기는 것이 필수.
 * 문서 "핵심 기능 3. 대화 메모리(ChatMemory)" 예제에 대응.
 */
@RestController
@RequestMapping("/api/memory")
class MemoryController(builder: ChatClient.Builder, chatMemory: ChatMemory) {

    private val chatClient = builder
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
        .build()

    @PostMapping("/chat")
    fun chat(@RequestBody req: ConversationRequest): Map<String, String?> =
        mapOf(
            "reply" to chatClient.prompt()
                .user(req.message)
                .advisors { it.param(ChatMemory.CONVERSATION_ID, req.conversationId) }
                .call()
                .content(),
        )
}
