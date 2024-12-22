package com.example.fyp.dao

import android.net.Uri
import android.util.Log
import com.example.fyp.data.PostImage
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
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
        // 查询与 postID 相关的所有图片
        dbRef.orderByChild("postID").equalTo(postID).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val deleteTasks = mutableListOf<Task<Void>>() // 跟踪所有删除任务

                for (child in snapshot.children) {
                    val imageID = child.key
                    val filePath = "PostImages/$imageID.jpg" // 存储路径

                    if (imageID != null) {
                        // 1. 删除 Realtime Database 中的记录
                        val deleteDatabaseTask = child.ref.removeValue()
                            .addOnSuccessListener {
                                Log.d(TAG, "Metadata deleted successfully for imageID: $imageID")
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to delete metadata for imageID: $imageID", e)
                            }

                        // 2. 删除 Firebase Storage 中的文件
                        val deleteStorageTask = storageRef.child(filePath).delete()
                            .addOnSuccessListener {
                                Log.d(TAG, "File deleted successfully from storage: $filePath")
                            }
                            .addOnFailureListener { e ->
                                if (e is StorageException && e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                                    Log.w(TAG, "File not found in storage (already deleted): $filePath")
                                } else {
                                    Log.e(TAG, "Failed to delete file from storage: $filePath", e)
                                }
                            }

                        // 将删除任务加入任务列表
                        deleteTasks.add(deleteDatabaseTask)
                        deleteTasks.add(deleteStorageTask)
                    } else {
                        Log.w(TAG, "Null imageID found for postID: $postID")
                    }
                }

                // 等待所有任务完成
                Tasks.whenAllComplete(deleteTasks).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Successfully deleted all images for postID: $postID")
                        onComplete(true, null)
                    } else {
                        val exception = task.exception ?: Exception("Some tasks failed during image deletion.")
                        Log.e(TAG, "Failed to delete some images for postID: $postID", exception)
                        onComplete(false, exception)
                    }
                }
            } else {
                Log.e(TAG, "No images found for postID: $postID")
                onComplete(false, Exception("No images found for postID: $postID"))
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Failed to query images for postID: $postID", exception)
            onComplete(false, exception)
        }
    }


    fun deleteImage(postImageID: String) {
        // Step 1: Remove metadata from Realtime Database
        dbRef.child(postImageID).removeValue()
            .addOnSuccessListener {
                Log.d("DeleteImage", "Metadata deleted successfully for imageID: $postImageID")

                // Step 2: Delete the image file from Firebase Storage
                val storagePath = "PostImages/$postImageID.jpg"
                storageRef.child(storagePath).delete()
                    .addOnSuccessListener {
                        Log.d("DeleteImage", "File deleted successfully from storage: $storagePath")
                    }
                    .addOnFailureListener { e ->
                        Log.e("DeleteImage", "Failed to delete file from storage: $storagePath", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("DeleteImage", "Failed to delete metadata for imageID: $postImageID", e)
            }
    }


    fun clearImagesForPost(postID: String) {
        dbRef.orderByChild("postID").equalTo(postID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val imageID = child.key
                    if (imageID != null) {
                        storageRef.child("postImage/$imageID.jpg").delete()
                    }
                    child.ref.removeValue()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to clear images for post $postID: ${error.message}")
            }
        })
    }

    fun deleteImageByID(imageID: String, onComplete: (Boolean, Exception?) -> Unit) {
        val storagePath = "PostImages/$imageID.jpg"
        val databasePath = "PostImage/$imageID"
        val storageReference = FirebaseStorage.getInstance().reference
        val databaseReference = FirebaseDatabase.getInstance().getReference()

        // Delete metadata from Realtime Database
        databaseReference.child(databasePath).removeValue().addOnCompleteListener { dbTask ->
            if (dbTask.isSuccessful) {
                Log.d("DeleteImage", "Metadata deleted successfully for: $imageID")

                // Delete the file from Firebase Storage
                storageReference.child(storagePath).delete().addOnCompleteListener { storageTask ->
                    if (storageTask.isSuccessful) {
                        Log.d("DeleteImage", "File deleted successfully from storage: $storagePath")
                        onComplete(true, null)
                    } else {
                        Log.e("DeleteImage", "Failed to delete file from storage: $storagePath", storageTask.exception)
                        onComplete(false, storageTask.exception)
                    }
                }
            } else {
                Log.e("DeleteImage", "Failed to delete metadata for: $imageID", dbTask.exception)
                onComplete(false, dbTask.exception)
            }
        }
    }


}
