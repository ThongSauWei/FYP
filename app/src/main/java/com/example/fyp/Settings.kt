package com.example.fyp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference

class Settings : Fragment() {

    private lateinit var spinnerLanguageSettings: Spinner
    private lateinit var btnBackSettings: ImageView
    private lateinit var btnSaveSettings: ImageView
    private lateinit var btnAboutUs: LinearLayout
    private lateinit var btnContactUs: LinearLayout
    private lateinit var btnFeedback: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Initialize views
        spinnerLanguageSettings = view.findViewById(R.id.spinnerLanguageSettings)
        btnBackSettings = view.findViewById(R.id.btnBackSettings)
        btnSaveSettings = view.findViewById(R.id.btnSaveSettings)
        btnAboutUs = view.findViewById(R.id.btnAboutUsSettings)
        btnContactUs = view.findViewById(R.id.btnContactUsSettings)
        btnFeedback = view.findViewById(R.id.btnFeedbackSettings)

        // Populate spinner with language options
        val languages = resources.getStringArray(R.array.language_array)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguageSettings.adapter = adapter

        // Load saved language and set the spinner selection
        val savedLanguage = SaveSharedPreference.getLanguage(requireContext())
        spinnerLanguageSettings.setSelection(if (savedLanguage == "zh") 1 else 0)

        // Back button functionality
        btnBackSettings.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }

        // Save selected language and restart activity
        btnSaveSettings.setOnClickListener {
            val selectedLanguage = spinnerLanguageSettings.selectedItem.toString()
            val languageCode = if (selectedLanguage == "中文") "zh" else "en"
            SaveSharedPreference.setLanguage(requireContext(), languageCode)

            // Restart the activity to apply the new language
            activity?.recreate()
        }

        // About Us button click
        btnAboutUs.setOnClickListener {
            navigateToFragment(AboutUs())
        }

        // Contact Us button click
        btnContactUs.setOnClickListener {
            navigateToFragment(ContactUs())
        }

        // Feedback button click
        btnFeedback.setOnClickListener {
            navigateToFragment(Feedback())
        }

        return view
    }

    private fun navigateToFragment(fragment: Fragment) {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transaction?.replace(R.id.fragmentContainerView, fragment)
        transaction?.addToBackStack(null)
        transaction?.commit()
    }
}
