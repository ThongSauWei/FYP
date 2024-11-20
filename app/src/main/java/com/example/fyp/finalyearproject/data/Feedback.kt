package com.mainapp.finalyearproject.data

data class Feedback(
    val feedbackID: String,
    val userID: String,
    val feedbackContent: String,
    val feedbackDateTime: String,
    val rate: Int
) {
    constructor() : this("", "", "", "", 0)
}
