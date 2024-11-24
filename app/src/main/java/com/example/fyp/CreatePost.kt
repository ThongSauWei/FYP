package com.example.fyp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class CreatePost : Fragment() {

//    private lateinit var txtTitleCreatePost: EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        return inflater.inflate(R.layout.fragment_home, container, false)
        val view = inflater.inflate(R.layout.fragment_create_post, container, false)

        (activity as MainActivity).setToolbar(R.layout.toolbar_with_annouce_and_title)

//        txtTitleCreatePost = view.findViewById(R.id.tvPostTitlePostHolder)

//        change toolbar thing to match
        // title
        val titleTextView = activity?.findViewById<TextView>(R.id.titleTextView)
        titleTextView?.text = "Create Post"

        // back icon
        val toolbar = activity?.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_with_annouce)
        toolbar?.setNavigationIcon(R.drawable.baseline_arrow_back_ios_24)

        // Set a click listener on the toolbar's navigation icon (optional)
        toolbar?.setNavigationOnClickListener {
            activity?.onBackPressed()
        }

        // send_post icon
        val btnSearchToolbarWithAnnouce = activity?.findViewById<ImageView>(R.id.btnSearchToolbarWithAnnouce)
        btnSearchToolbarWithAnnouce?.setImageResource(R.drawable.send_post)

        // Hide the notification button on CreatePost page
        val btnNotification = activity?.findViewById<ImageView>(R.id.btnNotification)
        btnNotification?.visibility = View.GONE


        return view
    }
}