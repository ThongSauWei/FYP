package com.example.fyp.dao

import com.example.fyp.data.PostShared
import com.google.firebase.database.*
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference


class PostSharedDAO {
    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("PostShared")

    fun addSharedPost(postID: String, userID: String, onComplete: (Boolean, Exception?) -> Unit) {
        SaveSharedPreference.getNextID(dbRef, "PS") { sharedID ->
            val postShared = PostShared(sharedID, postID, userID)
            dbRef.child(sharedID).setValue(postShared).addOnCompleteListener { task ->
                onComplete(task.isSuccessful, task.exception)
            }
        }
    }
}

