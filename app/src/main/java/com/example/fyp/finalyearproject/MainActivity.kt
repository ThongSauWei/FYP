package com.mainapp.finalyearproject

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.View
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.navigation.NavigationView

class MainActivity() : AppCompatActivity(), Parcelable {

    private lateinit var progressBar: ProgressBar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    constructor(parcel: Parcel) : this() {

    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MainActivity> {
        override fun createFromParcel(parcel: Parcel): MainActivity {
            return MainActivity(parcel)
        }

        override fun newArray(size: Int): Array<MainActivity?> {
            return arrayOfNulls(size)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Handle window insets for edge-to-edge UI
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        progressBar = findViewById(R.id.progressBar)
        drawerLayout = findViewById(R.id.main)
        navigationView = findViewById(R.id.navigationView)

//        // Setup navigation drawer functionality
//        navigationView.setNavigationItemSelectedListener { menuItem ->
//            when (menuItem.itemId) {
//                R.id.nav_home -> {
//                    loadFragment(HomeFragment())
//                    true
//                }
//                R.id.nav_settings -> {
//                    loadFragment(SettingsFragment())
//                    true
//                }
//                R.id.nav_profile -> {
//                    loadFragment(ProfileFragment())
//                    true
//                }
//                else -> false
//            }
//        }
//
//        // Default fragment
//        loadFragment(HomeFragment())
//    }
//
//    // Function to load fragments dynamically
//    private fun loadFragment(fragment: Fragment) {
//        supportFragmentManager.beginTransaction()
//            .replace(R.id.fragmentContainerView, fragment)
//            .addToBackStack(null)
//            .commit()
//    }
//
//    // Function to show progress bar
//    fun showProgressBar() {
//        progressBar.visibility = View.VISIBLE
//    }
//
//    // Function to hide progress bar
//    fun hideProgressBar() {
//        progressBar.visibility = View.GONE
//    }
    }
}
