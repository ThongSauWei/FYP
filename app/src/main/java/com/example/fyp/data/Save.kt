package com.example.fyp.data
data class Save(
    var saveID: String,
    val userID: String,
    val postID: String,
    var status: Int,
    val timeStamp: String
) {
    constructor() : this("", "", "", 0, "")
}
