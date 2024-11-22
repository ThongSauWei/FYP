package com.example.fyp.data

data class Announcement(
    val announcementID: String,
    val announcementTitle: String,
    val announcementDesc: String,
    val announcementDate: String,
    val announcementStatus: Int
) {
    constructor() : this("", "", "", "", 0)
}
