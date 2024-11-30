package com.example.fyp.data

data class ContactUs(
    var contactUsID: String,           // Unique ID for the Contact Us entry
    val contactUsName: String,         // Reporter Name
    val contactUsEmail: String,        // Reporter Email
    val contactUsProblemType: String,  // Problem Type
    val contactContent: String,        // Report Content
    val contactUsDateTime: String,     // Date and Time of Report
    val userID: String?                // User ID if logged in (nullable)
) {
    constructor() : this("", "", "", "", "", "", null)
}
