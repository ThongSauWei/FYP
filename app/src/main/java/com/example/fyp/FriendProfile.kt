package com.example.fyp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
            Glide.with(imgProfile).load(storageRef.child("imageProfile/$friendUserID.png")).into(imgProfile)
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
                    isProfileMode = true // Custom flag for profile-specific logic
                )
                recyclerView.adapter = adapter
            }
        }
    }

    private fun setupAddButton() {
        lifecycleScope.launch {
            val friend = friendViewModel.getFriend(currentUserID, friendUserID)

            when {
                friend == null -> {
                    // Not friends
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
                    // Request received
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
                    // Request sent
                    btnAdd.text = "Requested"
                    btnAdd.setBackgroundColor(requireContext().getColor(R.color.light_grey))
                    btnAdd.isClickable = false
                }
                friend.status == "Friend" -> {
                    // Already friends
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

    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
