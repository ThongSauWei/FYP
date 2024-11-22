package com.example.fyp.data

data class ChatLine(
    val chatLineID: String = "",        // Default empty string
    val chatID: String = "",            // Default empty string
    val content: String? = null,        // Default null for optional text
    val mediaType: String? = null,      // Default null for optional media type
    val mediaURL: String? = null,       // Default null for optional media URL
    val dateTime: String = "",          // Default empty string
    val senderID: String = "",          // Default empty string
    val receiverID: String = ""         // Default empty string
) {

}