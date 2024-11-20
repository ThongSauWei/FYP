package com.mainapp.finalyearproject.data

data class Chat(
    val chatID: String,
    val initiatorLastSeen: String,
    val userID: String,
    val receivedLastSeen: String
) {
    constructor() : this("", "", "", "")
}
