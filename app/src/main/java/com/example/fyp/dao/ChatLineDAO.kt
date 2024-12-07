package com.example.fyp.dao

import com.example.fyp.data.ChatLine
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

class ChatLineDAO {
    val dbRef : DatabaseReference = FirebaseDatabase.getInstance().getReference("ChatLine")
    private var chatLineList = mutableListOf<ChatLine>()

    private var nextID = 1000
    init {
        GlobalScope.launch {
            nextID = getNextID()
        }
    }

    fun addChatLine(chatLine : ChatLine) {
        chatLine.chatLineID = "CL$nextID"
        nextID++

        dbRef.child(chatLine.chatLineID).setValue(chatLine)
            .addOnCompleteListener {

            }
            .addOnFailureListener {

            }
    }

    suspend fun getChatLine(chatID : String) : List<ChatLine> = withContext(Dispatchers.IO) {

        val deferred = CompletableDeferred<List<ChatLine>>()

        dbRef.orderByChild("chatID").equalTo(chatID)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val chatList = ArrayList<ChatLine>()
                    if (snapshot.exists()) {
                        for (chatLineSnapshot in snapshot.children) {
                            val chatLine = chatLineSnapshot.getValue(ChatLine::class.java)
                            chatList.add(chatLine!!)
                        }
                    }

                    deferred.complete(chatList)
                }

                override fun onCancelled(error: DatabaseError) {
                    deferred.completeExceptionally(error.toException())
                }

            })

        val chatList = deferred.await()

        chatList
    }

    fun getChatLines(chatID: String, callback: (List<ChatLine>) -> Unit) {
        dbRef.orderByChild("chatID").equalTo(chatID)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val chatLines = mutableListOf<ChatLine>()
                    for (chatSnapshot in snapshot.children) {
                        val chatLine = chatSnapshot.getValue(ChatLine::class.java)
                        if (chatLine != null) {
                            chatLines.add(chatLine)
                        }
                    }
                    callback(chatLines)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database errors
                }
            })
    }


    suspend fun getLastChat(chatID : String) : ChatLine? {
        var chatLine : ChatLine? = null

        val snapshot = dbRef.orderByChild("chatID").equalTo(chatID).limitToLast(1).get().await()

        if (snapshot.exists()) {
            for (chatLineSnapshot in snapshot.children) {
                chatLine = chatLineSnapshot.getValue(ChatLine::class.java)
                break
            }
        }

        return chatLine
    }

    fun deleteChatLine(chatLineID : String) {
        dbRef.child(chatLineID).removeValue()
            .addOnCompleteListener {

            }
            .addOnFailureListener {

            }
    }

    private suspend fun getNextID() : Int {
        var chatLineID = 1000
        val snapshot = dbRef.orderByKey().limitToLast(1).get().await()

        if (snapshot.exists()) {
            for (chatLineSnapshot in snapshot.children) {
                val lastChatLineID = chatLineSnapshot.key!!
                chatLineID = lastChatLineID.substring(2).toInt() + 1
            }
        }

        return chatLineID
    }
}