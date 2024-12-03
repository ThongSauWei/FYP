package com.example.fyp

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.google.firebase.database.FirebaseDatabase
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import java.util.*

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
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        (activity as MainActivity).setToolbar()

        spinnerLanguageSettings = view.findViewById(R.id.spinnerLanguageSettings)
        btnBackSettings = view.findViewById(R.id.btnBackSettings)
        btnSaveSettings = view.findViewById(R.id.btnSaveSettings)
        btnAboutUs = view.findViewById(R.id.btnAboutUsSettings)
        btnContactUs = view.findViewById(R.id.btnContactUsSettings)
        btnFeedback = view.findViewById(R.id.btnFeedbackSettings)

        val languages = resources.getStringArray(R.array.language_array)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguageSettings.adapter = adapter

        val savedLanguage = SaveSharedPreference.getLanguage(requireContext())
        spinnerLanguageSettings.setSelection(if (savedLanguage == "zh") 1 else 0)

        btnBackSettings.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }

        btnSaveSettings.setOnClickListener {
            val selectedLanguage = spinnerLanguageSettings.selectedItem.toString()
            val languageCode = if (selectedLanguage == "中文") "zh" else "en"

            SaveSharedPreference.setLanguage(requireContext(), languageCode)
            setAppLocale(languageCode)

            val userID = SaveSharedPreference.getUserID(requireContext())
            val userLanguageRef = FirebaseDatabase.getInstance().getReference("users/$userID/userChosenLanguage")
            userLanguageRef.setValue(languageCode)

            activity?.recreate()
        }

        btnAboutUs.setOnClickListener {
            navigateToFragment(AboutUs())
        }

        btnContactUs.setOnClickListener {
            navigateToFragment(ContactUs())
        }

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

    private fun setAppLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        requireContext().resources.updateConfiguration(config, requireContext().resources.displayMetrics)
    }
}
