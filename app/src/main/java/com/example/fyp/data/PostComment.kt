package com.example.fyp.data

data class PostComment(
    var postCommentID: String,
    val pcContent: String,
    val pcDateTime: String,
    val postID: String,
    val userID: String
) {
    constructor() : this("", "", "", "", "")
}
