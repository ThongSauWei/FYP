package com.example.fyp.data

data class Like(
    val likeID: String,
    val userID: String,
    val postID: String,
    val status: Int,
    val timeStamp: String
) {
    constructor() : this("", "", "", 0, "")
}
