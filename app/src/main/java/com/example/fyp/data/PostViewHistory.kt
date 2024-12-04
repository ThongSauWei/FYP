package com.example.fyp.data

data class PostViewHistory(
    var viewID: String,
    val postID: String,
    val userID: String,
    var timestamp: String
) {
    constructor() : this("", "", "",  "")
}
