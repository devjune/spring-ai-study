package com.example.demo

/** 단순 채팅 요청 */
data class ChatRequest(val message: String)

/** 대화 ID를 포함한 요청 (ChatMemory 데모용) */
data class ConversationRequest(val conversationId: String, val message: String)

/** Structured Output 데모용 응답 타입 */
data class Movie(val title: String, val year: Int, val director: String)
