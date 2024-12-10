package com.example.fyp.data
data class Save(
    var saveID: String,
    val userID: String,
    val postID: String,
    var status: Int,
    var timeStamp: String
) {
    constructor() : this("", "", "", 0, "")
}
