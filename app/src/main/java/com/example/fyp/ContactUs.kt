package com.example.fyp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class ContactUs : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        return inflater.inflate(R.layout.fragment_home, container, false)
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        (activity as MainActivity).setToolbar(R.layout.toolbar_with_annouce_and_title)

        return view
    }
}