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

        inputEmail = view.findViewById(R.id.txtPasswordRecoveryByEmail)
        btnSubmit = view.findViewById(R.id.btnSubmitPasswordRecoveryByEmail)
        backBtn = view.findViewById(R.id.btnExitPasswordRecoveryByEmail)
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)

        backBtn.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }

        btnSubmit.setOnClickListener {
            val email = inputEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                sendPasswordRecoveryEmail(email)
            } else {
                inputEmail.error = "Email cannot be empty!"
            }
        }

        return view
    }

    private fun sendPasswordRecoveryEmail(email: String) {
        userViewModel.viewModelScope.launch {
            val user = userViewModel.getUserByEmail(email)
            if (user != null) {
                val token = generateRandomToken()
                val expirationTime = System.currentTimeMillis() + TOKEN_EXPIRATION_TIME

                userViewModel.saveToken(email, token, expirationTime) { isSuccess ->
                    if (isSuccess) {
                        val resetLink = "$appBaseUrl/resetpassword?email=$email&token=$token"
                        val emailContent = """
                            <p>Dear ${user.username},</p>
                            <p>We hope this message finds you well.</p>
                            <p>You are receiving this email because we received a request to reset your password for your TARUMT Campus Hub account.</p>
                            <p>If you made this request, please click the link below to reset your password:</p>
                            <p><a href="$resetLink">Reset Password</a></p>
                            <p>If you did not request this, you can safely ignore this email. Your password will remain unchanged.</p>
                            <p>For security reasons, this link will expire in 10 minutes.</p>
                            <p>If you have any questions or concerns, please do not hesitate to contact us at erika_fung26@outlook.com.</p>
                            <p>Thank you for using TARUMT Campus Hub.</p>
                            <p>Best regards,</p>
                            <p>The TARUMT Campus Hub Team</p>
                        """.trimIndent()

                        CoroutineScope(Dispatchers.IO).launch {
                            val response = sendEmailViaSendGrid(email, emailContent)
                            CoroutineScope(Dispatchers.Main).launch {
                                if (response) {
                                    showEmailValidationDialog()
                                } else {
                                    Toast.makeText(requireContext(), "Failed to send email.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "Failed to save token. Try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Email not found!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendEmailViaSendGrid(toEmail: String, content: String): Boolean {
        val client = OkHttpClient()
        val jsonBody = JSONObject()

        jsonBody.put("personalizations", JSONArray().apply {
            put(JSONObject().apply {
                put("to", JSONArray().apply {
                    put(JSONObject().apply {
                        put("email", toEmail)
                    })
                })
            })
        })

        jsonBody.put("from", JSONObject().apply {
            put("email", "erika_fung26@outlook.com") // Replace with verified sender email
        })
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

        client.newCall(request).execute().use { response ->
            return response.isSuccessful
        }
    }

    private fun showEmailValidationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_email_recovery, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val closeBtn = dialogView.findViewById<ImageView>(R.id.imgCloseDeleteFriendDialog)
        val okayBtn = dialogView.findViewById<AppCompatButton>(R.id.btnYesDeleteFriendDialog)

        closeBtn.setOnClickListener {
            dialog.dismiss()
        }

        okayBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun generateRandomToken(): String {
        return java.util.UUID.randomUUID().toString()
    }
}
