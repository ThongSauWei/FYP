package com.example.fyp

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
import kotlinx.coroutines.launch
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
    private lateinit var btnMessage: AppCompatButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutBackground: View // Background view

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
        btnMessage = view.findViewById(R.id.btnMessageFriendProfile)
        layoutBackground = view.findViewById(R.id.linearLayoutFriendProfile) // Background layout
        recyclerView = view.findViewById(R.id.recyclerViewPostFriendProfile)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)

        setupFriendProfile()
        setupAddButton()
        setupMessageButton()

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

            val saveDAO = SaveDAO() // Initialize SaveDAO

            // Populate posts
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
                    saveDAO = saveDAO, // Pass the SaveDAO instance
                    context = requireContext(), // Pass the context
                    postViewModel = postViewModel, // Pass the PostViewModel instance
                    friendViewModel = friendViewModel, // Pass the FriendViewModel instance
                    isProfileMode = false // Profile mode for friend
                )
                recyclerView.adapter = adapter
            }
        }
    }

    private fun loadProfilePicture(userID: String) {
        val ref = storageRef.child("imageProfile").child("$userID.png")
        ref.downloadUrl.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val imageUrl = task.result.toString()
                Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.nullprofile) // Placeholder image
                    .error(R.drawable.nullprofile) // Error image
                    .into(imgProfile)

                // Set click listener to display the image in a dialog
                imgProfile.setOnClickListener {
                    showImageInDialog(imageUrl)
                }
            } else {
                imgProfile.setImageResource(R.drawable.nullprofile) // Default image
            }
        }
    }

    private fun loadBackgroundImage(userID: String) {
        val ref = storageRef.child("userBackgroundImage").child("$userID.png")
        ref.downloadUrl.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val imageUrl = task.result.toString()
                Picasso.get().load(imageUrl).into(object : com.squareup.picasso.Target {
                    override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
                        val drawable = BitmapDrawable(resources, bitmap)
                        layoutBackground.background = drawable // Set background
                    }

                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                        layoutBackground.setBackgroundColor(
                            requireContext().getColor(R.color.profile_color)
                        )
                    }

                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
                })
            } else {
                layoutBackground.setBackgroundColor(requireContext().getColor(R.color.profile_color))
            }
        }
    }

    private fun setupAddButton() {
        lifecycleScope.launch {
            val friend = friendViewModel.getFriend(currentUserID, friendUserID)

            when {
                friend == null -> {
                    btnAdd.text = "Add Friend"
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
                        setupAddButton()
                    }
                }
                friend.status == "Pending" && friend.receiveUserID == currentUserID -> {
                    btnAdd.text = "Accept"
                    btnAdd.setBackgroundColor(requireContext().getColor(R.color.light_grey))
                    btnAdd.setOnClickListener {
                        friend.status = "Friend"
                        friendViewModel.updateFriend(friend)
                        Toast.makeText(requireContext(), "Friend request accepted.", Toast.LENGTH_SHORT).show()
                        setupAddButton()
                    }
                }
                friend.status == "Pending" -> {
                    btnAdd.text = "Requested"
                    btnAdd.setBackgroundColor(requireContext().getColor(R.color.light_grey))
                    btnAdd.isClickable = false
                }
                friend.status == "Friend" -> {
                    btnAdd.text = "Unfriend"
                    btnAdd.setBackgroundColor(requireContext().getColor(R.color.red_button))
                    btnAdd.setOnClickListener {
                        friendViewModel.deleteFriend(friend.friendID)
                        Toast.makeText(requireContext(), "Friend removed.", Toast.LENGTH_SHORT).show()
                        setupAddButton()
                    }
                }
            }
        }
    }

    private fun setupMessageButton() {
        btnMessage.setOnClickListener {
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            val fragment = InnerChat()
            val bundle = Bundle()
            bundle.putString("friendUserID", friendUserID)
            fragment.arguments = bundle
            transaction?.replace(R.id.fragmentContainerView, fragment)
            transaction?.addToBackStack(null)
            transaction?.commit()
        }
    }

    private fun showImageInDialog(imageUrl: String) {
        val dialog = android.app.Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_image_view) // Ensure this layout exists
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val imageView = dialog.findViewById<ImageView>(R.id.dialogImageView)
        val closeButton = dialog.findViewById<ImageButton>(R.id.closeFullImageButton)

        Picasso.get().load(imageUrl).into(imageView)

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}