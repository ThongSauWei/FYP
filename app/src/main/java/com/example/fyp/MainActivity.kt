package com.example.fyp

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import com.example.fyp.viewModel.UserViewModel
import com.google.android.material.navigation.NavigationView
import com.jakewharton.threetenabp.AndroidThreeTen
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks

class MainActivity : AppCompatActivity() {

    private lateinit var toolbarContainer: FrameLayout
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private lateinit var progressBar: ProgressBar
    private lateinit var navigationView: NavigationView
    private val manager = supportFragmentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        AndroidThreeTen.init(this)

        // Check if the activity was started with a deep link
        val intent = intent
        val data = intent.data

        if (data != null && data.path == "/resetpassword") {
            val email = data.getQueryParameter("email")
            val token = data.getQueryParameter("token")

            if (!email.isNullOrEmpty() && !token.isNullOrEmpty()) {
                // Navigate to the NewPasswordEmail fragment
                val fragment = EmailNewPassword()
                val bundle = Bundle()
                bundle.putString("email", email)
                bundle.putString("token", token)
                fragment.arguments = bundle

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainerView, fragment)
                    .commit()
            }
        }


        // Apply system bar insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        progressBar = findViewById(R.id.progressBar)
        navigationView = findViewById(R.id.navigationView)

        // Initialize the toolbar and drawer layout
        toolbarContainer = findViewById(R.id.toolbarContainer)
        drawerLayout = findViewById(R.id.main)

        //link page
        navigationView.setNavigationItemSelectedListener {
                menuItem ->
            when(menuItem.itemId) {
                R.id.nav_home -> {
                    drawerLayout.closeDrawer(GravityCompat.START)

                    val transaction = manager.beginTransaction()
                    val fragment = Home()
                    transaction.replace(R.id.fragmentContainerView, fragment)
                    transaction.addToBackStack(null)
                    transaction.commit()

                    true
                }
                R.id.nav_profile -> {
                    drawerLayout.closeDrawer(GravityCompat.START)

                    val transaction = manager.beginTransaction()
                    val fragment = Profile()
                    transaction.replace(R.id.fragmentContainerView, fragment)
                    transaction.addToBackStack(null)
                    transaction.commit()

                    true
                }
                R.id.nav_search_friend -> {
                    drawerLayout.closeDrawer(GravityCompat.START)

                    val transaction = manager.beginTransaction()
                    val fragment = SearchFriend()
                    transaction.replace(R.id.fragmentContainerView, fragment)
                    transaction.addToBackStack(null)
                    transaction.commit()

                    true
                }
                R.id.nav_history -> {
                    drawerLayout.closeDrawer(GravityCompat.START)

                    val transaction = manager.beginTransaction()
                    val fragment = HistoryPost()
                    transaction.replace(R.id.fragmentContainerView, fragment)
                    transaction.addToBackStack(null)
                    transaction.commit()

                    true
                }
                R.id.nav_map -> {
                    drawerLayout.closeDrawer(GravityCompat.START)

                    val transaction = manager.beginTransaction()
                    val fragment = Map()
                    transaction.replace(R.id.fragmentContainerView, fragment)
                    transaction.addToBackStack(null)
                    transaction.commit()

                    true
                }
                R.id.nav_contact -> {
                    drawerLayout.closeDrawer(GravityCompat.START)

                    val transaction = manager.beginTransaction()
                    val fragment = ContactUs()
                    transaction.replace(R.id.fragmentContainerView, fragment)
                    transaction.addToBackStack(null)
                    transaction.commit()

                    true
                }
                R.id.nav_about -> {
                    drawerLayout.closeDrawer(GravityCompat.START)

                    val transaction = manager.beginTransaction()
                    val fragment = AboutUs()
                    transaction.replace(R.id.fragmentContainerView, fragment)
                    transaction.addToBackStack(null)
                    transaction.commit()

                    true
                }
                R.id.nav_feedback -> {
                    drawerLayout.closeDrawer(GravityCompat.START)

                    val transaction = manager.beginTransaction()
                    val fragment = Feedback()
                    transaction.replace(R.id.fragmentContainerView, fragment)
                    transaction.addToBackStack(null)
                    transaction.commit()

                    true
                }
                R.id.nav_settings -> {
                    drawerLayout.closeDrawer(GravityCompat.START)

                    val transaction = manager.beginTransaction()
                    val fragment = Settings()
                    transaction.replace(R.id.fragmentContainerView, fragment)
                    transaction.addToBackStack(null)
                    transaction.commit()

                    true
                }
                else -> {
                    setToolbar()
                    false
                }
            }
        }

        val footerMenu: View = findViewById(R.id.footerMenu)
        footerMenu.setOnClickListener {
            SaveSharedPreference.setUserID(this, "")
            refreshNavigationViewMenu()
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            }

            val transaction = manager.beginTransaction()
            val fragment = SignIn()
            transaction.replace(R.id.fragmentContainerView, fragment)
            manager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            transaction.commit()

            setDrawerEnabled(false) // Lock the drawer after logout
            Toast.makeText(this, "You have been logged out.", Toast.LENGTH_SHORT).show()
        }


        // Set up Drawer Toggle
        actionBarDrawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            R.string.menu_open,
            R.string.menu_close
        )
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()



        // Enable navigation icon
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
    }

//    for friend part
    fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    fun hideProgressBar() {
        progressBar.visibility = View.GONE
    }

    fun openDrawer() {
        drawerLayout.openDrawer(GravityCompat.START)
    }

    fun setToolbar(toolbarLayoutResId: Int, bgColorResId: Int = R.color.background) {
        toolbarContainer.visibility = View.VISIBLE

        toolbarContainer.removeAllViews()
        val toolbar: View = layoutInflater.inflate(toolbarLayoutResId, toolbarContainer, false)

        toolbarContainer.addView(toolbar)
        toolbarContainer.setBackgroundColor(this.getColor(bgColorResId))

        setSupportActionBar(toolbar as Toolbar)
        supportActionBar?.title = ""

        // Find the navIcon (ImageView) and set click listener
        val navIcon: ImageView = toolbar.findViewById(R.id.navIcon)
        navIcon.setOnClickListener {
            openDrawer()  // This will open the drawer
        }

        // Handle notification button click
        val btnNotification: ImageView = toolbar.findViewById(R.id.btnNotification)
        btnNotification.setOnClickListener {
            val transaction = supportFragmentManager.beginTransaction()
            val fragment = Annoucement() // Or start AnnouncementActivity if you're using an activity
            transaction.replace(R.id.fragmentContainerView, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        val btnChat: ImageView = toolbar.findViewById(R.id.btnChatToolbarWithAnnouce)
        btnChat.setOnClickListener {
            val transaction = supportFragmentManager.beginTransaction()
            val fragment = OuterChat()
            transaction.replace(R.id.fragmentContainerView, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }


    fun setToolbar() {
        toolbarContainer.removeAllViews()
        toolbarContainer.visibility = View.GONE
    }

    fun setDrawerEnabled(enabled: Boolean) {
        if (enabled) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        } else {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }
    }

    fun refreshNavigationViewMenu() {
        // Clear the current menu
        navigationView.menu.clear()

        // Re-inflate the menu to reflect updated language resources
        navigationView.inflateMenu(R.menu.menu) // Replace `drawer_menu` with your menu XML file name
    }

}
