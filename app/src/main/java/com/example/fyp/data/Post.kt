package com.example.fyp.data
data class Post(
    var postID: String,
    val postDateTime: String,
    val postDescription: String,
    val postTitle: String,
    val userID: String,
    val active: Int,
    val postType: String
) {
    constructor() : this("", "", "", "", "", 0, "")
}
