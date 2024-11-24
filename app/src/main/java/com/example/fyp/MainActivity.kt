package com.example.fyp

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout

class MainActivity : AppCompatActivity() {

    private lateinit var toolbarContainer: FrameLayout
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle

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

        // Initialize the toolbar and drawer layout
        toolbarContainer = findViewById(R.id.toolbarContainer)
        drawerLayout = findViewById(R.id.main)

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
