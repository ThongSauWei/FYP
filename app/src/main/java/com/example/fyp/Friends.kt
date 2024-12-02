package com.example.fyp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fyp.dataAdapter.FriendAdapter
import com.example.fyp.dialog.DeleteFriendDialog
import com.example.fyp.viewModel.FriendViewModel
import com.example.fyp.viewModel.UserViewModel
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import kotlinx.coroutines.launch

class Friends : Fragment() {

    private lateinit var friendViewModel: FriendViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var friendAdapter: FriendAdapter
    private lateinit var tvFriendCount: TextView
    private lateinit var recyclerView: RecyclerView
    private val currentUserID by lazy { SaveSharedPreference.getUserID(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_friend, container, false)

        // Initialize Views
        tvFriendCount = view.findViewById(R.id.tvFriendCountNumberFriends)
        recyclerView = view.findViewById(R.id.recyclerViewFriendFriends)

        // Initialize ViewModels
        friendViewModel = ViewModelProvider(requireActivity()).get(FriendViewModel::class.java)
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)

        // Setup RecyclerView
        setupRecyclerView()

        // Load Friends
        loadFriends()

        return view
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)
        friendAdapter = FriendAdapter(FriendAdapter.Mode.DELETE).apply {
            setFragmentManager(parentFragmentManager)
            setViewModel(friendViewModel)
            setDeleteFriendDialog(DeleteFriendDialog())
            setOnItemClickListener { user, _, _ ->
                navigateToChat(user.userID)
            }
        }
        recyclerView.adapter = friendAdapter
    }

    private fun loadFriends() {
        lifecycleScope.launch {
            try {
                val friends = friendViewModel.getFriendList(currentUserID)
                val users = friends.mapNotNull {
                    val otherUserID =
                        if (it.receiveUserID != currentUserID) it.receiveUserID else it.requestUserID
                    userViewModel.getUserByID(otherUserID)
                }

                friendAdapter.setFriendList(friends)
                friendAdapter.setUserList(users)

                // Update Friend Count
                tvFriendCount.text = friends.size.toString()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load friends: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToChat(friendUserID: String) {
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
