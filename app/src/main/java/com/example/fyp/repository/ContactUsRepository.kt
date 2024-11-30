package com.example.fyp.repository

import android.util.Log
import com.example.fyp.dao.ContactUsDAO
import com.example.fyp.data.ContactUs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject

class ContactUsRepository {
    private val contactUsDAO = ContactUsDAO()

    suspend fun addContactUs(contactUs: ContactUs) {
        contactUsDAO.addContactUs(contactUs)
    }

    fun sendEmail(toEmail: String, subject: String, name: String, email: String, problemType: String, description: String, dateTime: String) {
        // Perform network operation on a background thread
        GlobalScope.launch(Dispatchers.IO) {  // Use IO dispatcher for network operations
            try {
                val sendGridApiKey = "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"
                val client = OkHttpClient()
                val jsonBody = JSONObject()

                val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333333; }
                    .container { max-width: 600px; margin: 0 auto; border: 1px solid #ddd; border-radius: 8px; padding: 20px; background-color: #f9f9f9; }
                    .header { background-color: #4CAF50; color: white; text-align: center; padding: 10px; font-size: 18px; border-radius: 8px 8px 0 0; }
                    .section { margin-bottom: 20px; }
                    .label { font-weight: bold; }
                    .footer { text-align: center; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">New Contact Us Report</div>
                    <div class="section">
                        <p><span class="label">Name:</span> $name</p>
                        <p><span class="label">Email:</span> $email</p>
                        <p><span class="label">Problem Type:</span> $problemType</p>
                    </div>
                    <div class="section">
                        <p><span class="label">Description:</span></p>
                        <p>$description</p>
                    </div>
                    <div class="section">
                        <p><span class="label">Date and Time:</span> $dateTime</p>
                    </div>
                    <div class="footer">
                        <p>Sent via the Contact Us form from the app.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

                jsonBody.put("personalizations", JSONArray().apply {
                    put(JSONObject().apply {
                        put("to", JSONArray().apply {
                            put(JSONObject().apply { put("email", toEmail) })
                        })
                        put("subject", subject)
                    })
                })
                jsonBody.put("from", JSONObject().apply { put("email", "erika_fung26@outlook.com") })
                jsonBody.put("content", JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "text/html")
                        put("value", htmlContent)
                    })
                })

                val body = RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody.toString())
                val request = Request.Builder()
                    .url("https://api.sendgrid.com/v3/mail/send")
                    .addHeader("Authorization", "Bearer $sendGridApiKey")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                Log.d("ContactUsRepository", "SendGrid Response Code: ${response.code}")
                if (!response.isSuccessful) {
                    throw Exception("Failed to send email: ${response.body?.string()}")
                }
            } catch (e: Exception) {
                Log.e("ContactUsRepository", "Error sending email: ${e.message}")
                throw e
            }
        }
    }


}
