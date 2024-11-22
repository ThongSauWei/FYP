package com.example.fyp.data
data class Chat(
    val chatID: String,
    val initiatorLastSeen: String,
    val userID: String,
    val receivedLastSeen: String
) {
    constructor() : this("", "", "", "")
}
