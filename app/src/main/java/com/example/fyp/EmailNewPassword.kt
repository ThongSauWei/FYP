package com.example.fyp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fyp.viewModel.UserViewModel
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class EmailNewPassword : Fragment()  {

    private lateinit var newPassword: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var backBtn: ImageView
    private lateinit var submitBtn: AppCompatButton
    private lateinit var userViewModel: UserViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.newpassword_email, container, false)

        newPassword = view.findViewById(R.id.txtEmailNewPassword)
        confirmPassword = view.findViewById(R.id.txtEmailConfirmPasswordNewPassword)
        backBtn = view.findViewById(R.id.btnExitNewPassword)
        submitBtn = view.findViewById(R.id.btnEmailSubmitNewPassword)

        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)

        backBtn.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }

        val email = arguments?.getString("email")
        val token = arguments?.getString("token")

        if (!email.isNullOrEmpty() && !token.isNullOrEmpty()) {
            userViewModel.validateToken(email, token) { isValid ->
                if (isValid) {
                    setupPasswordReset(email)
                } else {
                    Toast.makeText(requireContext(), "Invalid or expired token.", Toast.LENGTH_SHORT).show()
                    activity?.supportFragmentManager?.popBackStack()
                    goBackSignIn()
                }
            }
        } else {
            Toast.makeText(requireContext(), "Invalid request.", Toast.LENGTH_SHORT).show()
            activity?.supportFragmentManager?.popBackStack()
        }

        return view
    }

    private fun setupPasswordReset(email: String) {
        submitBtn.setOnClickListener {
            val newPass = newPassword.text.toString().trim()
            val confirmPass = confirmPassword.text.toString().trim()

            if (newPass.isNotEmpty() && confirmPass.isNotEmpty()) {
                if (newPass == confirmPass) {
                    if (isValidPassword(newPass)) {
                        userViewModel.viewModelScope.launch {
                            val user = userViewModel.getUserByEmail(email)
                            if (user != null) {
                                user.password = userViewModel.hashPassword(newPass)
                                userViewModel.updateUser(user)
                                userViewModel.deleteToken(email) {}
                                Toast.makeText(requireContext(), "Password updated successfully!", Toast.LENGTH_SHORT).show()
                                activity?.supportFragmentManager?.popBackStack()
                            } else {
                                Toast.makeText(requireContext(), "Failed to update password!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        newPassword.error = "Password must be at least 8 characters long, include a special character, uppercase, and lowercase letters."
                    }
                } else {
                    confirmPassword.error = "Passwords do not match!"
                }
            } else {
                newPassword.error = "Fields cannot be empty!"
                confirmPassword.error = "Fields cannot be empty!"
            }
        }
    }

    private fun goBackSignIn(){
        val transaction = activity?.supportFragmentManager?.beginTransaction()//after success go to home
        val fragment = SignIn()
        transaction?.replace(R.id.fragmentContainerView, fragment)
        transaction?.addToBackStack(null)
        transaction?.commit()
    }

    private fun isValidPassword(password: String): Boolean {
        val pattern = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=_!])(?=\\S+$).{8,}$")
        return pattern.matcher(password).matches()
    }
}