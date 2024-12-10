package com.example.fyp.dao

import android.net.Uri
import android.util.Log
import com.example.fyp.data.PostImage
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.*
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PostImageDAO(
    private val storageRef: StorageReference,
    private val databaseRef: DatabaseReference
) {
    private val TAG = "PostImageDAO"
    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("PostImage")

    fun uploadImages(postID: String, uris: List<Uri>, userID: String, callback: (Boolean, Exception?) -> Unit) {
        if (uris.isEmpty()) {
            callback(true, null) // No images to upload, consider it successful
            return
        }

        val uploadTasks = mutableListOf<Task<Void>>()

        for (uri in uris) {
            val taskSource = TaskCompletionSource<Void>()
            uploadTasks.add(taskSource.task)

            // Generate a unique ID for each image
            generateUniqueImageID { imageID ->
                if (imageID == null) {
                    Log.e(TAG, "Failed to generate unique image ID")
                    taskSource.setException(Exception("Failed to generate image ID"))
                    callback(false, Exception("Failed to generate image ID"))
                    return@generateUniqueImageID
                }

                Log.d(TAG, "Generated Image ID: $imageID")
                val imageRef = storageRef.child("$imageID.jpg")

                // Upload the image
                imageRef.putFile(uri)
                    .continueWithTask { uploadTask ->
                        if (!uploadTask.isSuccessful) {
                            throw uploadTask.exception ?: Exception("Image upload failed")
                        }
                        imageRef.downloadUrl
                    }
                    .addOnSuccessListener { downloadUrl ->
                        Log.d(TAG, "Image uploaded successfully. URL: $downloadUrl")
                        val postImage = PostImage(imageID, postID, downloadUrl.toString(), userID)
                        databaseRef.child(imageID).setValue(postImage)
                            .addOnSuccessListener {
                                Log.d(TAG, "Image metadata saved successfully: $imageID")
                                taskSource.setResult(null)
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to save metadata to database", e)
                                taskSource.setException(e)
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to upload image", e)
                        taskSource.setException(e)
                    }
            }
        }

        // Wait for all upload tasks to complete
        Tasks.whenAllComplete(uploadTasks).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                callback(true, null)
            } else {
                val exception = task.exception ?: Exception("Unknown error occurred")
                callback(false, exception)
            }
        }
    }


    /**
     * Generates a unique image ID.
     */
    private fun generateUniqueImageID(callback: (String?) -> Unit) {
        val idRef = databaseRef.child("lastImageID")

        idRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentID = currentData.getValue(String::class.java)?.toIntOrNull() ?: 0
                currentData.value = (currentID + 1).toString()
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                snapshot: DataSnapshot?
            ) {
                if (committed) {
                    val newID = snapshot?.value?.toString()?.padStart(4, '0')
                    callback("PI$newID")
                } else {
                    Log.e(TAG, "Failed to generate unique ID", error?.toException())
                    callback(null)
                }
            }
        })
    }

    suspend fun getImagesByPostID(postID: String): List<PostImage> = suspendCancellableCoroutine { continuation ->
        dbRef.orderByChild("postID").equalTo(postID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val images = mutableListOf<PostImage>()
                for (imageSnapshot in snapshot.children) {
                    imageSnapshot.getValue(PostImage::class.java)?.let { images.add(it) }
                }
                continuation.resume(images)
            }

            override fun onCancelled(error: DatabaseError) {
                continuation.resumeWithException(error.toException())
            }
        })
    }

    fun deleteImagesByPostID(postID: String, onComplete: (Boolean, Exception?) -> Unit) {
        dbRef.orderByChild("postID").equalTo(postID).get().addOnSuccessListener { snapshot ->
            val tasks = snapshot.children.map { it.ref.removeValue() }
            Tasks.whenAllComplete(tasks).addOnCompleteListener { task ->
                if (task.isSuccessful) onComplete(true, null)
                else onComplete(false, task.exception)
            }
        }
    }

    fun deleteImage(postImageID: String) {
        dbRef.child(postImageID).removeValue()
    }

    fun clearImagesForPost(postID: String) {
        dbRef.orderByChild("postID").equalTo(postID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    child.ref.removeValue() // Removes the image metadata
                    val imageID = child.key
                    if (imageID != null) {
                        storageRef.child("$imageID.jpg").delete() // Deletes the image from storage
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to clear images for post $postID: ${error.message}")
            }
        })
    }

    fun deleteImageByID(imageID: String, onComplete: (Boolean, Exception?) -> Unit) {
        dbRef.child(imageID).removeValue().addOnSuccessListener {
            onComplete(true, null)
        }.addOnFailureListener { exception ->
            onComplete(false, exception)
        }
    }


}
