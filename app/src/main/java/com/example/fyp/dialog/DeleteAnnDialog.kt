package com.example.fyp.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.fyp.Annoucement
import com.example.fyp.Home
import com.example.fyp.R
import com.example.fyp.dao.AnnoucementDAO
import com.example.fyp.viewModel.FriendViewModel
import java.lang.IllegalStateException

class DeleteAnnDialog : DialogFragment() {
    lateinit var announcementDAO: AnnoucementDAO
    var announcementID: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it, R.style.CustomAlertDialog)
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.delete_annoucement, null)

            val btnYes: Button = view.findViewById(R.id.btnYes)
            val btnNo: Button = view.findViewById(R.id.btnNo)
            val imgClose: ImageView = view.findViewById(R.id.imgCloseDeleteFriendDialog)

            announcementDAO = AnnoucementDAO()

            btnYes.setOnClickListener {
                announcementID?.let { id ->
                    announcementDAO.deleteAnnouncement(id) { success, exception ->
                        if (success) {
                            context?.let {
                                Toast.makeText(it, "Announcement status updated successfully", Toast.LENGTH_SHORT).show()
                                val fragment = Annoucement()
                                val transaction = activity?.supportFragmentManager?.beginTransaction()
                                transaction?.replace(R.id.fragmentContainerView, fragment)
                                transaction?.addToBackStack(null)
                                transaction?.commit()
                            }
                            dismiss() // Safely dismiss the dialog
                        } else {
                            Log.e("DeleteAnnDialog", "Error updating announcement status: ${exception?.message}")
                            context?.let {
                                Toast.makeText(it, "Failed to update announcement status", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }



            btnNo.setOnClickListener {
                dismiss()
            }

            imgClose.setOnClickListener {
                dismiss()
            }

            builder.setView(view)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
