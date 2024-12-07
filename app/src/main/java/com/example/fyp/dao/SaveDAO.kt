package com.example.fyp.dao

import com.example.fyp.data.Save
import com.google.firebase.database.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SaveDAO {
    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("Save")

    suspend fun saveSave(save: Save) = suspendCancellableCoroutine<Unit> { continuation ->
        // Query to get the latest saveID used in the Save node
        dbRef.orderByKey().limitToLast(1).get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val dataSnapshot = task.result
                val lastKey = dataSnapshot?.children?.firstOrNull()?.key

                // If there is no lastKey, this is the first Save object
                val newSaveID = if (lastKey == null) {
                    "S1000" // First Save object
                } else {
                    // Increment the last saveID to generate a new one
                    val lastSaveID = lastKey.removePrefix("S").toInt()
                    "S${lastSaveID + 1}"
                }

                save.saveID = newSaveID

                // Save the Save object with the generated saveID
                dbRef.child(save.saveID).setValue(save).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(Unit)
                    } else {
                        continuation.resumeWithException(task.exception ?: Exception("Unknown error"))
                    }
                }
            } else {
                continuation.resumeWithException(task.exception ?: Exception("Error retrieving last saveID"))
            }
        }
    }

    // Fetch the save by userID and postID
    suspend fun getSaveByUserIDAndPostID(userID: String, postID: String): Save? = suspendCancellableCoroutine { continuation ->
        dbRef.orderByChild("userID").equalTo(userID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var save: Save? = null
                for (data in snapshot.children) {
                    val tempSave = data.getValue(Save::class.java)
                    if (tempSave?.postID == postID) {
                        save = tempSave
                        break
                    }
                }
                continuation.resume(save)
            }

            override fun onCancelled(error: DatabaseError) {
                continuation.resumeWithException(error.toException())
            }
        })
    }

    // Update save status
    suspend fun updateSaveStatus(save: Save) = suspendCancellableCoroutine<Unit> { continuation ->
        dbRef.child(save.saveID).setValue(save).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(Unit)
            } else {
                continuation.resumeWithException(task.exception ?: Exception("Error updating save status"))
            }
        }
    }

    suspend fun getSavesByUserID(userID: String): List<Save> = suspendCancellableCoroutine { continuation ->
        dbRef.orderByChild("userID").equalTo(userID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val saves = mutableListOf<Save>()
                for (data in snapshot.children) {
                    val save = data.getValue(Save::class.java)
                    if (save != null) {
                        saves.add(save)
                    }
                }
                continuation.resume(saves)
            }

            override fun onCancelled(error: DatabaseError) {
                continuation.resumeWithException(error.toException())
            }
        })
    }

}


