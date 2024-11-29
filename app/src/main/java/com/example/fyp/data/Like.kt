package com.example.fyp.data

data class Like(
    var likeID: String,
    val userID: String,
    val postID: String,
    var status: Int,
    val timeStamp: String
) {
    constructor() : this("", "", "", 0, "")
}
