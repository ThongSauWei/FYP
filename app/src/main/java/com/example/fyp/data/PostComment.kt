package com.example.fyp.data

data class PostComment(
    val postCommentID: String,
    val pcContent: String,
    val pcDateTime: String,
    val postID: String,
    val userID: String
) {
    constructor() : this("", "", "", "", "")
}
