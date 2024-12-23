package com.example.fyp

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.fyp.data.ContactUs
import com.example.fyp.viewModel.ContactUsViewModel
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ContactUs : Fragment() {

    private lateinit var txtNameContactUs: EditText
    private lateinit var txtEmailContactUs: EditText
    private lateinit var ddlProblemTypeContactUs: Spinner
    private lateinit var txtDescriptionContactUs: EditText
    private lateinit var submitBtn: AppCompatButton
    private lateinit var backBtn: ImageView
    private lateinit var viewModel: ContactUsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_contact_us, container, false)
        (activity as MainActivity).setToolbar()

        // Initialize UI components
        txtNameContactUs = view.findViewById(R.id.txtNameContactUs)
        txtEmailContactUs = view.findViewById(R.id.txtEmailContactUs)
        ddlProblemTypeContactUs = view.findViewById(R.id.ddlProblemTypeContactUs)
        txtDescriptionContactUs = view.findViewById(R.id.txtDescriptionContactUs)
        submitBtn = view.findViewById(R.id.btnSubmitContactUs)
        backBtn = view.findViewById(R.id.btnCloseContactUs)

        // ViewModel initialization
        viewModel = ViewModelProvider(this).get(ContactUsViewModel::class.java)

        val scrollView = view.findViewById<ScrollView>(R.id.contactUsScrollView)

        // Set up the spinner
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.problem_type,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            ddlProblemTypeContactUs.adapter = adapter
        }

        // Handle back button click
        backBtn.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }

        // Handle form submission
        submitBtn.setOnClickListener {
            handleSubmit()
        }
        setupKeyboardListener(scrollView)

        return view
    }

    private fun setupKeyboardListener(scrollView: ScrollView) {
        val rootView = scrollView.rootView
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.height
            val keypadHeight = screenHeight - rect.bottom

            if (keypadHeight > screenHeight * 0.15) {
                // Keyboard is visible
                scrollView.setPadding(0, 0, 0, 900) // Add 1000px padding at the bottom
                scrollView.post {
                    scrollView.smoothScrollTo(0, scrollView.bottom)
                }
            } else {
                // Keyboard is hidden
                scrollView.setPadding(0, 0, 0, 0) // Remove the padding when the keyboard hides
            }
        }
    }

    private fun validateInput(): Boolean {
        val name = txtNameContactUs.text.toString().trim()
        val email = txtEmailContactUs.text.toString().trim()
        val description = txtDescriptionContactUs.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Name is required", Toast.LENGTH_SHORT).show()
            txtNameContactUs.error = "Name is required"
            return false
        }

        if (email.isEmpty()) {
            Toast.makeText(requireContext(), "Email is required", Toast.LENGTH_SHORT).show()
            txtEmailContactUs.error = "Email is required"
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            txtEmailContactUs.error = "Invalid email format"
            return false
        }

        if (description.isEmpty()) {
            Toast.makeText(requireContext(), "Description is required", Toast.LENGTH_SHORT).show()
            txtDescriptionContactUs.error = "Description is required"
            return false
        }

        return true
    }


    private fun handleSubmit() {

        if (!validateInput()) return
        val name = txtNameContactUs.text.toString().trim()
        val email = txtEmailContactUs.text.toString().trim()
        val problemType = ddlProblemTypeContactUs.selectedItem.toString()
        val description = txtDescriptionContactUs.text.toString().trim()
        val dateTime = getCurrentTimestamp()

        if (name.isEmpty() || email.isEmpty() || description.isEmpty()) {
            Toast.makeText(requireContext(), "All fields are required!", Toast.LENGTH_SHORT).show()
            return
        }

        val contactUs = ContactUs(
            contactUsID = "",
            contactUsName = name,
            contactUsEmail = email,
            contactUsProblemType = problemType,
            contactContent = description,
            contactUsDateTime = dateTime,
            userID = SaveSharedPreference.getUserID(requireContext())
        )

        lifecycleScope.launch {
            var firebaseSuccess = false
            var emailSuccess = false

            try {
                viewModel.addContactUs(contactUs)
                firebaseSuccess = true
            } catch (e: Exception) {
                Log.e("ContactUs", "Firebase Error: ${e.message}")
            }

            try {
                viewModel.sendEmailToSupport(
                    toEmail = "erikafung26@gmail.com",
                    subject = "New Contact Us Report: $problemType",
                    name = name,
                    email = email,
                    problemType = problemType,
                    description = description,
                    dateTime = dateTime
                )
                emailSuccess = true
            } catch (e: Exception) {
                Log.e("ContactUs", "SendGrid Error: ${e.message}")
            }

            if (firebaseSuccess && emailSuccess) {
                showSuccessDialog()
            } else {
                Toast.makeText(requireContext(), "Failed to submit. Try again later.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSuccessDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_success_mail_contact_us, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val closeBtn = dialogView.findViewById<ImageView>(R.id.imgCloseDeleteFriendDialog)
        val okayBtn = dialogView.findViewById<AppCompatButton>(R.id.btnYesSuccessContactUs)

        closeBtn.setOnClickListener {
            dialog.dismiss()
        }

        okayBtn.setOnClickListener {
            dialog.dismiss()
            activity?.supportFragmentManager?.popBackStack()
        }

        dialog.show()
    }

    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
