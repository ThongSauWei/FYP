package com.example.fyp.dao

import android.util.Log
import com.example.fyp.data.Announcement
import com.example.fyp.data.UserAnnouncement
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.*

class AnnoucementDAO {

    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("Announcement")
    private val userAnnRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("UserAnnouncement")

    // Method to add an announcement and link it with the user
    fun addAnnouncement(announcement: Announcement, userID: String, onComplete: (Boolean, Exception?) -> Unit) {
        // Fetch the last announcement ID from the database and increment it
        dbRef.orderByKey().limitToLast(1).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var newAnnouncementID = "A1000"  // Default starting ID

                if (snapshot.exists()) {
                    // Get the last key and generate the next ID
                    for (child in snapshot.children) {
                        val key = child.key // The ID in the form of A1000, A1001, etc.
                        if (key != null && key.startsWith("A")) {
                            val idNumber = key.substring(1).toIntOrNull()
                            if (idNumber != null) {
                                newAnnouncementID = "A" + (idNumber + 1).toString()  // Increment the ID
                            }
                        }
                    }
                }

                // Create the Announcement object with the generated ID
                val newAnnouncement = announcement.copy(announcementID = newAnnouncementID)

                // Start a batch write to save both Announcement and UserAnnouncement
                val announcementTask = dbRef.child(newAnnouncementID).setValue(newAnnouncement)

                // Generate a new userAnnouncementID
                val newUserAnnouncementID = "UA" + newAnnouncementID.substring(1)

                val userAnnouncement = UserAnnouncement(
                    userAnnID = newUserAnnouncementID,  // Unique userAnnouncementID (UA1000, UA1001, etc.)
                    userID = userID,
                    announcementID = newAnnouncementID
                )

                // Save the UserAnnouncement
                val userAnnouncementTask = userAnnRef.child(newUserAnnouncementID).setValue(userAnnouncement)

                // Wait for both tasks to complete
                announcementTask.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        userAnnouncementTask.addOnCompleteListener { userTask ->
                            if (userTask.isSuccessful) {
                                Log.d("AnnoucementDAO", "Announcement and UserAnnouncement saved successfully.")
                                onComplete(true, null)
                            } else {
                                Log.e("AnnoucementDAO", "Failed to save UserAnnouncement.", userTask.exception)
                                onComplete(false, userTask.exception)
                            }
                        }
                    } else {
                        Log.e("AnnoucementDAO", "Failed to save Announcement.", task.exception)
                        onComplete(false, task.exception)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AnnoucementDAO", "Error fetching last Announcement ID.", error.toException())
                onComplete(false, error.toException())
            }
        })
    }

    // Fetch announcements by user ID
    fun getUserAnnouncementsByUserID(userID: String, onComplete: (List<UserAnnouncement>) -> Unit) {
        userAnnRef.orderByChild("userID").equalTo(userID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userAnnouncements = mutableListOf<UserAnnouncement>()
                for (child in snapshot.children) {
                    val userAnnouncement = child.getValue(UserAnnouncement::class.java)
                    userAnnouncement?.let { userAnnouncements.add(it) }
                }
                Log.d("AnnoucementDAO", "User Announcements: $userAnnouncements")
                onComplete(userAnnouncements)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AnnoucementDAO", "Error fetching UserAnnouncements.", error.toException())
            }
        })
    }


    // Fetch announcements by their IDs
    // AnnoucementDAO.kt
    fun getAnnouncementsByIds(announcementIDs: List<String>, onComplete: (List<Announcement>) -> Unit) {
        val tasks = announcementIDs.map { id ->
            dbRef.child(id).get()
        }

        Tasks.whenAllComplete(tasks).addOnCompleteListener {
            val announcements = mutableListOf<Announcement>()
            for (task in tasks) {
                if (task.isSuccessful) {
                    task.result?.getValue(Announcement::class.java)?.let { announcements.add(it) }
                }
            }
            Log.d("AnnoucementDAO", "Fetched Announcements by IDs: $announcements")
            onComplete(announcements)
        }
    }

}
