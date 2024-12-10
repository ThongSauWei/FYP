package com.example.fyp.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

//data class PostViewHistory(
//    var viewID: String,
//    val postID: String,
//    val userID: String,
//    var timestamp: String
//) {
//    constructor() : this("", "", "",  "")
//}

data class PostViewHistory(
    var viewID: String,
    val postID: String,
    val userID: String,
    var timestamp: String // Timestamp should be in a format like "yyyy-MM-dd HH:mm:ss"
) {
    constructor() : this("", "", "", "")

    // Get the formatted date from the timestamp
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Use only the date part (year-month-day)
        val date = try {
            sdf.parse(timestamp)
        } catch (e: Exception) {
            null
        }
        return date?.let { sdf.format(it) } ?: ""
    }

//    fun getFormattedDate(): String {
//        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) // Format for "10 Dec 2024"
//        val date = try {
//            sdf.parse(timestamp)
//        } catch (e: Exception) {
//            null
//        }
//        return date?.let { sdf.format(it) } ?: ""
//    }


    // Check if the post is today
//    fun isToday(): Boolean {
//        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//        val today = sdf.format(Date())
//        return getFormattedDate() == today
//    }
//
//    // Check if the post is yesterday
//    fun isYesterday(): Boolean {
//        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//        val cal = Calendar.getInstance()
//        cal.add(Calendar.DATE, -1)
//        val yesterday = sdf.format(cal.time)
//        return getFormattedDate() == yesterday
//    }
}
