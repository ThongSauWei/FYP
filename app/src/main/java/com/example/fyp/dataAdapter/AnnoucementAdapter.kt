package com.example.fyp.dataAdapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fyp.R
import com.example.fyp.data.Announcement
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


class AnnoucementAdapter(private val announcements: List<Announcement>, private val userID: String) :
    RecyclerView.Adapter<AnnoucementAdapter.AnnouncementViewHolder>() {

    // Group announcements by date
//    private val groupedAnnouncements: Map<String, List<Announcement>> = announcements.groupBy { it.announcementDate }

    class AnnouncementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        val tvTypeTitle: TextView = itemView.findViewById(R.id.tvTypeTitle)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnouncementViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.annoucement_holder, parent, false)
        return AnnouncementViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AnnouncementViewHolder, position: Int) {
//        val (date, groupAnnouncements) = groupedAnnouncements.toList()[position]
        val announcement = announcements[position]
//        val firstAnnouncement = groupAnnouncements.first()


//        // Set the tvDay (group title)
//        holder.tvDay.text = getFormattedDate(date)
//
//        // Set tvTypeTitle (announcement message)
//        holder.tvTypeTitle.text = getAnnouncementTypeMessage(firstAnnouncement)
//
//        // Set tvTime (relative time)
//        holder.tvTime.text = calculateRelativeTime(firstAnnouncement.announcementDate)
        // Set the tvDay (formatted date)
        holder.tvDay.text = getFormattedDate(announcement.announcementDate)

        // Set tvTypeTitle (announcement message)
        holder.tvTypeTitle.text = getAnnouncementTypeMessage(announcement)

        // Set tvTime (relative time)
        holder.tvTime.text = calculateRelativeTime(announcement.announcementDate)
    }

//    override fun getItemCount(): Int {
//        // Log the number of groups to verify
//        Log.d("AnnoucementAdapter", "Grouped Items Count: ${groupedAnnouncements.size}")
//        return groupedAnnouncements.size
//    }
override fun getItemCount(): Int {
    Log.d("AnnoucementAdapter", "Total Announcements: ${announcements.size}")
    return announcements.size
}


    // Get the message for the announcement based on the type
    private fun getAnnouncementTypeMessage(announcement: Announcement): String {
        return when (announcement.announcementType) {
            "Like" -> "${getUserName()} liked your post"
            "Comment" -> "${getUserName()} commented on your post"
            "Shared" -> "${getUserName()} shared your post"
            "Friend Request" -> "${getUserName()} sent you a friend request"
            else -> "New notification"
        }
    }

    // Get the formatted date string (Today, Yesterday, or the actual date)
    private fun getFormattedDate(date: String): String {
        val currentDate = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val announcementDate = dateFormat.parse(date) ?: return date

        val calendar = Calendar.getInstance()
        calendar.time = currentDate
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentDate)

        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(yesterday.time)

        val formattedDate = when {
            date == today -> "Today"
            date == yesterdayStr -> "Yesterday"
            else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(announcementDate)
        }

        Log.d("AnnoucementAdapter", "Formatted Date: $formattedDate")
        return formattedDate
    }


    // Helper method to get the current user's name (assuming it's stored in preferences)
    private fun getUserName(): String {
        // This can be customized to fetch the current user's name
        return "User"
    }

    // Helper method to calculate relative time like "5 minutes ago", "1 hour ago", etc.
    private fun calculateRelativeTime(postDateTime: String): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try {
            // Parse postDateTime into Date object
            val postDate = dateFormat.parse(postDateTime)
            val currentDate = Date()

            // Calculate the time difference in milliseconds
            val diffInMillis = currentDate.time - postDate.time

            // Convert to meaningful time units
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
            val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
            val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)

            when {
                minutes < 60 -> "$minutes mins ago"
                hours < 24 -> "$hours hours ago"
                days < 30 -> "$days days ago"
                days < 365 -> "${days / 30} months ago"
                else -> "${days / 365} years ago"
            }
        } catch (e: Exception) {
            // Fallback in case of parsing error
            "Unknown time"
        }
    }

}
