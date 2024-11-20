package com.mainapp.finalyearproject.data

data class PostCategory(
    val postCategoryID: String,
    val postID: String,
    val userID: String
) {
    constructor() : this("", "", "")
}
