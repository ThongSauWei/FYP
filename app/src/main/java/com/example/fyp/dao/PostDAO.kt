package com.example.fyp.dao

import com.example.fyp.data.Post
import com.google.firebase.database.*
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference

class PostDAO {
    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("Post")

    fun addPost(post: Post, onComplete: (String?, Exception?) -> Unit) {
        SaveSharedPreference.getNextID(dbRef, "P") { newPostID ->
            post.postID = newPostID
            dbRef.child(post.postID).setValue(post).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(post.postID, null)
                } else {
                    onComplete(null, task.exception)
                }
            }
        }
    }

    fun deletePost(postID: String) {
        dbRef.child(postID).removeValue()
    }

}
