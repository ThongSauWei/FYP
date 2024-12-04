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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fyp.dao.AnnoucementDAO
import com.example.fyp.dao.FriendDAO
import com.example.fyp.dao.PostImageDAO
import com.example.fyp.dataAdapter.AnnoucementAdapter
import com.example.fyp.dataAdapter.FriendRequestAdapter
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val friendDAO = FriendDAO()

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
        loadFriendRequests()

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

    private fun loadFriendRequests() {
        val cardAnnExits = view?.findViewById<CardView>(R.id.cardAnnExits)
        val cardViewProfile = view?.findViewById<MaterialCardView>(R.id.cardViewProfile) // Use MaterialCardView instead of CardView

        val currentUserID = getCurrentUserID()

        // Fetch pending friend requests directly from the Friend table
        GlobalScope.launch(Dispatchers.IO) {
            val pendingFriendRequests = friendDAO.getPendingFriendRequests(currentUserID)
            withContext(Dispatchers.Main) {
                if (pendingFriendRequests.isNotEmpty()) {
                    // Show the cardAnnExits and set the red border for cardViewProfile
                    cardAnnExits?.visibility = View.VISIBLE
                    cardViewProfile?.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.red)) // Set red border
                    Log.d("FriendRequest", "Pending requests found, card visible with red border")
                } else {
                    // Hide the cardAnnExits and reset the border color
                    cardAnnExits?.visibility = View.GONE
                    cardViewProfile?.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.black)) // Reset to original border color
                    Log.d("FriendRequest", "No pending requests, card hidden")
                }
            }
        }
    }




    private fun loadAnnouncements() {
        val currentUserID = getCurrentUserID()

        annoucementDAO.getUserAnnouncementsByUserID(currentUserID) { userAnnouncements ->
            val announcementIDs = userAnnouncements.map { it.announcementID }

            annoucementDAO.getAnnouncementsByIds(announcementIDs) { announcements ->
                // Filter out announcements with type "Friend Request"
                val filteredAnnouncements = announcements.filter { it.announcementType != "Friend Request" }

                // Sort announcements by date in descending order
                val sortedAnnouncements = filteredAnnouncements.sortedByDescending { announcement ->
                    try {
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(announcement.announcementDate)?.time
                    } catch (e: Exception) {
                        Log.e("Annoucement", "Error parsing date: ${announcement.announcementDate}", e)
                        0L // Default to earliest date if parsing fails
                    }
                }

                // Group announcements by date, with "Today" and "Yesterday" prioritized
                val groupedItems = sortedAnnouncements.groupBy { announcement ->
                    getFormattedDate(announcement.announcementDate) // Group by "Today", "Yesterday", or specific date
                }.flatMap { (formattedDate, announcementsForDate) ->
                    // Create a header for each date and add all announcements for that date
                    listOf(ListItem.Header(formattedDate)) + announcementsForDate.map { ListItem.AnnouncementItem(it) }
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
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val parsedDate = inputFormat.parse(dateTime)

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val yesterdayCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(yesterdayCalendar.time)

            val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(parsedDate!!)

            when (formattedDate) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(parsedDate)
            }
        } catch (e: Exception) {
            Log.e("Annoucement", "Error formatting date: $dateTime", e)
            dateTime // Return original if parsing fails
        }
    }

}
