package com.example.fyp.data

data class PostShared(
    val postSharedID: String,
    val postID: String,
    val userID: String
) {
    constructor() : this("", "", "")
}
