package com.example.fyp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fyp.dao.AnnoucementDAO
import com.example.fyp.data.Announcement
import com.example.fyp.dataAdapter.AnnoucementAdapter
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference

class Annoucement : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AnnoucementAdapter
    private val announcementsList = mutableListOf<Announcement>()
    private val annoucementDAO = AnnoucementDAO() // Assuming you will implement DAO methods

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_annoucement, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

//        addAnnouncement()

        // Call the function to fetch and display data
        loadAnnouncements()

        return view
    }

//    private fun loadAnnouncements() {
//        val currentUserID = getCurrentUserID()
//
//        // Step 1: Get all user announcements for the current user
//        annoucementDAO.getUserAnnouncementsByUserID(currentUserID) { userAnnouncements ->
//            // Log the user announcements data to verify
//            Log.d("AnnoucementFragment", "User Announcements: $userAnnouncements")
//
//            // Step 2: For each user announcement, get the detailed announcement data
//            val announcementIDs = userAnnouncements.map { it.announcementID }
//
//            // Step 3: Fetch announcements based on the announcementIDs
//            annoucementDAO.getAnnouncementsByIds(announcementIDs) { announcements ->
//                // Log the announcements data to verify
//                Log.d("AnnoucementFragment", "Fetched Announcements: $announcements")
//
//                // Step 4: Set data to adapter and notify RecyclerView
//                announcementsList.clear()
//                announcementsList.addAll(announcements)
//                adapter = AnnoucementAdapter(announcementsList, currentUserID)
//                recyclerView.adapter = adapter
//            }
//        }
//    }
private fun loadAnnouncements() {
    val currentUserID = getCurrentUserID()

    annoucementDAO.getUserAnnouncementsByUserID(currentUserID) { userAnnouncements ->
        Log.d("AnnoucementFragment", "User Announcements: $userAnnouncements")

        val announcementIDs = userAnnouncements.map { it.announcementID }

        annoucementDAO.getAnnouncementsByIds(announcementIDs) { announcements ->
            Log.d("AnnoucementFragment", "Fetched Announcements: $announcements")

            announcementsList.clear()
            announcementsList.addAll(announcements)
            adapter = AnnoucementAdapter(announcementsList, currentUserID)
            recyclerView.adapter = adapter
        }
    }
}




//    private fun addAnnouncement() {
//        val currentUserID = getCurrentUserID()  // Get the current user's ID
//
//        // Create an announcement object
//        val newAnnouncement = Announcement(
//            announcementID = "",  // This will be filled in automatically
//            announcementType = "Friend Request",  // Type of announcement
//            announcementDate = "2024-12-01",  // Date of the announcement
//            announcementStatus = 1  // Status (1 for active, 0 for inactive)
//        )
//
//        // Add the new announcement and link it to the user
//        annoucementDAO.addAnnouncement(newAnnouncement, currentUserID) { success, exception ->
//            if (success) {
//                // The announcement was added successfully, so load the announcements
//                loadAnnouncements()
//            } else {
//                // Handle the failure case
//                Log.e("AnnoucementFragment", "Failed to add announcement", exception)
//            }
//        }
//    }


    private fun getCurrentUserID(): String {
        return SaveSharedPreference.getUserID(requireContext())  // Assuming SaveSharedPreference is your utility class
    }
}