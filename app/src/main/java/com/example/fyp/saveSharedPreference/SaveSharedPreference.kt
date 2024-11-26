package com.mainapp.finalyearproject.saveSharedPreference

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

object SaveSharedPreference { //save and retrieve user-specific data, such as the user ID
    private const val PREF_USER_ID = "userID" //value to store or check user ID
    private const val PREFERENCES_FILE = "app_preferences"
    private const val LANGUAGE_KEY = "language"

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

    fun setLanguage(context: Context, language: String) {
        val editor = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE).edit()
        editor.putString(LANGUAGE_KEY, language)
        editor.apply()
    }

    fun getLanguage(context: Context): String {
        return context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE).getString(LANGUAGE_KEY, "en") ?: "en"
    }
}