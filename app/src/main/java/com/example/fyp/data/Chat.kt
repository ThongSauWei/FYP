package com.example.fyp.data
data class Chat(
    var chatID : String,
    val initiatorUserID : String,
    val receiverUserID : String,
    val initiatorLastSeen : String,
    val receiverLastSeen : String
) {
    constructor() : this("", "", "", "", "")
}

