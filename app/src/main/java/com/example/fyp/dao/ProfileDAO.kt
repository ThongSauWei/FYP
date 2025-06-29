package com.example.fyp.dao

import com.example.fyp.data.Profile
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ProfileDAO {
    private val dbRef : DatabaseReference = FirebaseDatabase.getInstance().getReference("Profile")
    private var userIDList = ArrayList<String>()

    fun addProfile(profile : Profile) {
        dbRef.child(profile.userID).setValue(profile)
            .addOnCompleteListener {

            }
            .addOnFailureListener {

            }
    }

    fun updateProfile(profile: Profile) {
        dbRef.child(profile.userID).setValue(profile)
            .addOnCompleteListener {
                // Handle completion if needed
            }
            .addOnFailureListener {
                // Handle failure if needed
            }
    }



    suspend fun getProfile(userID : String) : Profile? = suspendCoroutine { continuation ->

        dbRef.orderByChild("userID").equalTo(userID)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (profileSnapshot in snapshot.children) {
                            val profile = profileSnapshot.getValue(Profile::class.java)
                            continuation.resume(profile)
                            return
                        }
                    }
                    continuation.resume(null)
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(error.toException())
                }

            })
    }

    suspend fun getUserListByCourse(course : String) : List<String> = withContext(Dispatchers.IO) {
        return@withContext suspendCoroutine { continuation ->

            dbRef.orderByChild("userCourse").equalTo(course).limitToFirst(10)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        userIDList.clear()
                        if (snapshot.exists()) {
                            for (profileSnapshot in snapshot.children) {
                                val userID = profileSnapshot.child("userID").getValue(String::class.java)
                                userIDList.add(userID!!)
                            }
                        }

                        continuation.resume(userIDList)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resumeWithException(error.toException())
                    }

                })
        }
    }

    suspend fun getRemainingUsers(list : List<String>) : List<String> = withContext(Dispatchers.IO) {
        return@withContext suspendCoroutine { continuation ->
            dbRef.orderByKey()
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val arrayList = ArrayList(list)
                        if (snapshot.exists()) {
                            for (profileSnapshot in snapshot.children) {
                                val userID = profileSnapshot.child("userID").getValue(String::class.java)
                                if (!arrayList.contains(userID)) {
                                    arrayList.add(userID)

                                    if (arrayList.size >= 10) {
                                        break
                                    }
                                }
                            }
                        }

                        continuation.resume(arrayList)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resumeWithException(error.toException())
                    }

                })
        }
    }

    fun updateBackgroundImage(userID: String, newImageUrl: String) {
        dbRef.child(userID).child("userBackgroundImage").setValue(newImageUrl)
            .addOnSuccessListener {

            }
            .addOnFailureListener {

            }
    }
    fun updateProfilePicture(userID: String, newImageUrl: String) {
        dbRef.child(userID).child("userImage").setValue(newImageUrl)
            .addOnSuccessListener {

            }
            .addOnFailureListener {

            }
    }

    fun deleteProfile(userID : String) {
        dbRef.child(userID).removeValue()
            .addOnCompleteListener {

            }
            .addOnFailureListener {

            }
    }

    suspend fun getAllProfiles(): List<Profile> = withContext(Dispatchers.IO) {
        return@withContext suspendCancellableCoroutine { continuation ->
            val profileList = mutableListOf<Profile>()

            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (profileSnapshot in snapshot.children) {
                            val profile = profileSnapshot.getValue(Profile::class.java)
                            if (profile != null) {
                                profileList.add(profile)
                            }
                        }
                    }
                    continuation.resume(profileList)
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(error.toException())
                }
            })
        }
    }
}