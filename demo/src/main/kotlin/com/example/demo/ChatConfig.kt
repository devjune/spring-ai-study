package com.example.demo

import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ChatConfig {

    /** 대화 메모리 (창 크기만큼 최근 메시지 유지) */
    @Bean
    fun chatMemory(): ChatMemory = MessageWindowChatMemory.builder().build()
}
