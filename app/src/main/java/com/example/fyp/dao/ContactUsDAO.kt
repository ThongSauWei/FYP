package com.example.fyp.dao

import com.example.fyp.data.ContactUs
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ContactUsDAO {
    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("ContactUs")
    private var nextID = 1 // Start from 1

    init {
        GlobalScope.launch {
            nextID = getNextID()
        }
    }

    // Add a ContactUs entry
    suspend fun addContactUs(contactUs: ContactUs) {
        if (contactUs.contactUsID.isEmpty()) {
            contactUs.contactUsID = "CU${nextID.toString().padStart(4, '0')}" // Generate ID as CU0001
            nextID++
        }
        dbRef.child(contactUs.contactUsID).setValue(contactUs).await()
    }

    // Helper method to get the next ID
    private suspend fun getNextID(): Int {
        var id = 1
        val snapshot = dbRef.orderByKey().limitToLast(1).get().await()

        if (snapshot.exists()) {
            for (contactSnapshot in snapshot.children) {
                val lastID = contactSnapshot.key!!
                if (lastID.startsWith("CU")) {
                    val numericPart = lastID.substring(2).toIntOrNull() ?: 0
                    id = numericPart + 1
                }
            }
        }

        return id
    }
}
