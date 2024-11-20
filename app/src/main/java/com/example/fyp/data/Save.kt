package com.mainapp.finalyearproject.data

data class Save(
    val saveID: String,
    val userID: String,
    val postID: String,
    val status: Int,
    val timeStamp: String
) {
    constructor() : this("", "", "", 0, "")
}
