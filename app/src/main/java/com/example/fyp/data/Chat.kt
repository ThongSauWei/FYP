package com.example.fyp.data
data class Chat(
    var chatID: String,
    val initiatorLastSeen: String,
    val userID: String,
    val receivedLastSeen: String
) {
    constructor() : this("", "", "", "")
}
