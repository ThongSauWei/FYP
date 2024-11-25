package com.example.fyp.data

data class Feedback(
    var feedbackID: String,
    val userID: String,
    val feedbackContent: String,
    val feedbackDateTime: String,
    val rate: Int
) {
    constructor() : this("", "", "", "", 0)
}
