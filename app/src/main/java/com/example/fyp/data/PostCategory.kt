package com.example.fyp.data

data class PostCategory(
    val postCategoryID: String,
    val postID: String,
    val userID: String
) {
    constructor() : this("", "", "")
}
