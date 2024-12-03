package com.example.fyp

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.fyp.data.User
import com.example.fyp.viewModel.UserViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.regex.Pattern

class Register : Fragment() {

    private lateinit var txtNameRegister: EditText
    private lateinit var txtEmailRegister: EditText
    private lateinit var txtPhoneNoRegister: EditText
    private lateinit var txtBirthdayRegister: EditText
    private lateinit var txtPasswordRegister: EditText
    private lateinit var txtConfirmPasswordRegister: EditText
    private lateinit var txtSecurityQuestionRegister: EditText
    private lateinit var btnSignUpRegister: Button
    private lateinit var btnSignInRegister: TextView
    private lateinit var btnExitRegister: ImageView

    private lateinit var userViewModel: UserViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)
        (activity as MainActivity).setToolbar()

        // Initialize views
        txtNameRegister = view.findViewById(R.id.txtNameRegister)
        txtEmailRegister = view.findViewById(R.id.txtEmailRegister)
        txtPhoneNoRegister = view.findViewById(R.id.txtPhoneNoRegister)
        txtBirthdayRegister = view.findViewById(R.id.txtBirthdayRegister)
        txtPasswordRegister = view.findViewById(R.id.txtPasswordRegister)
        txtConfirmPasswordRegister = view.findViewById(R.id.txtConfirmPasswordRegister)
        txtSecurityQuestionRegister = view.findViewById(R.id.txtSecurityQuestionRegister)
        btnSignUpRegister = view.findViewById(R.id.btnSignUpRegister)
        btnSignInRegister = view.findViewById(R.id.btnSignInRegister)
        btnExitRegister = view.findViewById(R.id.btnExitRegister)

        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)

        btnSignInRegister.setOnClickListener { navigateToSignIn() }

        btnExitRegister.setOnClickListener { activity?.supportFragmentManager?.popBackStack() }

        btnSignUpRegister.setOnClickListener {
            if (validateInput()) {
                val name = txtNameRegister.text.toString()
                val email = txtEmailRegister.text.toString()
                val phoneNo = txtPhoneNoRegister.text.toString()
                val birthday = txtBirthdayRegister.text.toString()
                val password = txtPasswordRegister.text.toString()
                val securityQuestion = txtSecurityQuestionRegister.text.toString()

                lifecycleScope.launch {
                    val isRegistered = userViewModel.isEmailRegistered(email)
                    if (isRegistered) {
                        Toast.makeText(
                            requireContext(),
                            "This email is already registered",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val user = User(
                            userID = "",
                            username = name,
                            email = email,
                            userMobileNo = phoneNo,
                            userDOB = birthday,
                            password = password,
                            securityQuestion = securityQuestion,
                            token = "",
                            timeOfToken = 0L
                        )

                        userViewModel.addUser(user)

                        // After successful registration, navigate to SignIn and show a toast
                        navigateToSignIn()
                        Toast.makeText(
                            requireContext(),
                            "Registration successful! Please login.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        txtBirthdayRegister.apply {
            isFocusable = false
            isClickable = true
            setOnClickListener { showDatePicker() }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Adjust layout when the keyboard is shown
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset layout behavior when leaving the fragment
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    }

    private fun navigateToSignIn() {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        val fragment = SignIn()
        transaction?.replace(R.id.fragmentContainerView, fragment)
        transaction?.addToBackStack(null)
        transaction?.commit()
    }

    private fun validateInput(): Boolean {
        val name = txtNameRegister.text.toString()
        val email = txtEmailRegister.text.toString()
        val phoneNo = txtPhoneNoRegister.text.toString()
        val birthday = txtBirthdayRegister.text.toString()
        val password = txtPasswordRegister.text.toString()
        val confirmPassword = txtConfirmPasswordRegister.text.toString()
        val securityQuestion = txtSecurityQuestionRegister.text.toString()

        if (name.isEmpty() || email.isEmpty() || phoneNo.isEmpty() || birthday.isEmpty() || password.isEmpty() || securityQuestion.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return false
        }

        // Check if email ends with the TARUC domains
        if (!email.endsWith("@student.tarc.edu.my") && !email.endsWith("@tarc.edu.my")) {
            Toast.makeText(requireContext(), "Email must be a valid TARUC email address", Toast.LENGTH_SHORT).show()
            txtEmailRegister.error = "Email must use TARUC domains (@student.tarc.edu.my or @tarc.edu.my)"
            return false
        }

        if (!isValidPhoneNumber(phoneNo)) {
            Toast.makeText(requireContext(), "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
            txtPhoneNoRegister.error = "Please enter a valid phone number (e.g., 0112345678)"
            return false
        }

        if (!isValidPassword(password)) {
            Toast.makeText(requireContext(), "Password must meet the complexity requirements", Toast.LENGTH_SHORT).show()
            txtPasswordRegister.error = "Password must be at least 8 characters, include uppercase, lowercase, numbers, and special characters"
            return false
        }

        if (password != confirmPassword) {
            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
            txtConfirmPasswordRegister.error = "Passwords must match"
            return false
        }

        return true
    }

    private fun isValidPhoneNumber(phoneNo: String): Boolean {
        val malaysiaMobilePattern = Regex("(\\+?6?01)[0-9]-*([0-9]{7}|[0-9]{8})")
        return phoneNo.matches(malaysiaMobilePattern)
    }

    private fun isValidPassword(password: String): Boolean {
        val pattern = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=_!])(?=\\S+\$).{8,}\$")
        return pattern.matcher(password).matches()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(selectedYear, selectedMonth, selectedDay)

            // Check if selected date is after the current date
            if (selectedCalendar.after(calendar)) {
                Toast.makeText(requireContext(), "Birthday cannot be in the future", Toast.LENGTH_SHORT).show()
            } else {
                val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                txtBirthdayRegister.setText(selectedDate)
            }
        }, year, month, day)

        // Prevent user from selecting a future date
        datePickerDialog.datePicker.maxDate = calendar.timeInMillis
        datePickerDialog.show()
    }
}
