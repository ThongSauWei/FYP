package com.example.fyp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fyp.viewModel.UserViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject

class RecoveryByEmail : Fragment() {
    private lateinit var inputEmail: EditText
    private lateinit var btnSubmit: AppCompatButton
    private lateinit var backBtn: ImageView
    private lateinit var userViewModel: UserViewModel

    private val sendGridApiKey = "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" // Replace with your SendGrid API Key
    private val appBaseUrl = "https://tarumt.page.link" // Replace with your dynamic link base URL
    private val TOKEN_EXPIRATION_TIME = 30 * 60 * 1000 // 30 minutes in milliseconds

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.email_recovery, container, false)
        initializeUIComponents(view)
        setupListeners()
        return view
    }

    // Initialize UI components
    private fun initializeUIComponents(view: View) {
        inputEmail = view.findViewById(R.id.txtPasswordRecoveryByEmail)
        btnSubmit = view.findViewById(R.id.btnSubmitPasswordRecoveryByEmail)
        backBtn = view.findViewById(R.id.btnExitPasswordRecoveryByEmail)
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
    }

    // Set up button listeners
    private fun setupListeners() {
        backBtn.setOnClickListener { activity?.supportFragmentManager?.popBackStack() }
        btnSubmit.setOnClickListener { handlePasswordRecovery() }
    }

    // Handle password recovery
    private fun handlePasswordRecovery() {
        val email = inputEmail.text.toString().trim()

        if (email.isNotEmpty()) {
            sendPasswordRecoveryEmail(email)
        } else {
            inputEmail.error = "Email cannot be empty!"
        }
    }

    // Send password recovery email
    private fun sendPasswordRecoveryEmail(email: String) {
        userViewModel.viewModelScope.launch {
            val user = userViewModel.getUserByEmail(email)
            if (user != null) {
                val token = generateRandomToken()
                val expirationTime = System.currentTimeMillis() + TOKEN_EXPIRATION_TIME

                userViewModel.saveToken(email, token, expirationTime) { isSuccess ->
                    if (isSuccess) {
                        val resetLink = "$appBaseUrl/resetpassword?email=$email&token=$token"
                        val emailContent = generateEmailContent(user.username, resetLink)
                        sendEmail(email, emailContent)
                    } else {
                        showToast("Failed to save token. Try again.")
                    }
                }
            } else {
                showToast("Email not found!")
            }
        }
    }

    // Generate email content
    private fun generateEmailContent(username: String, resetLink: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333333; }
                    .container { max-width: 600px; margin: 0 auto; border: 1px solid #ddd; border-radius: 8px; padding: 20px; background-color: #f9f9f9; }
                    .header { background-color: #4CAF50; color: white; text-align: center; padding: 10px; font-size: 18px; border-radius: 8px 8px 0 0; }
                    .section { margin-bottom: 20px; }
                    .footer { text-align: center; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">Password Recovery</div>
                    <div class="section">
                        <p>Dear $username,</p>
                        <p>We received a request to reset your password for your TARUMT Campus Hub account.</p>
                        <p><a href="$resetLink">Click here to reset your password</a></p>
                        <p><strong>Note:</strong> This link will expire in 10 minutes.</p>
                    </div>
                    <div class="footer">
                        <p>If you didn't request this, please ignore this email. Your password will remain unchanged.</p>
                        <p>For further assistance, contact <a href="mailto:erika_fung26@outlook.com">erika_fung26@outlook.com</a>.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    // Send email using SendGrid
    private fun sendEmail(toEmail: String, content: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = sendEmailViaSendGrid(toEmail, content)
            CoroutineScope(Dispatchers.Main).launch {
                if (response) {
                    showEmailValidationDialog()
                } else {
                    showToast("Failed to send email.")
                }
            }
        }
    }

    // SendGrid API request
    private fun sendEmailViaSendGrid(toEmail: String, content: String): Boolean {
        val client = OkHttpClient()
        val jsonBody = JSONObject()

        jsonBody.put("personalizations", JSONArray().apply {
            put(JSONObject().apply {
                put("to", JSONArray().apply {
                    put(JSONObject().apply { put("email", toEmail) })
                })
            })
        })

        jsonBody.put("from", JSONObject().apply { put("email", "erika_fung26@outlook.com") })
        jsonBody.put("subject", "Password Recovery")
        jsonBody.put("content", JSONArray().apply {
            put(JSONObject().apply {
                put("type", "text/html")
                put("value", content)
            })
        })

        val body = RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody.toString())
        val request = Request.Builder()
            .url("https://api.sendgrid.com/v3/mail/send")
            .addHeader("Authorization", "Bearer $sendGridApiKey")
            .post(body)
            .build()

        return client.newCall(request).execute().use { response -> response.isSuccessful }
    }

    // Show email validation dialog
    private fun showEmailValidationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_email_recovery, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<ImageView>(R.id.imgCloseDeleteFriendDialog)?.setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<AppCompatButton>(R.id.btnYesDeleteFriendDialog)?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // Generate random token
    private fun generateRandomToken(): String {
        return java.util.UUID.randomUUID().toString()
    }

    // Show toast message
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}