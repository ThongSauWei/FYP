package com.example.fyp.dao

import android.util.Log
import com.example.fyp.data.Friend
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FriendDAO {
    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("Friend")

    private var nextID = 1000

    init {
        GlobalScope.launch {
            nextID = getNextID()
        }
    }


    suspend fun addFriend(friend: Friend) {
        Log.d("AddFriend", "Checking if the friend exists between ${friend.requestUserID} and ${friend.receiveUserID}")

        // If the friend relationship doesn't exist, we proceed to add
        if (getFriend(friend.requestUserID, friend.receiveUserID) == null) {
            friend.friendID = "F$nextID"
            nextID++

            Log.d("AddFriend", "Adding friend with ID: ${friend.friendID}")

            dbRef.child(friend.friendID).setValue(friend)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("AddFriend", "Friend added successfully")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("AddFriend", "Failed to add friend: ${exception.message}")
                }.await()
        } else {
            Log.d("AddFriend", "Friendship already exists.")
        }
    }


    suspend fun getFriendList(userID: String): List<Friend> = withContext(Dispatchers.IO) {

        val deferred = CompletableDeferred<List<Friend>>()

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val friendList = ArrayList<Friend>()
                if (snapshot.exists()) {
                    for (friendSnapshot in snapshot.children) {
                        val status = friendSnapshot.child("status").getValue(String::class.java)
                        if (status == "Friend") {
                            val friend = friendSnapshot.getValue(Friend::class.java)
                            friendList.add(friend!!)
                        }
                    }

                    val iterator = friendList.iterator()
                    while (iterator.hasNext()) {
                        val friend = iterator.next()
                        if (friend.requestUserID != userID && friend.receiveUserID != userID) {
                            iterator.remove()
                        }
                    }
                }
                deferred.complete(friendList)
            }

            override fun onCancelled(error: DatabaseError) {
                deferred.completeExceptionally(error.toException())
            }

        })

        val friendList = deferred.await()

        friendList
    }

    fun updateFriend(friend: Friend) {
        dbRef.child(friend.friendID).setValue(friend)
            .addOnCompleteListener {

            }
            .addOnFailureListener {

            }
    }

    fun deleteFriend(friendID: String) {
        dbRef.child(friendID).removeValue()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("DeleteFriend", "Successfully deleted friend with ID: $friendID")
                } else {
                    Log.e("DeleteFriend", "Failed to delete friend: ${task.exception?.message}")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("DeleteFriend", "Error deleting friend: ${exception.message}")
            }
    }


    suspend fun getFriend(userID_1: String, userID_2: String): Friend? {
        var friend: Friend? = null

        val snapshot = dbRef.get().await()

        if (snapshot.exists()) {
            for (friendSnapshot in snapshot.children) {
                val requestUserID =
                    friendSnapshot.child("requestUserID").getValue(String::class.java)
                val receiveUserID =
                    friendSnapshot.child("receiveUserID").getValue(String::class.java)
                if ((requestUserID == userID_1 && receiveUserID == userID_2) ||
                    (requestUserID == userID_2 && receiveUserID == userID_1)
                ) {
                    friend = friendSnapshot.getValue(Friend::class.java)
                    break
                }
            }
        }

        return friend
    }

    private suspend fun getNextID(): Int {
        var friendID = 1000
        val snapshot = dbRef.orderByKey().limitToLast(1).get().await()

        if (snapshot.exists()) {
            for (friendSnapshot in snapshot.children) {
                val lastFriendID = friendSnapshot.key!!
                friendID = lastFriendID.substring(1).toInt() + 1
            }
        }

        return friendID
    }

    suspend fun getPendingFriendRequests(receiveUserID: String): List<Friend> = withContext(Dispatchers.IO) {
        val pendingRequests = mutableListOf<Friend>()
        val snapshot = dbRef.orderByChild("receiveUserID").equalTo(receiveUserID).get().await()
        if (snapshot.exists()) {
            for (friendSnapshot in snapshot.children) {
                val friend = friendSnapshot.getValue(Friend::class.java)
                if (friend != null && friend.status == "Pending") {
                    pendingRequests.add(friend)
                }
            }
        }
        return@withContext pendingRequests
    }

    fun observeFriendStatus(userID1: String, userID2: String, callback: (Friend?) -> Unit) {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var friend: Friend? = null
                for (friendSnapshot in snapshot.children) {
                    val requestUserID = friendSnapshot.child("requestUserID").getValue(String::class.java)
                    val receiveUserID = friendSnapshot.child("receiveUserID").getValue(String::class.java)

                    if ((requestUserID == userID1 && receiveUserID == userID2) ||
                        (requestUserID == userID2 && receiveUserID == userID1)
                    ) {
                        friend = friendSnapshot.getValue(Friend::class.java)
                        break
                    }
                }
                callback(friend)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    suspend fun getFriendByID(friendID: String): Friend? {
        val snapshot = dbRef.child(friendID).get().await()
        return if (snapshot.exists()) snapshot.getValue(Friend::class.java) else null
    }


}