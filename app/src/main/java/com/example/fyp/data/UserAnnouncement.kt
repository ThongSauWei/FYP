package com.example.fyp.data

data class UserAnnouncement(
    val userAnnID: String,
    val userID: String,
    val postID: String,
    val senderUserID: String,
    val announcementID: String
) {
    constructor() : this("", "","","", "")
}
