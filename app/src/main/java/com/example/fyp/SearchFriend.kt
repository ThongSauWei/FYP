package com.example.fyp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fyp.dataAdapter.SearchFriendAdapter
import com.example.fyp.viewModel.FriendViewModel
import com.example.fyp.viewModel.ProfileViewModel
import com.example.fyp.viewModel.UserViewModel
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import kotlinx.coroutines.launch

class SearchFriend : Fragment() {

    private lateinit var txtSearch: EditText
    private lateinit var btnSearch: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var userViewModel: UserViewModel
    private lateinit var friendViewModel: FriendViewModel
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var adapter: SearchFriendAdapter
    private lateinit var currentUserID: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search_friend, container, false)
        (activity as MainActivity).setToolbar(R.layout.toolbar_with_annouce_and_title)
        // Initialize Views
        txtSearch = view.findViewById(R.id.txtSearchSearchFriend)
        btnSearch = view.findViewById(R.id.btnSearchSearchFriend)
        recyclerView = view.findViewById(R.id.recyclerViewFriendSearchFriend)

        // Initialize ViewModels
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        friendViewModel = ViewModelProvider(this).get(FriendViewModel::class.java)
        profileViewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)
        currentUserID = SaveSharedPreference.getUserID(requireContext())

        // Initialize RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)
        adapter = SearchFriendAdapter(friendViewModel, profileViewModel, currentUserID, parentFragmentManager)
        recyclerView.adapter = adapter

        // Load Suggested Friends
        loadSuggestedFriends()

        // Search Button
        btnSearch.setOnClickListener {
            val searchText = txtSearch.text.toString().trim()
            if (searchText.isNotEmpty()) {
                searchUsers(searchText)
            } else {
                activity?.supportFragmentManager?.popBackStack()
                goBack()
            }
        }

        return view
    }

    private fun loadSuggestedFriends() {
        lifecycleScope.launch {
            try {
                val allUsers = userViewModel.getAllUsers()
                val allProfiles = profileViewModel.getAllProfiles()
                val friends = friendViewModel.getFriendList(currentUserID)

                // Pass filtered users (not friends) and profiles to adapter
                adapter.setSuggestedFriends(allUsers, allProfiles, friends)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load suggested friends: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun searchUsers(searchText: String) {
        lifecycleScope.launch {
            try {
                val userList = userViewModel.searchUsers(searchText)
                val allProfiles = profileViewModel.getAllProfiles()
                adapter.setSearchResults(userList, allProfiles)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Search failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goBack()
    {
        val transaction = activity?.supportFragmentManager?.beginTransaction()//after success go to home
        val fragment = SearchFriend()
        transaction?.replace(R.id.fragmentContainerView, fragment)
        transaction?.addToBackStack(null)
        transaction?.commit()
    }
}
