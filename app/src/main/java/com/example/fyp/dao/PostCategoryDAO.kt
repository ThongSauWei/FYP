package com.example.fyp.dao

import android.util.Log
import com.example.fyp.data.PostCategory
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.*
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class PostCategoryDAO {
    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("PostCategory")

    fun addCategories(postID: String, categories: List<String>, userID: String, onComplete: (Boolean, Exception?) -> Unit) {
        val tasks = mutableListOf<Task<Void>>() // List to store Firebase tasks

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var currentMaxID = 1000 // Default starting ID if no valid IDs exist

                if (snapshot.exists()) {
                    // Iterate through the existing keys and find the maximum valid ID
                    for (child in snapshot.children) {
                        val key = child.key
                        if (key != null && key.startsWith("PT")) {
                            try {
                                val idNumber = key.substring(2).toInt()
                                if (idNumber > currentMaxID) {
                                    currentMaxID = idNumber
                                }
                            } catch (e: NumberFormatException) {
                                Log.w("PostCategoryDAO", "Invalid key format: $key")
                            }
                        }
                    }
                }

                // Add categories with unique IDs
                for ((index, category) in categories.withIndex()) {
                    val categoryID = "PT${currentMaxID + index + 1}" // Increment for each category
                    val postCategory = PostCategory(categoryID, postID, category, userID)

                    val taskCompletionSource = TaskCompletionSource<Void>() // Track each task
                    dbRef.child(categoryID).setValue(postCategory)
                        .addOnSuccessListener {
                            Log.d("PostCategoryDAO", "Category added successfully: $category with ID: $categoryID")
                            taskCompletionSource.setResult(null)
                        }
                        .addOnFailureListener { exception ->
                            Log.e("PostCategoryDAO", "Failed to add category: $category", exception)
                            taskCompletionSource.setException(exception)
                        }

                    tasks.add(taskCompletionSource.task)
                }

                // Wait for all tasks to complete
                Tasks.whenAll(tasks).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("PostCategoryDAO", "All categories added successfully")
                        onComplete(true, null)
                    } else {
                        Log.e("PostCategoryDAO", "Failed to add some categories", task.exception)
                        onComplete(false, task.exception)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PostCategoryDAO", "Failed to fetch max ID", error.toException())
                onComplete(false, error.toException())
            }
        })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getCategoriesByPostID(postID: String): List<PostCategory> = suspendCancellableCoroutine { continuation ->
        dbRef.orderByChild("postID").equalTo(postID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val categories = mutableListOf<PostCategory>()
                for (categorySnapshot in snapshot.children) {
                    categorySnapshot.getValue(PostCategory::class.java)?.let { categories.add(it) }
                }
                continuation.resume(categories)
            }

            override fun onCancelled(error: DatabaseError) {
                continuation.resumeWithException(error.toException())
            }
        })
    }


}


