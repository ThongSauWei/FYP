package com.mainapp.finalyearproject.data

data class PostShared(
    val postSharedID: String,
    val postID: String,
    val userID: String
) {
    constructor() : this("", "", "")
}
