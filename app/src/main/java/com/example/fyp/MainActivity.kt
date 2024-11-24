package com.example.fyp

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var toolbarContainer: FrameLayout
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private lateinit var progressBar : ProgressBar
    private lateinit var navigationView : NavigationView
    private val manager = supportFragmentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

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
                R.id.nav_friend -> {
                    drawerLayout.closeDrawer(GravityCompat.START)

//                    val transaction = manager.beginTransaction()
//                    val fragment = Friends()
//                    transaction.replace(R.id.fragmentContainerView, fragment)
//                    transaction.addToBackStack(null)
//                    transaction.commit()

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

//                    val transaction = manager.beginTransaction()
//                    val fragment = Feedback()
//                    transaction.replace(R.id.fragmentContainerView, fragment)
//                    transaction.addToBackStack(null)
//                    transaction.commit()

                    true
                }
                R.id.nav_settings -> {
                    drawerLayout.closeDrawer(GravityCompat.START)

//                    val transaction = manager.beginTransaction()
//                    val fragment = Settings()
//                    transaction.replace(R.id.fragmentContainerView, fragment)
//                    transaction.addToBackStack(null)
//                    transaction.commit()

                    true
                }
                else -> {
                    false
                }
            }
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

        // Set navigation click listener
        toolbar.setNavigationOnClickListener {
            openDrawer()
        }
    }

    fun setToolbar() {
        toolbarContainer.removeAllViews()
        toolbarContainer.visibility = View.GONE
    }
}
