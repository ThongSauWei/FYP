package com.mainapp.finalyearproject.data

data class Friend(
    val friendID: String,
    val receiverUserID: String,
    val requestUserID: String,
    val status: String,
    val timeStamp: String
) {
    constructor() : this("", "", "", "", "")
}