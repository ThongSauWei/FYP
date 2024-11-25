package com.example.fyp.data

data class ChatLine(
    var chatLineID: String = "",        // Default empty string
    val chatID: String = "",            // Default empty string
    val content: String? = null,        // Default null for optional text
    val mediaType: String? = null,      // Default null for optional media type
    val mediaURL: String? = null,       // Default null for optional media URL
    val dateTime: String = "",          // Default empty string
    val senderID: String = "",          // Default empty string
    val receiverID: String = "",        // Default empty string

    val isSharedPost: Boolean = false,
    val postSharedID: String? = null    //ID of the shared post, if applicable

) {

}