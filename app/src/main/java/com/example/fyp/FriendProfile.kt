package com.example.fyp

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fyp.dao.LikeDAO
import com.example.fyp.dao.PostCategoryDAO
import com.example.fyp.dao.PostCommentDAO
import com.example.fyp.dao.PostImageDAO
import com.example.fyp.dao.SaveDAO
import com.example.fyp.data.Friend
import com.example.fyp.data.Post
import com.example.fyp.dataAdapter.PostAdapter
import com.example.fyp.viewModel.FriendViewModel
import com.example.fyp.viewModel.PostViewModel
import com.example.fyp.viewModel.ProfileViewModel
import com.example.fyp.viewModel.UserViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class FriendProfile : Fragment() {
    private lateinit var imgProfile: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvPost: TextView
    private lateinit var tvFriend: TextView
    private lateinit var tvDOB: TextView
    private lateinit var tvCourse: TextView
    private lateinit var tvBio: TextView
    private lateinit var btnAdd: AppCompatButton
    private lateinit var btnRequest: AppCompatButton
    private lateinit var btnUnfriend: AppCompatButton
    private lateinit var btnMessage: AppCompatButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutBackground: View
    private lateinit var tvGenderFriendProfile: TextView
    private lateinit var imageGenderFriendProfile: ImageView

    private val storageRef = FirebaseStorage.getInstance().getReference()
    private lateinit var friendViewModel: FriendViewModel
    private lateinit var postViewModel: PostViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var profileViewModel: ProfileViewModel

    private lateinit var currentUserID: String
    private lateinit var friendUserID: String
    private lateinit var postList: List<Post>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_friend_profile, container, false)

        currentUserID = SaveSharedPreference.getUserID(requireContext())
        friendUserID = arguments?.getString("friendUserID") ?: ""

        // Navigate to Profile if friendUserID matches currentUserID
        if (friendUserID == currentUserID) {
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            transaction?.replace(R.id.fragmentContainerView, Profile())
            transaction?.addToBackStack(null)
            transaction?.commit()
            return null
        }

        friendViewModel = ViewModelProvider(this).get(FriendViewModel::class.java)
        postViewModel = ViewModelProvider(this).get(PostViewModel::class.java)
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        profileViewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)

        imgProfile = view.findViewById(R.id.imgProfileFriendProfile)
        tvName = view.findViewById(R.id.tvNameFriendProfile)
        tvPost = view.findViewById(R.id.tvPostsProfile)
        tvFriend = view.findViewById(R.id.tvFriendsFriendProfile)
        tvDOB = view.findViewById(R.id.tvDOBFriendProfile)
        tvCourse = view.findViewById(R.id.tvCoursesFriendProfile)
        tvBio = view.findViewById(R.id.tvBioFriendProfile)
        btnAdd = view.findViewById(R.id.btnAddFriendFriendProfile)
        btnRequest = view.findViewById(R.id.btnRequestFriendFriendProfile)
        btnUnfriend = view.findViewById(R.id.btnUnfriendFriendProfile)
        btnMessage = view.findViewById(R.id.btnMessageFriendProfile)
        layoutBackground = view.findViewById(R.id.linearLayoutFriendProfile)
        recyclerView = view.findViewById(R.id.recyclerViewPostFriendProfile)
        tvGenderFriendProfile = view.findViewById(R.id.tvGenderFriendProfile)
        imageGenderFriendProfile = view.findViewById(R.id.imageFriendGender)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)

        setupFriendProfile()
        setupButtons()

        return view
    }

    private fun setupFriendProfile() {
        lifecycleScope.launch {
            val user = userViewModel.getUserByID(friendUserID)
            val profile = profileViewModel.getProfile(friendUserID)
            val totalFriends = friendViewModel.getFriendList(friendUserID).size
            postList = postViewModel.getPostByUser(friendUserID)

            // Populate UI
            loadProfilePicture(friendUserID)
            loadBackgroundImage(friendUserID)
            tvName.text = user?.username
            tvPost.text = postList.size.toString()
            tvFriend.text = totalFriends.toString()
            tvDOB.text = user?.userDOB
            tvCourse.text = profile?.userCourse
            tvBio.text = profile?.userBio

            // Set gender text and icon
            tvGenderFriendProfile.text = profile?.userGender ?: ""
            when (profile?.userGender) {
                "Male" -> {
                    imageGenderFriendProfile.setImageResource(R.drawable.baseline_male_24)
                    imageGenderFriendProfile.visibility = View.VISIBLE
                }
                "Female" -> {
                    imageGenderFriendProfile.setImageResource(R.drawable.baseline_female_24)
                    imageGenderFriendProfile.visibility = View.VISIBLE
                }
                else -> {
                    tvGenderFriendProfile.visibility = View.INVISIBLE
                    imageGenderFriendProfile.visibility = View.INVISIBLE
                }
            }

            val saveDAO = SaveDAO()

            if (postList.isNotEmpty()) {
                val adapter = PostAdapter(
                    posts = postList,
                    userViewModel = userViewModel,
                    postImageDAO = PostImageDAO(
                        FirebaseStorage.getInstance().reference,
                        FirebaseDatabase.getInstance().reference
                    ),
                    postCategoryDAO = PostCategoryDAO(),
                    likeDAO = LikeDAO(),
                    postCommentDAO = PostCommentDAO(),
                    saveDAO = saveDAO,
                    context = requireContext(),
                    postViewModel = postViewModel,
                    friendViewModel = friendViewModel,
                    isProfileMode = false
                )
                recyclerView.adapter = adapter
            }
        }
    }


    private fun setupButtons() {
        friendViewModel.observeFriendStatus(currentUserID, friendUserID) { friend ->
            btnAdd.visibility = View.GONE
            btnRequest.visibility = View.GONE
            btnUnfriend.visibility = View.GONE
            btnMessage.visibility = View.VISIBLE // Always show the Message button

            // Navigate to chat regardless of friend status
            btnMessage.setOnClickListener { navigateToInnerChat() }

            when (friend?.status) {
                null -> {
                    btnAdd.visibility = View.VISIBLE
                    btnAdd.setOnClickListener {
                        val newFriend = Friend(
                            friendID = "0",
                            requestUserID = currentUserID,
                            receiveUserID = friendUserID,
                            status = "Pending",
                            timeStamp = getCurrentTimestamp()
                        )
                        friendViewModel.addFriend(newFriend)
                        Toast.makeText(requireContext(), "Friend request sent.", Toast.LENGTH_SHORT).show()
                    }
                }
                "Pending" -> {
                    btnRequest.visibility = View.VISIBLE
                    btnRequest.isClickable = false
                }
                "Friend" -> {
                    btnUnfriend.visibility = View.VISIBLE
                    btnUnfriend.setOnClickListener { showDeleteFriendDialog(friend) }
                }
                "Rejected" -> {
                    btnAdd.visibility = View.VISIBLE
                    btnAdd.setOnClickListener {
                        val newFriend = Friend(
                            friendID = "0",
                            requestUserID = currentUserID,
                            receiveUserID = friendUserID,
                            status = "Pending",
                            timeStamp = getCurrentTimestamp()
                        )
                        friendViewModel.addFriend(newFriend)
                        Toast.makeText(requireContext(), "Friend request sent.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun navigateToInnerChat() {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        val fragment = InnerChat() // Assuming InnerChat is a Fragment
        val bundle = Bundle()

        // Pass the friendUserID to the InnerChat fragment
        bundle.putString("friendUserID", friendUserID)
        fragment.arguments = bundle

        // Navigate to the InnerChat fragment
        transaction?.replace(R.id.fragmentContainerView, fragment)
        transaction?.addToBackStack(null)
        transaction?.commit()
    }

    private fun showDeleteFriendDialog(friend: Friend) {
        val dialogView = layoutInflater.inflate(R.layout.delete_friend_dialog, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val btnYes = dialogView.findViewById<AppCompatButton>(R.id.btnYesDeleteFriendDialog)
        val btnNo = dialogView.findViewById<AppCompatButton>(R.id.btnNoDeleteFriendDialog)
        val closeDialog = dialogView.findViewById<ImageView>(R.id.imgCloseDeleteFriendDialog)

        btnYes.setOnClickListener {
            // Delete friend and await confirmation
            friendViewModel.deleteFriend(friend.friendID)
            lifecycleScope.launch {
                val friendDeleted = withContext(Dispatchers.IO) {
                    friendViewModel.checkFriendDeleted(friend.friendID) // Add this method to verify deletion
                }
                if (friendDeleted) {
                    Toast.makeText(requireContext(), getString(R.string.friend_removed), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), getString(R.string.error_friend_not_removed), Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
        }

        btnNo.setOnClickListener { dialog.dismiss() }
        closeDialog.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }



    private fun loadProfilePicture(userID: String) {
        val ref = storageRef.child("imageProfile/$userID.png")
        ref.downloadUrl.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val imageUrl = task.result.toString()
                if (isAdded && context != null) { // Ensure fragment is still attached
                    Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.nullprofile)
                        .error(R.drawable.nullprofile)
                        .into(imgProfile)
                    imgProfile.setOnClickListener { showImageInDialog(imageUrl) }
                }
            } else {
                if (isAdded && context != null) { // Ensure fragment is still attached
                    imgProfile.setImageResource(R.drawable.nullprofile)
                }
            }
        }
    }


    private fun loadBackgroundImage(userID: String) {
        val ref = storageRef.child("userBackgroundImage/$userID.png")
        ref.downloadUrl.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val imageUrl = task.result.toString()
                Picasso.get().load(imageUrl).into(object : com.squareup.picasso.Target {
                    override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
                        if (isAdded && context != null) { // Ensure fragment is still attached
                            layoutBackground.background = BitmapDrawable(resources, bitmap)
                        }
                    }

                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                        if (isAdded && context != null) { // Ensure fragment is still attached
                            layoutBackground.setBackgroundColor(
                                requireContext().getColor(R.color.profile_color)
                            )
                        }
                    }

                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
                })
            } else {
                if (isAdded && context != null) { // Ensure fragment is still attached
                    layoutBackground.setBackgroundColor(
                        requireContext().getColor(R.color.profile_color)
                    )
                }
            }
        }
    }

    private fun showImageInDialog(imageUrl: String) {
        if (!isAdded || context == null) return // Ensure fragment is attached

        val dialog = android.app.Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_image_view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val imageView = dialog.findViewById<ImageView>(R.id.dialogImageView)
        val closeButton = dialog.findViewById<ImageButton>(R.id.closeFullImageButton)

        Picasso.get().load(imageUrl).into(imageView)
        closeButton.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
