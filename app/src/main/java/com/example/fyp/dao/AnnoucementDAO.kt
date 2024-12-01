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
    fun addAnnouncement(announcement: Announcement, userID: String, senderUserID: String, postID: String, onComplete: (Boolean, Exception?) -> Unit) {
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

                // Create the UserAnnouncement object with senderUserID and postID
                val userAnnouncement = UserAnnouncement(
                    userAnnID = newUserAnnouncementID,  // Unique userAnnouncementID (UA1000, UA1001, etc.)
                    userID = userID,
                    senderUserID = senderUserID,  // The user who performed the action
                    postID = postID,  // The ID of the post
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
                val totalChildren = snapshot.childrenCount.toInt()
                var processedChildren = 0

                if (totalChildren == 0) {
                    // No children, return immediately
                    onComplete(userAnnouncements)
                    return
                }

                for (child in snapshot.children) {
                    val userAnnouncement = child.getValue(UserAnnouncement::class.java)
                    if (userAnnouncement?.announcementID != null) {
                        dbRef.child(userAnnouncement.announcementID).get().addOnCompleteListener { task ->
                            processedChildren++
                            if (task.isSuccessful) {
                                val announcement = task.result.getValue(Announcement::class.java)
                                if (announcement?.announcementStatus == 1) {
                                    userAnnouncements.add(userAnnouncement)
                                }
                            }
                            // Check if all children are processed
                            if (processedChildren == totalChildren) {
                                onComplete(userAnnouncements)
                            }
                        }
                    } else {
                        // Increment processedChildren for invalid userAnnouncement
                        processedChildren++
                        if (processedChildren == totalChildren) {
                            onComplete(userAnnouncements)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AnnoucementDAO", "Error fetching UserAnnouncements.", error.toException())
                onComplete(emptyList()) // Return an empty list on error
            }
        })
    }


    // Fetch announcements by their IDs
    fun getAnnouncementsByIds(announcementIDs: List<String>, onComplete: (List<Announcement>) -> Unit) {
        val tasks = announcementIDs.map { id -> dbRef.child(id).get() }

        Tasks.whenAllComplete(tasks).addOnCompleteListener {
            val announcements = mutableListOf<Announcement>()
            for (task in tasks) {
                if (task.isSuccessful) {
                    val announcement = task.result?.getValue(Announcement::class.java)
                    // Only include announcements with status == 1
                    if (announcement?.announcementStatus == 1) {
                        announcements.add(announcement)
                    }
                }
            }
            Log.d("AnnoucementDAO", "Fetched Announcements by IDs: $announcements")
            onComplete(announcements)
        }
    }


    fun deleteAnnouncement(announcementID: String, onComplete: (Boolean, Exception?) -> Unit) {
        // Update announcementStatus to 0 in the Announcement table
        dbRef.child(announcementID).child("announcementStatus").setValue(0)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Optionally, you can also update related UserAnnouncement if needed
                    userAnnRef.orderByChild("announcementID").equalTo(announcementID)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val updateTasks = snapshot.children.map { child ->
                                    child.ref.child("status").setValue(0) // Assuming UserAnnouncement also has a status field
                                }

                                Tasks.whenAllComplete(updateTasks).addOnCompleteListener {
                                    onComplete(true, null)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("AnnoucementDAO", "Error updating UserAnnouncement status.", error.toException())
                                onComplete(false, error.toException())
                            }
                        })
                } else {
                    Log.e("AnnoucementDAO", "Error updating Announcement status.", task.exception)
                    onComplete(false, task.exception)
                }
            }
    }


}

