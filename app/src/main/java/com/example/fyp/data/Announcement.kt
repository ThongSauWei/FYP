package com.example.fyp.data

data class Announcement(
    var announcementID: String,
    val announcementType: String,
    val announcementDate: String,
    val announcementStatus: Int
) {
    constructor() : this("", "", "", 0)
}
