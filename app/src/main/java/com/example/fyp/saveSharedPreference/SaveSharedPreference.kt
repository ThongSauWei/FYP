package com.mainapp.finalyearproject.saveSharedPreference

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.database.DatabaseReference

object SaveSharedPreference { //save and retrieve user-specific data, such as the user ID
    private const val PREF_USER_ID = "userID" //value to store or check user ID

    private fun getSharedPreference(context : Context) : SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)//get the default SharedPreferences
    }

    fun setUserID(context : Context, userID : String) { // store user id in preference
        val editor = getSharedPreference(context).edit()
        editor.putString(PREF_USER_ID, userID)//save
        editor.apply()//commits the changes
    }

    fun getUserID(context : Context) : String {
        return getSharedPreference(context).getString(PREF_USER_ID, "U1000")?: "U1000" // U1000 as default make sure the app have somethings to works
    }

    fun getNextID(dbRef: DatabaseReference, prefix: String, onComplete: (String) -> Unit) {
        dbRef.orderByKey().limitToLast(1).get().addOnSuccessListener { snapshot ->
            val lastID = snapshot.children.firstOrNull()?.key ?: "${prefix}1000"
            val number = lastID.drop(prefix.length).toInt() + 1
            val newID = "$prefix$number"
            onComplete(newID)
        }.addOnFailureListener {
            onComplete("${prefix}1000") // Default to initial ID on failure
        }
    }


}