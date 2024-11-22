package com.example.fyp.data

data class ContactUs(
    val contactUsID: String,
    val contactContent: String,
    val contactUsDateTime: String,
    val userID: String
) {
    constructor() : this("", "", "", "")
}
