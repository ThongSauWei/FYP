package com.example.fyp.dataAdapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fyp.R
import com.example.fyp.data.Announcement
import com.example.fyp.models.ListItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


class AnnoucementAdapter(private val items: List<ListItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_HEADER = 0
    private val TYPE_ANNOUNCEMENT = 1

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.Header -> TYPE_HEADER
            is ListItem.AnnouncementItem -> TYPE_ANNOUNCEMENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.annoucement_holder, parent, false)
            AnnouncementViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is ListItem.AnnouncementItem -> (holder as AnnouncementViewHolder).bind(item.announcement)
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDay: TextView = itemView.findViewById(R.id.tvDay)

        fun bind(header: ListItem.Header) {
            tvDay.text = header.date
        }
    }

    class AnnouncementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTypeTitle: TextView = itemView.findViewById(R.id.tvTypeTitle)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)

        fun bind(announcement: Announcement) {
            tvTypeTitle.text = when (announcement.announcementType) {
                "Like" -> "User liked your post"
                "Comment" -> "User commented on your post"
                "Shared" -> "User shared your post"
                "Friend Request" -> "User sent you a friend request"
                else -> "New notification"
            }
            tvTime.text = calculateRelativeTime(announcement.announcementDate)
        }

        private fun calculateRelativeTime(postDateTime: String): String {
            val inputDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) // Adjusted to parse date and time
            return try {
                val postDate = inputDateFormat.parse(postDateTime)
                val currentDate = Date()
                val diffInMillis = currentDate.time - postDate.time

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
                Log.e("AnnouncementAdapter", "Error parsing date: $postDateTime", e)
                "Unknown time"
            }
        }
    }

}

