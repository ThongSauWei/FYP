package com.mainapp.finalyearproject.data

data class UserAnnouncement(
    val userAnnID: String,
    val userID: String,
    val announcementID: String
) {
    constructor() : this("", "", "")
}
