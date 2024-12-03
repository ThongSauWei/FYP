package com.example.fyp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fyp.dataAdapter.FriendAdapter
import com.example.fyp.data.Profile
import com.example.fyp.data.User
import com.example.fyp.viewModel.FriendViewModel
import com.example.fyp.viewModel.ProfileViewModel
import com.example.fyp.viewModel.UserViewModel
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import kotlinx.coroutines.launch

class Friends : Fragment() {

    private lateinit var tvFriendCount: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var friendViewModel: FriendViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var profileViewModel: ProfileViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_friend, container, false)
        (activity as MainActivity).setToolbar(R.layout.toolbar_with_annouce_and_title)
        // Initialize views
        tvFriendCount = view.findViewById(R.id.tvFriendCountNumberFriends)
        recyclerView = view.findViewById(R.id.recyclerViewFriendFriends)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)

        // Initialize ViewModels
        friendViewModel = ViewModelProvider(this).get(FriendViewModel::class.java)
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        profileViewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)

        loadFriends()
        observeFriendList()

        return view
    }

    private fun observeFriendList() {
        friendViewModel.friendList.observe(viewLifecycleOwner) { friendList ->
            lifecycleScope.launch {
                val adapter = FriendAdapter(
                    userList = mutableListOf(), // Populate dynamically
                    profileList = mutableListOf(),
                    friendList = friendList,
                    fragmentManager = parentFragmentManager,
                    friendViewModel = friendViewModel
                )
                recyclerView.adapter = adapter
                tvFriendCount.text = friendList.size.toString()
            }
        }

        // Initial load
        friendViewModel.refreshFriendList(
            com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference.getUserID(requireContext())
        )
    }

    private fun loadFriends() {
        friendViewModel.friendList.observe(viewLifecycleOwner) { friendList ->
            lifecycleScope.launch {
                val userList = mutableListOf<User>()
                val profileList = mutableListOf<Profile>()

                for (friend in friendList) {
                    val friendUserID =
                        if (friend.requestUserID == SaveSharedPreference.getUserID(requireContext())) {
                            friend.receiveUserID
                        } else {
                            friend.requestUserID
                        }

                    val user = userViewModel.getUserByID(friendUserID)
                    val profile = profileViewModel.getProfile(friendUserID)

                    // Handle null values gracefully
                    val safeUser = user ?: User(
                        userID = friendUserID,
                        username = "Unknown User",
                        email = "",
                        userMobileNo = "",
                        userDOB = "",
                        password = "",
                        securityQuestion = "",
                        token = ""
                    )
                    val safeProfile = profile ?: Profile(
                        userID = friendUserID,
                        userCourse = "TARUMT",
                        userBio = "",
                        userImage = "",
                        userGender = "",
                        userBackgroundImage = "",
                        userChosenLanguage = ""
                    )

                    userList.add(safeUser)
                    profileList.add(safeProfile)
                }

                val adapter = FriendAdapter(
                    userList,
                    profileList,
                    friendList,
                    parentFragmentManager,
                    friendViewModel
                )
                recyclerView.adapter = adapter
                tvFriendCount.text = userList.size.toString()
            }
        }
    }
}
