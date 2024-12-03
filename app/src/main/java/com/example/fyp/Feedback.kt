package com.example.fyp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.fyp.data.Feedback
import com.example.fyp.viewModel.FeedbackViewModel
import com.example.fyp.viewModel.ProfileViewModel
import com.google.firebase.storage.FirebaseStorage
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class Feedback : Fragment() {
    private lateinit var feedbackViewModel: FeedbackViewModel
    private lateinit var profileViewModel: ProfileViewModel

    private lateinit var imgProfile: ImageView
    private lateinit var btnClose: ImageView
    private lateinit var txtDescription: EditText
    private lateinit var btnSubmit: Button
    private lateinit var tvFeedbackStatus: TextView

    private lateinit var emojiVerySatisfied: ImageView
    private lateinit var emojiSatisfied: ImageView
    private lateinit var emojiNeutral: ImageView
    private lateinit var emojiUnsatisfied: ImageView
    private lateinit var emojiVeryUnsatisfied: ImageView

    private var selectedRate: Int = 0
    private var feedbackStatusText: String = ""
    private var existingFeedback: Feedback? = null // Store existing feedback if any

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_feedback, container, false)

        (activity as MainActivity).setToolbar()

        // Initialize ViewModels
        feedbackViewModel = ViewModelProvider(this).get(FeedbackViewModel::class.java)
        profileViewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)

        // Initialize Views
        imgProfile = view.findViewById(R.id.imgProfileFeedback)
        btnClose = view.findViewById(R.id.btnCloseFeedback)
        txtDescription = view.findViewById(R.id.txtDescriptionFeedback)
        btnSubmit = view.findViewById(R.id.btnSubmitFeedback)
        tvFeedbackStatus = view.findViewById(R.id.tvFeedbackStatus)

        emojiVerySatisfied = view.findViewById(R.id.emojiVerySatisfied)
        emojiSatisfied = view.findViewById(R.id.emojiSatisfied)
        emojiNeutral = view.findViewById(R.id.emojiNeutral)
        emojiUnsatisfied = view.findViewById(R.id.emojiUnsatisfied)
        emojiVeryUnsatisfied = view.findViewById(R.id.emojiVeryUnsatisfied)

        val currentUserID = SaveSharedPreference.getUserID(requireContext())

        // Load Profile Image
        loadUserProfileImage(currentUserID)

        // Check and Load Existing Feedback
        loadExistingFeedback(currentUserID)

        // Set Emoji Click Listeners
        setEmojiClickListeners()

        // Submit Feedback
        btnSubmit.setOnClickListener {
            if (existingFeedback == null) {
                submitFeedback(currentUserID) // Add new feedback
            } else {
                updateFeedback() // Update existing feedback
            }
        }

        // Navigate to Profile
        imgProfile.setOnClickListener {
            navigateToProfile(currentUserID)
        }

        // Close Button
        btnClose.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        return view
    }

    private fun loadUserProfileImage(userID: String) {
        val ref = FirebaseStorage.getInstance().reference.child("imageProfile/$userID.png")
        ref.downloadUrl.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val imageUrl = task.result.toString()
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.nullprofile) // Show placeholder while loading
                    .error(R.drawable.nullprofile) // Show default if loading fails
                    .into(imgProfile)
            } else {
                imgProfile.setImageResource(R.drawable.nullprofile) // Default image
            }
        }
    }


    private fun loadExistingFeedback(userID: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val feedback = feedbackViewModel.getFeedbackByUserID(userID)
                if (feedback != null) {
                    existingFeedback = feedback
                    txtDescription.setText(feedback.feedbackContent)
                    selectedRate = feedback.rate
                    feedbackStatusText = when (selectedRate) {
                        5 -> "Very Satisfied"
                        4 -> "Satisfied"
                        3 -> "Neutral"
                        2 -> "Unsatisfied"
                        1 -> "Very Unsatisfied"
                        else -> ""
                    }
                    tvFeedbackStatus.text = feedbackStatusText
                    updateEmojiUI(selectedRate)
                } else {
                    resetFeedbackForm()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load feedback: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun resetFeedbackForm() {
        resetEmojiColors()
        txtDescription.setText("")
        tvFeedbackStatus.text = ""
        selectedRate = 0
        feedbackStatusText = ""
        existingFeedback = null
    }


    private fun updateEmojiUI(rate: Int) {
        resetEmojiColors()
        when (rate) {
            5 -> emojiVerySatisfied.setImageResource(R.drawable.very_statisfied_yellow)
            4 -> emojiSatisfied.setImageResource(R.drawable.statisfied_yellow)
            3 -> emojiNeutral.setImageResource(R.drawable.neutral_yellow)
            2 -> emojiUnsatisfied.setImageResource(R.drawable.unsatisfied_yellow)
            1 -> emojiVeryUnsatisfied.setImageResource(R.drawable.very_unsatisfied_yellow)
        }
    }

    private fun resetEmojiColors() {
        emojiVerySatisfied.setImageResource(R.drawable.very_satisfied)
        emojiSatisfied.setImageResource(R.drawable.satisfied)
        emojiNeutral.setImageResource(R.drawable.neutral)
        emojiUnsatisfied.setImageResource(R.drawable.unsatisfied)
        emojiVeryUnsatisfied.setImageResource(R.drawable.very_unsatisfied)
    }

    private fun submitFeedback(userID: String) {
        val feedbackContent = txtDescription.text.toString().trim()

        if (selectedRate == 0) {
            Toast.makeText(requireContext(), "Please select a rating!", Toast.LENGTH_SHORT).show()
            return
        }

        if (feedbackContent.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter feedback description!", Toast.LENGTH_SHORT).show()
            return
        }

        val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val feedback = existingFeedback?.apply {
            this.feedbackContent = feedbackContent
            this.rate = selectedRate
            this.feedbackDateTime = dateTime
        } ?: Feedback(
            feedbackID = "",
            userID = userID,
            feedbackContent = feedbackContent,
            feedbackDateTime = dateTime,
            rate = selectedRate
        )

        if (existingFeedback == null) {
            feedbackViewModel.addFeedback(feedback)
            Toast.makeText(requireContext(), "Feedback submitted successfully!", Toast.LENGTH_SHORT).show()
        } else {
            feedbackViewModel.updateFeedback(feedback)
            Toast.makeText(requireContext(), "Feedback updated successfully!", Toast.LENGTH_SHORT).show()
        }

        existingFeedback = feedback
    }


    private fun updateFeedback() {
        val feedbackContent = txtDescription.text.toString().trim()

        if (selectedRate == 0) {
            Toast.makeText(requireContext(), "Please select a rating!", Toast.LENGTH_SHORT).show()
            return
        }

        if (feedbackContent.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter feedback description!", Toast.LENGTH_SHORT).show()
            return
        }

        existingFeedback?.let {
            it.feedbackContent = feedbackContent
            it.rate = selectedRate
            feedbackViewModel.updateFeedback(it)
            Toast.makeText(requireContext(), "Feedback updated successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToProfile(userID: String) {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        val fragment = Profile()
        transaction?.replace(R.id.fragmentContainerView, fragment)
        transaction?.addToBackStack(null)
        transaction?.commit()
    }

    private fun setEmojiClickListeners() {
        emojiVerySatisfied.setOnClickListener {
            resetEmojiColors()
            emojiVerySatisfied.setImageResource(R.drawable.very_statisfied_yellow)
            selectedRate = 5
            feedbackStatusText = "Very Satisfied"
            tvFeedbackStatus.text = feedbackStatusText
        }
        emojiSatisfied.setOnClickListener {
            resetEmojiColors()
            emojiSatisfied.setImageResource(R.drawable.statisfied_yellow)
            selectedRate = 4
            feedbackStatusText = "Satisfied"
            tvFeedbackStatus.text = feedbackStatusText
        }
        emojiNeutral.setOnClickListener {
            resetEmojiColors()
            emojiNeutral.setImageResource(R.drawable.neutral_yellow)
            selectedRate = 3
            feedbackStatusText = "Neutral"
            tvFeedbackStatus.text = feedbackStatusText
        }
        emojiUnsatisfied.setOnClickListener {
            resetEmojiColors()
            emojiUnsatisfied.setImageResource(R.drawable.unsatisfied_yellow)
            selectedRate = 2
            feedbackStatusText = "Unsatisfied"
            tvFeedbackStatus.text = feedbackStatusText
        }
        emojiVeryUnsatisfied.setOnClickListener {
            resetEmojiColors()
            emojiVeryUnsatisfied.setImageResource(R.drawable.very_unsatisfied_yellow)
            selectedRate = 1
            feedbackStatusText = "Very Unsatisfied"
            tvFeedbackStatus.text = feedbackStatusText
        }
    }
}
