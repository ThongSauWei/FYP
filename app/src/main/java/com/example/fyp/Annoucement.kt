package com.example.fyp

//import android.graphics.pdf.models.ListItem
import com.example.fyp.models.ListItem
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Header
import com.example.fyp.dao.AnnoucementDAO
import com.example.fyp.data.Announcement
import com.example.fyp.dataAdapter.AnnoucementAdapter
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Annoucement : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AnnoucementAdapter
    private val annoucementDAO = AnnoucementDAO()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_annoucement, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Fetch and display data
        loadAnnouncements()

        return view
    }

    private fun loadAnnouncements() {
        val currentUserID = getCurrentUserID()

        annoucementDAO.getUserAnnouncementsByUserID(currentUserID) { userAnnouncements ->
            val announcementIDs = userAnnouncements.map { it.announcementID }

            annoucementDAO.getAnnouncementsByIds(announcementIDs) { announcements ->
                // Group announcements by the date part only
                val groupedItems = announcements.groupBy { announcement ->
                    getDatePart(announcement.announcementDate) // Group by date only
                }.flatMap { (date, announcementsForDate) ->
                    // Create a header for the date and add all announcements for that date
                    listOf(ListItem.Header(getFormattedDate(date))) + // Add header
                            announcementsForDate.map { ListItem.AnnouncementItem(it) } // Add items
                }

                adapter = AnnoucementAdapter(groupedItems)
                recyclerView.adapter = adapter
            }
        }
    }

    private fun getDatePart(dateTime: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val parsedDate = inputFormat.parse(dateTime)
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(parsedDate!!)
        } catch (e: Exception) {
            Log.e("Annoucement", "Error parsing date: $dateTime", e)
            dateTime // Return original if parsing fails
        }
    }



    private fun getCurrentUserID(): String {
        return SaveSharedPreference.getUserID(requireContext())
    }

    private fun getFormattedDate(dateTime: String): String {
        try {
            // Try to parse the date and time (full format)
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            var parsedDate: Date? = null
            try {
                parsedDate = inputFormat.parse(dateTime)
            } catch (e: Exception) {
                // If parsing with time fails, try parsing just the date part
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                parsedDate = dateFormat.parse(dateTime)
            }

            // If parsedDate is null, return the original string as a fallback
            if (parsedDate == null) {
                return dateTime
            }

            // Get today's date in yyyy-MM-dd format
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // Get yesterday's date in yyyy-MM-dd format
            val yesterdayCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(yesterdayCalendar.time)

            // Format the parsed date as per the requirement
            val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(parsedDate)

            // Compare with today and yesterday
            return when (formattedDate) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(parsedDate)
            }
        } catch (e: Exception) {
            Log.e("Annoucement", "Error parsing date: $dateTime", e)
            return dateTime // Return original if parsing fails
        }
    }




}
