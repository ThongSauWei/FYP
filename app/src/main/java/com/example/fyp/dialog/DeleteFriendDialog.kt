package com.example.fyp.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.fyp.R
import com.example.fyp.viewModel.FriendViewModel

class DeleteFriendDialog : DialogFragment() {

    lateinit var viewModel: FriendViewModel
    lateinit var friendID: String
    lateinit var username: String

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it, R.style.CustomAlertDialog)
            val inflater = requireActivity().layoutInflater

            val view = inflater.inflate(R.layout.delete_friend_dialog, null)

            val btnYes: Button = view.findViewById(R.id.btnYesDeleteFriendDialog)
            val btnNo: Button = view.findViewById(R.id.btnNoDeleteFriendDialog)
            val imgClose: ImageView = view.findViewById(R.id.imgCloseDeleteFriendDialog)
            val tvDelete: TextView = view.findViewById(R.id.tvDeleteDeleteFriendDialog)

            val context = requireContext()
            tvDelete.text = context.getString(R.string.confirm_delete, username)

            btnYes.setOnClickListener {
                viewModel.deleteFriend(friendID) // Trigger manual reload in ViewModel
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
