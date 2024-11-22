package com.example.fyp.data

data class PostImage(
    val postImageID: String,
    val postID: String,
    val userID: String
) {
    constructor() : this("", "", "")
}
