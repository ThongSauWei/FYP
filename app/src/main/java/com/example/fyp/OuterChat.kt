package com.example.fyp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fyp.data.Chat
import com.example.fyp.dataAdapter.OuterChatAdapter
import com.example.fyp.viewModel.ChatViewModel
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference

class OuterChat : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OuterChatAdapter
    private lateinit var chatViewModel: ChatViewModel
    private lateinit var txtSearchOuterChat: EditText

    private lateinit var currentUserID: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_outer_chat, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewFriendOuterChat)
        txtSearchOuterChat = view.findViewById(R.id.txtSearchOuterChat)
        chatViewModel = ViewModelProvider(this).get(ChatViewModel::class.java)

        // Get the current logged-in user ID
        currentUserID = SaveSharedPreference.getUserID(requireContext())

        setupRecyclerView()
        setupSearch()
        fetchChats()

        return view
    }

    private fun setupRecyclerView() {
        adapter = OuterChatAdapter(listOf(), currentUserID) { chat, friendUserID ->
            // Navigate to InnerChat
            val bundle = Bundle().apply {
                putString("chatID", chat.chatID)
                putString("friendUserID", friendUserID)
            }
            val innerChatFragment = InnerChat()
            innerChatFragment.arguments = bundle
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.outer_chat, innerChatFragment)
                .addToBackStack(null)
                .commit()
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun fetchChats() {
        chatViewModel.fetchChatsByUser(currentUserID) // Trigger the fetch
        chatViewModel.chatList.observe(viewLifecycleOwner) { chatList ->
            adapter.updateChatList(chatList)
        }
    }


    private fun setupSearch() {
        txtSearchOuterChat.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                chatViewModel.searchChats(query).observe(viewLifecycleOwner) { filteredChats ->
                    adapter.updateChatList(filteredChats)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
}
