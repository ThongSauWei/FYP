package com.example.fyp

import android.graphics.pdf.models.ListItem
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fyp.dao.AnnoucementDAO
import com.example.fyp.dao.FriendDAO
import com.example.fyp.dao.PostImageDAO
import com.example.fyp.dataAdapter.AnnoucementAdapter
import com.example.fyp.dataAdapter.FriendRequestAdapter
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


class FriendRequest : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FriendRequestAdapter
    private lateinit var postImageDAO: PostImageDAO
//    private val userRequestDAO = UserRequestDAO() // Example DAO to fetch friend requests
    private val annoucementDAO = AnnoucementDAO()
    private lateinit var storageRef: StorageReference
    private val friendDAO = FriendDAO()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_friend_request, container, false)

        (activity as MainActivity).setToolbar(R.layout.toolbar_with_annouce_and_title)
        // Customize toolbar appearance
        val titleTextView = activity?.findViewById<TextView>(R.id.titleTextView)
        titleTextView?.text = "FRIEND REQUEST"

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

        loadFriendRequests()
        return view
    }

    private fun loadFriendRequests() {
        val currentUserID = getCurrentUserID()

        // Fetch pending friend requests directly from the Friend table
        GlobalScope.launch(Dispatchers.IO) {
            val pendingFriendRequests = friendDAO.getPendingFriendRequests(currentUserID) // Query pending requests
            withContext(Dispatchers.Main) {
                if (pendingFriendRequests.isNotEmpty()) {
                    // Group the requests by date and prepare the data for the adapter
                    val groupedItems = pendingFriendRequests.groupBy { friend ->
                        getDatePart(friend.timeStamp) // Group by date only
                    }.flatMap { (date, friendsForDate) ->
                        // Create a header for the date and add all friend requests for that date
                        listOf(com.example.fyp.models.ListItem.Header(getFormattedDate(date))) + // Add header
                                friendsForDate.map { com.example.fyp.models.ListItem.FriendItem(it) } // Change AnnouncementItem to FriendItem
                    }

                    // Pass the activity context and postImageDAO to the adapter
                    adapter = FriendRequestAdapter(groupedItems, requireActivity() as AppCompatActivity, postImageDAO)
                    recyclerView.adapter = adapter
                } else {
                    Log.d("FriendRequest", "No pending friend requests found.")
                }
            }
        }
    }

    private fun getCurrentUserID(): String {
        return SaveSharedPreference.getUserID(requireContext())
    }

    private fun getDatePart(dateTime: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val parsedDate = inputFormat.parse(dateTime)
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(parsedDate!!)
        } catch (e: Exception) {
            Log.e("FriendRequest", "Error parsing date: $dateTime", e)
            dateTime // Return original if parsing fails
        }
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
            Log.e("Friend Request", "Error parsing date: $dateTime", e)
            return dateTime // Return original if parsing fails
        }
    }
}
