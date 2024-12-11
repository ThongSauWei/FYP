package com.example.fyp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.lifecycleScope;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fyp.data.Chat;
import com.example.fyp.dataAdapter.OuterChatAdapter;
import com.example.fyp.viewModel.ChatViewModel;
import com.example.fyp.viewModel.UserViewModel;
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.launch;
import kotlinx.coroutines.withContext;

class OuterChat : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OuterChatAdapter
    private lateinit var chatViewModel: ChatViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var txtSearchOuterChat: EditText
    private lateinit var currentUserID: String
    private var chatList: List<Chat> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_outer_chat, container, false)

        // Set the toolbar with announcement
        (activity as MainActivity).setToolbar(R.layout.toolbar_with_annouce_and_title)

        recyclerView = view.findViewById(R.id.recyclerViewFriendOuterChat)
        txtSearchOuterChat = view.findViewById(R.id.txtSearchOuterChat)

        chatViewModel = ViewModelProvider(this).get(ChatViewModel::class.java)
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)

        currentUserID = SaveSharedPreference.getUserID(requireContext())

        setupRecyclerView()
        setupSearch()
        fetchChats()
        listenForUpdates()

        return view
    }

    override fun onResume() {
        super.onResume()
        // Reset toolbar
        (activity as MainActivity).setToolbar(R.layout.toolbar_with_annouce_and_title)

        // Reload chats
        fetchChats()
    }

    private fun setupRecyclerView() {
        adapter = OuterChatAdapter(listOf(), currentUserID) { chat, friendUserID ->
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
        chatViewModel.fetchChatsByUser(currentUserID)
        chatViewModel.chatList.observe(viewLifecycleOwner) { fetchedChats ->
            chatList = fetchedChats
            adapter.updateChatList(chatList)
        }
    }

    private fun setupSearch() {
        txtSearchOuterChat.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    performSearch(query)
                } else {
                    adapter.updateChatList(chatList)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun performSearch(query: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val filteredChats = chatList.filter { chat ->
                val friendUserID = if (chat.initiatorUserID == currentUserID) chat.receiverUserID else chat.initiatorUserID
                val user = userViewModel.getUserByID(friendUserID)
                user?.username?.contains(query, ignoreCase = true) == true
            }
            withContext(Dispatchers.Main) {
                adapter.updateChatList(filteredChats)
            }
        }
    }

    private fun listenForUpdates() {
        chatViewModel.listenForChatUpdates(currentUserID)
        chatViewModel.chatList.observe(viewLifecycleOwner) { updatedChats ->
            chatList = updatedChats
            adapter.updateChatList(chatList)
        }
    }
}

