package com.example.fyp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout
import android.widget.PopupMenu;
import android.widget.ScrollView
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.lifecycleScope;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.fyp.dao.ChatLineDAO;
import com.example.fyp.dao.UserDAO;
import com.example.fyp.data.ChatLine;
import com.example.fyp.dataAdapter.InnerChatAdapter;
import com.example.fyp.repository.ChatLineRepository;
import com.google.firebase.storage.FirebaseStorage;
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.launch;
import kotlinx.coroutines.withContext;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class InnerChat : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: InnerChatAdapter
    private lateinit var txtChat: EditText
    private lateinit var btnSend: ImageView
    private lateinit var btnAttachment: ImageView
    private lateinit var btnCamera: ImageView
    private lateinit var imgProfile: ImageView
    private lateinit var tvName: TextView
    private lateinit var btnBackInnerChat: ImageView

    private lateinit var chatLineRepository: ChatLineRepository
    private lateinit var chatLineDAO: ChatLineDAO
    private lateinit var userDAO: UserDAO

    private val storageRef = FirebaseStorage.getInstance().reference

    private lateinit var currentUserID: String
    private lateinit var friendUserID: String
    private lateinit var chatID: String
    private lateinit var photoUri: Uri

    private val chatLines = mutableListOf<ChatLine>()

    @RequiresApi(Build.VERSION_CODES.O)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inner_chat, container, false)
        (activity as MainActivity).supportActionBar?.hide()

        chatLineDAO = ChatLineDAO()
        userDAO = UserDAO()
        chatLineRepository = ChatLineRepository(chatLineDAO)

        recyclerView = view.findViewById(R.id.recyclerViewChatInnerChat)
        txtChat = view.findViewById(R.id.txtChatInnerChat)
        btnSend = view.findViewById(R.id.btnSendInnerChat)
        btnAttachment = view.findViewById(R.id.btnAddAttachmentInnerChat)
        btnCamera = view.findViewById(R.id.btnCameraInnerChat)
        imgProfile = view.findViewById(R.id.imgProfileInnerChat)
        tvName = view.findViewById(R.id.tvNameInnerChat)
        btnBackInnerChat = view.findViewById(R.id.btnBackInnerChat)

        currentUserID = SaveSharedPreference.getUserID(requireContext())
        friendUserID = arguments?.getString("friendUserID") ?: ""
        chatID = arguments?.getString("chatID") ?: ""

        onResume()
        setupRecyclerView()
        fetchFriendProfile()
        fetchChatLines()
        setupTextWatcher()
        setupKeyboardListener(view)

        btnSend.setOnClickListener {
            val content = txtChat.text.toString().trim()
            if (content.isNotEmpty()) {
                sendMessage(content)
            }
        }

        btnAttachment.setOnClickListener { openAttachmentMenu() }
        btnCamera.setOnClickListener { checkCameraPermissionAndOpenCamera() }
        btnBackInnerChat.setOnClickListener {
            val transaction = activity?.supportFragmentManager?.beginTransaction()
            val fragment = OuterChat()
            transaction?.replace(R.id.fragmentContainerView, fragment)
            transaction?.addToBackStack(null)
            transaction?.commit()
        }



        return view

    }

    override fun onResume() {
        super.onResume()

        // 确保每次返回时隐藏 Toolbar
        (activity as MainActivity).supportActionBar?.hide()
    }
    private fun setupKeyboardListener(view: View) {
        val rootView = view.rootView
        val inputContainerInnerChat = view.findViewById<LinearLayout>(R.id.inputContainerInnerChat)
        val recyclerViewChatInnerChat = view.findViewById<RecyclerView>(R.id.recyclerViewChatInnerChat)

        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.height
            val keypadHeight = screenHeight - rect.bottom

            if (keypadHeight > screenHeight * 0.15) {
                // Keyboard is visible
                inputContainerInnerChat.translationY = -keypadHeight.toFloat()
                recyclerViewChatInnerChat.setPadding(0, 0, 0, keypadHeight)
            } else {
                // Keyboard is hidden
                inputContainerInnerChat.translationY = 0f
                recyclerViewChatInnerChat.setPadding(0, 0, 0, 0)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = InnerChatAdapter(currentUserID)
        recyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        recyclerView.adapter = adapter
    }

    private fun fetchFriendProfile() {
        lifecycleScope.launch {
            val user = userDAO.getUserByID(friendUserID)
            tvName.text = user?.username ?: "Unknown User"

            val profileRef = storageRef.child("imageProfile/$friendUserID.png")
            profileRef.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(this@InnerChat).load(uri).placeholder(R.drawable.nullprofile).into(imgProfile)
            }.addOnFailureListener {
                imgProfile.setImageResource(R.drawable.nullprofile)
            }
        }
    }

    private fun fetchChatLines() {
        lifecycleScope.launch {
            // Fetch existing chat lines
            chatLineDAO.getChatLines(chatID) { fetchedChatLines ->
                chatLines.clear()
                chatLines.addAll(fetchedChatLines)

                // Mark all unread messages as read
                chatLineDAO.markAllMessagesAsRead(chatID, currentUserID)

                adapter.setChatLines(chatLines)
                recyclerView.scrollToPosition(chatLines.size - 1)

                // Add a listener for new chat lines
                chatLineDAO.listenForNewChatLines(chatID) { newChatLine ->
                    lifecycleScope.launch {
                        chatLines.add(newChatLine)
                        adapter.setChatLines(chatLines)
                        recyclerView.scrollToPosition(chatLines.size - 1)
                    }
                }
            }
        }
    }

    private fun setupTextWatcher() {
        txtChat.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnSend.visibility = if (!s.isNullOrEmpty()) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendMessage(content: String) {
        lifecycleScope.launch {
            val chatLine = ChatLine(
                chatLineID = "CL${System.currentTimeMillis()}",
                chatID = chatID,
                content = content,
                mediaType = "text",
                mediaURL = null,
                dateTime = LocalDateTime.now().format(dateTimeFormatter),
                senderID = currentUserID,
                receiverID = friendUserID,
                isSharedPost = false
            )

            chatLineDAO.addChatLine(chatLine)
            chatLines.add(chatLine)
            adapter.setChatLines(chatLines)
            recyclerView.scrollToPosition(chatLines.size - 1)
            txtChat.text.clear()
        }
    }

    private fun openAttachmentMenu() {
        val popupMenu = PopupMenu(requireContext(), btnAttachment)
        popupMenu.menuInflater.inflate(R.menu.attachment_options, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.optionImage -> openGallery("image/*")
                R.id.optionVideo -> openGallery("video/*")
                R.id.optionFile -> openFilePicker()
            }
            true
        }
        popupMenu.show()
    }

    private fun openGallery(type: String) {
        val intent = Intent(Intent.ACTION_PICK).apply { this.type = type }
        startActivityForResult(intent, REQUEST_GALLERY)
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { this.type = "*/*" }
        startActivityForResult(intent, REQUEST_FILE)
    }

    private fun checkCameraPermissionAndOpenCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (requireContext().checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                requireContext().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CAMERA_PERMISSION
                )
            } else {
                openCamera()
            }
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        val photoFile = try {
            File.createTempFile("IMG_${System.currentTimeMillis()}", ".jpg", requireContext().cacheDir)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
        photoFile?.let {
            photoUri = FileProvider.getUriForFile(requireContext(), "com.example.fyp.provider", it)
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            }
            startActivityForResult(intent, REQUEST_CAMERA)
        }
    }

    private fun handleMediaUpload(fileUri: Uri, mediaType: String) {
        val storagePath = when (mediaType) {
            "image" -> "chatImages"
            "video" -> "chatVideos"
            "file" -> "chatFiles"
            else -> "chatMedia"
        }
        val fileRef = storageRef.child("$storagePath/${System.currentTimeMillis()}")

        fileRef.putFile(fileUri).addOnSuccessListener {
            fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                val chatLine = ChatLine(
                    chatLineID = "CL${System.currentTimeMillis()}",
                    chatID = chatID,
                    content = null,
                    mediaType = mediaType,
                    mediaURL = downloadUri.toString(),
                    dateTime = LocalDateTime.now().format(dateTimeFormatter),
                    senderID = currentUserID,
                    receiverID = friendUserID
                )
                lifecycleScope.launch {
                    chatLineDAO.addChatLine(chatLine)
                    chatLines.add(chatLine)
                    adapter.setChatLines(chatLines)
                    recyclerView.scrollToPosition(chatLines.size - 1)
                }
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to upload $mediaType", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_GALLERY -> {
                    val fileUri = data?.data
                    val mediaType = if (fileUri?.let { context?.contentResolver?.getType(it)?.startsWith("image") } == true) {
                        "image"
                    } else {
                        "video"
                    }
                    if (fileUri != null) handleMediaUpload(fileUri, mediaType)
                }
                REQUEST_FILE -> {
                    val fileUri = data?.data
                    if (fileUri != null) handleMediaUpload(fileUri, "file")
                }
                REQUEST_CAMERA -> handleMediaUpload(photoUri, "image")
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val REQUEST_GALLERY = 1
        private const val REQUEST_FILE = 2
        private const val REQUEST_CAMERA = 3
        private const val REQUEST_CAMERA_PERMISSION = 4
    }
}
