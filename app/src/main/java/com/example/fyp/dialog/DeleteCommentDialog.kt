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
import com.example.fyp.data.PostComment
import com.example.fyp.viewModel.FriendViewModel
import java.lang.IllegalStateException

class DeleteCommentDialog(private val comment: PostComment, private val onDeleteConfirmed: (PostComment) -> Unit) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it, R.style.CustomAlertDialog)
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.delete_annoucement, null)

            val btnYes: Button = view.findViewById(R.id.btnYes)
            val btnNo: Button = view.findViewById(R.id.btnNo)
            val imgClose: ImageView = view.findViewById(R.id.imgCloseDeleteFriendDialog)

            btnYes.setOnClickListener {
                onDeleteConfirmed(comment)
                dismiss()
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

