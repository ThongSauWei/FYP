package com.example.fyp

import android.content.Intent
import com.example.fyp.models.ListItem
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fyp.dao.AnnoucementDAO
import com.example.fyp.dao.PostImageDAO
import com.example.fyp.dataAdapter.AnnoucementAdapter
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Annoucement : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AnnoucementAdapter
    private val annoucementDAO = AnnoucementDAO()
    private lateinit var postImageDAO: PostImageDAO
    private lateinit var storageRef: StorageReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_annoucement, container, false)

        (activity as MainActivity).setToolbar(R.layout.toolbar_with_annouce_and_title)
        // Customize toolbar appearance
        val titleTextView = activity?.findViewById<TextView>(R.id.titleTextView)
        titleTextView?.text = "ANNOUNCEMENT"

        val navIcon = activity?.findViewById<ImageView>(R.id.navIcon)
        navIcon?.setImageResource(R.drawable.baseline_arrow_back_ios_24) // Set the navigation icon
        navIcon?.setOnClickListener { activity?.onBackPressed() } // Set click behavior

        val btnNotification = activity?.findViewById<ImageView>(R.id.btnNotification)
        btnNotification?.visibility = View.GONE

        val btnSearchToolbarWithAnnouce = activity?.findViewById<ImageView>(R.id.btnSearchToolbarWithAnnouce)
        btnSearchToolbarWithAnnouce?.visibility = View.GONE

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val databaseRef = FirebaseDatabase.getInstance().reference
        this.storageRef = FirebaseStorage.getInstance().reference

        postImageDAO = PostImageDAO(storageRef, databaseRef)
        // Fetch and display data
        loadAnnouncements()


        val cardViewRequest = view.findViewById<CardView>(R.id.cardViewProfile)
        val tvFriendRequest = view.findViewById<TextView>(R.id.textView9)
        val tvApproveRequest = view.findViewById<TextView>(R.id.textView11)

        // Define a common click listener
        val navigateToFriendRequest = View.OnClickListener {
            val fragment = FriendRequest()
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.fragmentContainerView, fragment)
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        // Set the listener to all the views
        cardViewRequest.setOnClickListener(navigateToFriendRequest)
        tvFriendRequest.setOnClickListener(navigateToFriendRequest)
        tvApproveRequest.setOnClickListener(navigateToFriendRequest)

        return view
    }

    private fun loadAnnouncements() {
        val currentUserID = getCurrentUserID()

        annoucementDAO.getUserAnnouncementsByUserID(currentUserID) { userAnnouncements ->
            val announcementIDs = userAnnouncements.map { it.announcementID }

            annoucementDAO.getAnnouncementsByIds(announcementIDs) { announcements ->
                // Filter out announcements with type "Friend Request"
                val filteredAnnouncements = announcements.filter { it.announcementType != "Friend Request" }

                // Group the remaining announcements by date
                val groupedItems = filteredAnnouncements.groupBy { announcement ->
                    getDatePart(announcement.announcementDate) // Group by date only
                }.flatMap { (date, announcementsForDate) ->
                    // Create a header for the date and add all announcements for that date
                    listOf(ListItem.Header(getFormattedDate(date))) + // Add header
                            announcementsForDate.map { ListItem.AnnouncementItem(it) } // Add items
                }

                // Pass the activity context and postImageDAO to the adapter
                adapter = AnnoucementAdapter(groupedItems, requireActivity() as AppCompatActivity, postImageDAO)
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
