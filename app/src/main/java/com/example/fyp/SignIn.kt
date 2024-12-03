package com.example.fyp

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.FirebaseDatabase
import com.example.fyp.viewModel.UserViewModel
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import kotlinx.coroutines.launch
import java.util.*

class SignIn : Fragment() {
    private lateinit var txtEmailSignIn: EditText
    private lateinit var txtPasswordSignIn: EditText
    private lateinit var btnSignInSignIn: Button
    private lateinit var btnRegisterSignIn: TextView
    private lateinit var btnForgetSignIn: TextView

    private lateinit var userViewModel: UserViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sign_in, container, false)
        (activity as MainActivity).setToolbar()

        txtEmailSignIn = view.findViewById(R.id.txtEmailSignIn)
        txtPasswordSignIn = view.findViewById(R.id.txtPasswordSignIn)
        btnSignInSignIn = view.findViewById(R.id.signInButton)
        btnRegisterSignIn = view.findViewById(R.id.signUpLink)
        btnForgetSignIn = view.findViewById(R.id.forgotPasswordLink)

        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)

        btnRegisterSignIn.setOnClickListener {
            navigateToFragment(Register())
        }

        btnSignInSignIn.setOnClickListener {
            val email = txtEmailSignIn.text.toString()
            val password = txtPasswordSignIn.text.toString()

            if (validateInput(email, password)) {
                signInWithEmailAndPassword(email, password)
            }
        }

        btnForgetSignIn.setOnClickListener {
            navigateToFragment(PasswordRecovery())
        }

        return view
    }

    private fun signInWithEmailAndPassword(email: String, password: String) {
        lifecycleScope.launch {
            try {
                val user = userViewModel.getUserByLogin(email, password)
                if (user != null) {
                    SaveSharedPreference.setUserID(requireContext(), user.userID)

                    applyUserLanguage(user.userID) {
                        refreshFragment(Home()) // Refresh the Home fragment
                    }
                } else {
                    Toast.makeText(
                        requireContext(), "Authentication failed. Invalid Email/Password!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(), "Authentication failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            txtEmailSignIn.error = "Please enter a valid email address (abc@gmail.com)"
            return false
        }

        return true
    }

    private fun applyUserLanguage(userID: String, onLanguageApplied: () -> Unit) {
        val userLanguageRef = FirebaseDatabase.getInstance().getReference("users/$userID/userChosenLanguage")
        userLanguageRef.get().addOnSuccessListener { snapshot ->
            val languageCode = snapshot.getValue(String::class.java) ?: "en"

            SaveSharedPreference.setLanguage(requireContext(), languageCode)
            setAppLocale(languageCode)

            // Refresh the NavigationView menu
            (requireActivity() as MainActivity).refreshNavigationViewMenu()
            onLanguageApplied()
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to retrieve user language.", Toast.LENGTH_SHORT).show()
            onLanguageApplied()
        }
    }

    private fun setAppLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = android.content.res.Configuration()
        config.setLocale(locale)
        requireContext().resources.updateConfiguration(config, requireContext().resources.displayMetrics)
    }

    private fun refreshFragment(fragment: Fragment) {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transaction?.replace(R.id.fragmentContainerView, fragment)
        transaction?.detach(fragment)
        transaction?.attach(fragment)
        transaction?.commit()
    }

    private fun navigateToFragment(fragment: Fragment) {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transaction?.replace(R.id.fragmentContainerView, fragment)
        transaction?.addToBackStack(null)
        transaction?.commit()
    }
}
