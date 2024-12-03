package com.example.fyp.data

data class Feedback(
    var feedbackID: String,
    val userID: String,
    var feedbackContent: String,
    var feedbackDateTime: String,
    var rate: Int
) {
    constructor() : this("", "", "", "", 0)
}
