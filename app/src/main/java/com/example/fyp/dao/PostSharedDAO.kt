package com.example.fyp.dao

import android.util.Log
import com.example.fyp.data.PostShared
import com.google.firebase.database.*
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference


class PostSharedDAO {
    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("PostShared")

    fun addSharedPost(postID: String, userID: String, onComplete: (Boolean, Exception?) -> Unit) {
        getNextID(dbRef, "PS") { sharedID ->
            Log.d("PostSharedDAO", "Generated postSharedID: $sharedID for userID: $userID")

            val postShared = PostShared(sharedID, postID, userID)
            dbRef.child(sharedID).setValue(postShared).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("PostSharedDAO", "PostShared entry created for userID: $userID with postSharedID: $sharedID")
                } else {
                    Log.e("PostSharedDAO", "Failed to create PostShared entry for userID: $userID: ${task.exception?.message}")
                }
                onComplete(task.isSuccessful, task.exception)
            }
        }
    }

    fun getNextID(dbRef: DatabaseReference, prefix: String, callback: (String) -> Unit) {
        val idRef = dbRef.child("lastID")
        val startOffset = 1000 // Starting point for IDs

        idRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val lastID = currentData.getValue(Int::class.java) ?: 0
                val newID = lastID + 1
                currentData.value = newID
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e("PostSharedDAO", "Transaction failed: ${error.message}")
                    callback("$prefix${System.currentTimeMillis()}") // Fallback to timestamp
                } else {
                    val newID = (currentData?.getValue(Int::class.java) ?: 0) + startOffset
                    callback("$prefix$newID")
                }
            }
        })
    }

    fun deleteSharesByPostID(postID: String) {
        dbRef.orderByChild("postID").equalTo(postID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (shareSnapshot in snapshot.children) {
                    shareSnapshot.ref.removeValue()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PostSharedDAO", "Failed to delete shares: ${error.message}")
            }
        })
    }

    fun clearSharedPost(postID: String, callback: (Boolean, Exception?) -> Unit) {
        dbRef.orderByChild("postID").equalTo(postID)
            .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    for (child in snapshot.children) {
                        child.ref.removeValue()
                            .addOnSuccessListener { callback(true, null) }
                            .addOnFailureListener { callback(false, it) }
                    }
                    callback(true, null) // All privacy settings cleared
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    callback(false, error.toException())
                }
            })
    }

}

