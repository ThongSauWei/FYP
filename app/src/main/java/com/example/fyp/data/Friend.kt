package com.example.fyp.data

data class Friend(
    var friendID: String,
    val receiveUserID: String,
    val requestUserID: String,
    var status: String,
    val timeStamp: String
) {
    constructor() : this("", "", "", "", "")
}