package com.example.fyp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fyp.data.Friend
import com.example.fyp.data.Post
import com.example.fyp.viewModel.FriendViewModel
import com.example.fyp.viewModel.PostViewModel
import com.example.fyp.viewModel.ProfileViewModel
import com.example.fyp.viewModel.UserViewModel
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FriendProfile : Fragment() {
    private val storageRef: StorageReference = FirebaseStorage.getInstance().getReference()

    private lateinit var friendViewModel: FriendViewModel

    private lateinit var imgProfile: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvPost: TextView
    private lateinit var tvFriend: TextView
    private lateinit var tvDOB: TextView
    private lateinit var tvCourse: TextView
    private lateinit var tvBio: TextView
    private lateinit var separator: View
    private lateinit var btnAdd: AppCompatButton
    private lateinit var layout: ConstraintLayout

    private lateinit var currentUserID: String

    private lateinit var postList: List<Post>

    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_friend_profile, container, false)

        (activity as MainActivity).setToolbar(R.layout.toolbar_with_profile, R.color.profile_color)

        if (arguments?.getString("friendUserID").isNullOrEmpty()) {
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            val fragment = SearchFriend()

            val bundle = Bundle()
            bundle.putString("NoSuchFriend", "No such friend")
            fragment.arguments = bundle

            transaction?.replace(R.id.fragmentContainerView, fragment)
            transaction?.addToBackStack(null)
            transaction?.commit()
        }

        currentUserID = SaveSharedPreference.getUserID(requireContext())

        imgProfile = view.findViewById(R.id.imgProfileFriendProfile)
        tvName = view.findViewById(R.id.tvNameFriendProfile)
        tvPost = view.findViewById(R.id.tvPostsProfile) // Changed "group" to "post"
        tvFriend = view.findViewById(R.id.tvFriendsFriendProfile)
        tvDOB = view.findViewById(R.id.tvDOBFriendProfile)
        tvCourse = view.findViewById(R.id.tvCoursesFriendProfile)
        tvBio = view.findViewById(R.id.tvBioFriendProfile)
        separator = view.findViewById(R.id.separatorFriendProfile)

        val friendUserID = arguments?.getString("friendUserID")!!

        friendViewModel = ViewModelProvider(this).get(FriendViewModel::class.java)

        recyclerView = view.findViewById(R.id.recyclerViewPostFriendProfile)
        profileSetup(friendUserID)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)

        layout = view.findViewById(R.id.layoutFriendProfile)

        btnAdd = view.findViewById(R.id.btnAddFriendFriendProfile)
        val btnMessage: AppCompatButton = view.findViewById(R.id.btnMessageFriendProfile)

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

        return view
    }

    private fun profileSetup(userID: String) {
        val userViewModel: UserViewModel =
            ViewModelProvider(this).get(UserViewModel::class.java)
        val profileViewModel: ProfileViewModel =
            ViewModelProvider(this).get(ProfileViewModel::class.java)
        /*val postViewModel: PostViewModel =
            ViewModelProvider(this).get(PostViewModel::class.java)*/

        lifecycleScope.launch {
            val friend = userViewModel.getUserByID(userID)!!
            val profile = profileViewModel.getProfile(userID)!!
            val totalFriend = friendViewModel.getFriendList(userID).size
            //postList = postViewModel.getPostByUser(userID) // Get posts instead of groups

            val ref = storageRef.child("imageProfile").child(friend.userID + ".png")
            ref.downloadUrl
                .addOnCompleteListener {
                    Glide.with(imgProfile).load(it.result.toString()).into(imgProfile)
                }

            tvName.text = friend.username
            tvPost.text = postList.size.toString() // Display total number of posts
            tvFriend.text = totalFriend.toString()
            tvDOB.text = friend.userDOB
            tvCourse.text = profile.userCourse
            tvBio.text = profile.userBio

            buttonSetup(currentUserID, userID)

            /*if (postList.isNotEmpty()) {
                val adapter = PostAdapter(postList)
                adapter.setViewModel(userViewModel)
                recyclerView.adapter = adapter

            } else {
                noPostSetup()
            }*/
        }
    }

    private fun buttonSetup(userID: String, friendUserID: String) {
        lifecycleScope.launch {
            val friend = friendViewModel.getFriend(userID, friendUserID)

            var onClickListener: (View) -> Unit = {}

            if (friend != null) {
                when (friend.status) {
                    "Pending", "Blocked", "Friend" -> {
                        if (friend.status == "Pending" && friend.receiveUserID == currentUserID) {
                            btnAdd.text = "Accept"

                            onClickListener = {
                                val updatedFriend =
                                    Friend(friend.friendID, friend.requestUserID, friend.receiveUserID, "Friend", getCurrentTimestamp())
                                friendViewModel.updateFriend(updatedFriend)
                            }

                        } else {
                            btnAdd.text = friend.status
                            btnAdd.setBackgroundColor(requireContext().getColor(R.color.light_grey))
                            btnAdd.isClickable = false
                        }
                    }
                    "Blocking" -> {
                        btnAdd.text = "Unblock"

                        onClickListener = {
                            val updatedFriend =
                                Friend(friend.friendID, friend.requestUserID, friend.receiveUserID, "Friend", getCurrentTimestamp())
                            friendViewModel.updateFriend(updatedFriend)
                        }
                    }
                    else -> {

                    }
                }
            } else {
                onClickListener = {
                    val newFriend = Friend("0", userID, friendUserID, "Pending", getCurrentTimestamp())

                    friendViewModel.addFriend(newFriend)
                }
            }

            btnAdd.setOnClickListener(onClickListener)
        }
    }

    private fun noPostSetup() {
        val tvNoPost = TextView(requireContext())
        tvNoPost.id = View.generateViewId()
        tvNoPost.text = "User has No Posts"
        tvNoPost.typeface = ResourcesCompat.getFont(requireContext(), R.font.abhayalibre_semibold)
        tvNoPost.setTextColor(requireContext().getColor(R.color.white))
        tvNoPost.textSize = 30f

        layout.addView(tvNoPost)

        val constraintSet = ConstraintSet()
        constraintSet.clone(layout)

        constraintSet.connect(
            tvNoPost.id,
            ConstraintSet.TOP,
            separator.id,
            ConstraintSet.BOTTOM,
            150
        )

        constraintSet.connect(
            tvNoPost.id,
            ConstraintSet.START,
            layout.id,
            ConstraintSet.START
        )

        constraintSet.connect(
            tvNoPost.id,
            ConstraintSet.END,
            layout.id,
            ConstraintSet.END
        )

        // apply the constraints to the layout
        constraintSet.applyTo(layout)
    }

    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
