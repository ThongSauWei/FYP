package com.example.fyp.models

import com.example.fyp.data.Announcement
import com.example.fyp.data.Friend

sealed class ListItem {
    data class Header(val date: String) : ListItem()
    data class AnnouncementItem(val announcement: Announcement) : ListItem()

    data class FriendItem(val friend: Friend) : ListItem()

}