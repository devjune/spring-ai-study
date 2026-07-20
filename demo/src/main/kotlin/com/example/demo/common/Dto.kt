package com.example.demo.common

/** 단순 채팅 요청. 대부분의 데모가 공용으로 쓴다. */
data class ChatRequest(val message: String)

/** 대화 ID를 포함한 요청 (ChatMemory 데모용) */
data class ConversationRequest(val conversationId: String, val message: String)
