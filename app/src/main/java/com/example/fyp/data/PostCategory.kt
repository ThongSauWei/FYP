package com.example.fyp.data

data class PostCategory(
    var postCategoryID: String,
    val postID: String,
    val userID: String
) {
    constructor() : this("", "", "")
}
