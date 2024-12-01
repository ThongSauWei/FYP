package com.example.fyp.models

import com.example.fyp.data.Announcement

sealed class ListItem {
    data class Header(val date: String) : ListItem()
    data class AnnouncementItem(val announcement: Announcement) : ListItem()
}