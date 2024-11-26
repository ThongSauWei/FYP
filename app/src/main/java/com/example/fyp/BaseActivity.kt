package com.example.fyp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference

class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme()
        super.onCreate(savedInstanceState)
    }

    private fun applyAppTheme() {
        val isChinese = SaveSharedPreference.getLanguage(this) == "zh"
        setTheme(R.style.AppTheme_Chinese)
    }
}
