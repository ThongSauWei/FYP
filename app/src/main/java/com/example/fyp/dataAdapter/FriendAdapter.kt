package com.example.fyp.dataAdapter

import android.app.ActionBar
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fyp.FriendProfile
import com.example.fyp.InnerChat
import com.example.fyp.R
import com.example.fyp.data.Chat
import com.example.fyp.data.ChatLine
import com.example.fyp.data.Friend
import com.example.fyp.data.Profile
import com.example.fyp.data.User
import com.example.fyp.dialog.DeleteFriendDialog
import com.example.fyp.viewModel.FriendViewModel
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.mainapp.finalyearproject.saveSharedPreference.SaveSharedPreference
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FriendAdapter(val mode: Int) : RecyclerView.Adapter<FriendAdapter.FriendHolder>() {

    private lateinit var storageRef: StorageReference
    private lateinit var fragmentManager: FragmentManager
    private lateinit var deleteFriendDialog: DeleteFriendDialog
    private lateinit var friendViewModel: FriendViewModel

    private var friendList = emptyList<Friend>()
    private var userList = emptyList<User>()
    private var profileList = emptyList<Profile>()
    private var chatList = emptyList<Chat>()
    private var lastChatList = emptyList<ChatLine>()
    private var unseenMsgList = emptyList<Int>()

    private lateinit var currentUserID: String

    // for invite friend's item click
    private var onItemClickListener: ((User, String, ImageView) -> Unit)? = null

    class FriendHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val constraintLayout: ConstraintLayout = itemView.findViewById(R.id.constraintLayoutFriendHolder)
        val constraintSet = ConstraintSet()

        val imgProfile: ImageView = itemView.findViewById(R.id.imgProfileFriendHolder)
        val tvName: TextView = itemView.findViewById(R.id.tvNameFriendHolder)
        val tvText: TextView = itemView.findViewById(R.id.tvTextFriendHolder)
        val dynamicContainer: CardView = itemView.findViewById(R.id.dynamicFriendHolder)

        // for delete & invite & add
        val imgContent = ImageView(itemView.context)

        // for chat
        val time = TextView(itemView.context)
        val notification = TextView(itemView.context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.friend_holder, parent, false)

        storageRef = FirebaseStorage.getInstance().getReference()
        currentUserID = SaveSharedPreference.getUserID(parent.context)

        return FriendHolder(itemView)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: FriendHolder, position: Int) {
        val currentUser = userList[position]

        // get the image of the friend
        val ref = storageRef.child("imageProfile").child(currentUser.userID + ".png")
        ref.downloadUrl
            .addOnCompleteListener {
                Glide.with(holder.imgProfile).load(it.result.toString()).into(holder.imgProfile)
            }

        holder.tvName.text = currentUser.username

        // convert dp to px
        val density = holder.dynamicContainer.context.resources.displayMetrics.density

        // get the default layout settings of the cardview
        val layoutParamsCardView = holder.dynamicContainer.layoutParams

        when (mode) {
            Mode.CHAT -> {
                val lastChat = lastChatList[position]
                holder.tvText.text = lastChat.content

                val dateTime = parseDateTime(lastChat.dateTime)
                val duration = System.currentTimeMillis() - dateTime.time
                val timeText: String = when {
                    duration >= 2 * 24 * 60 * 60 * 1000 -> "${duration / (24 * 60 * 60 * 1000)} days ago"
                    duration >= 1 * 24 * 60 * 60 * 1000 -> "Yesterday"
                    else -> SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(dateTime)
                }

                // Initialise the TextView for time
                holder.time.id = View.generateViewId()
                holder.time.text = timeText
                holder.time.typeface = ResourcesCompat.getFont(holder.time.context, R.font.abhayalibre_semibold)
                holder.time.textSize = 14f

                // Add the TextView into the layout
                holder.constraintLayout.addView(holder.time)

                // Clone the applied constraints to the constraintSet
                holder.constraintSet.clone(holder.constraintLayout)

                // Apply new constraints to the TextView time
                holder.constraintSet.connect(
                    holder.time.id,
                    ConstraintSet.BOTTOM,
                    holder.dynamicContainer.id,
                    ConstraintSet.TOP,
                    5
                )

                holder.constraintSet.connect(
                    holder.time.id,
                    ConstraintSet.START,
                    holder.dynamicContainer.id,
                    ConstraintSet.START
                )

                holder.constraintSet.connect(
                    holder.time.id,
                    ConstraintSet.END,
                    holder.dynamicContainer.id,
                    ConstraintSet.END
                )

                // Apply the constraints to the layout
                holder.constraintSet.applyTo(holder.constraintLayout)

                holder.constraintLayout.setOnClickListener {
                    val transaction = fragmentManager.fragments.get(0).activity?.supportFragmentManager?.beginTransaction()
                    val fragment = InnerChat()

                    val bundle = Bundle()
                    bundle.putString("chatID", lastChat.chatID)
                    bundle.putString("friendUserID", currentUser.userID)
                    fragment.arguments = bundle

                    transaction?.replace(R.id.fragmentContainerView, fragment)
                    transaction?.addToBackStack(null)
                    transaction?.commit()
                }
            }
            Mode.DELETE -> {
                val currentProfile = profileList[position]
                holder.tvText.text = currentProfile.userCourse

                // Setup dynamic container for DELETE mode
                holder.dynamicContainer.layoutParams.width = (30 * holder.dynamicContainer.context.resources.displayMetrics.density).toInt()
                holder.dynamicContainer.layoutParams.height = (30 * holder.dynamicContainer.context.resources.displayMetrics.density).toInt()
                holder.dynamicContainer.cardElevation = 5f
                holder.dynamicContainer.radius = 15f

                holder.imgContent.setImageResource(R.drawable.remove_friend_red)
                holder.dynamicContainer.removeAllViews()
                holder.dynamicContainer.addView(holder.imgContent)

                holder.imgContent.setOnClickListener {
                    val friendID = friendList[position].friendID
                    deleteFriendDialog.friendID = friendID
                    deleteFriendDialog.username = currentUser.username
                    deleteFriendDialog.viewModel = friendViewModel
                    deleteFriendDialog.show(fragmentManager, "DeleteFriendDialog")
                }

                holder.constraintLayout.setOnClickListener {
                    val transaction = fragmentManager.fragments[0].activity?.supportFragmentManager?.beginTransaction()
                    val fragment = FriendProfile()
                    val bundle = Bundle()
                    bundle.putString("friendUserID", currentUser.userID)
                    fragment.arguments = bundle
                    transaction?.replace(R.id.fragmentContainerView, fragment)
                    transaction?.addToBackStack(null)
                    transaction?.commit()
                }
            }
            else -> {
                // Handle other modes
            }
        }
    }

    fun setDeleteFriendDialog(deleteFriendDialog : DeleteFriendDialog) {
        this.deleteFriendDialog = deleteFriendDialog

        notifyDataSetChanged()
    }

    private fun parseDateTime(dateTimeString: String): Date {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formatter.parse(dateTimeString) ?: Date()
    }

    fun setUserList(userList: List<User>) {
        this.userList = userList
        notifyDataSetChanged()
    }

    fun setProfileList(profileList: List<Profile>) {
        this.profileList = profileList
        notifyDataSetChanged()
    }

    fun setFriendList(friendList: List<Friend>) {
        this.friendList = friendList
        notifyDataSetChanged()
    }

    fun setViewModel(friendViewModel: FriendViewModel) {
        this.friendViewModel = friendViewModel
        notifyDataSetChanged()
    }

    fun setFragmentManager(fragmentManager: FragmentManager) {
        this.fragmentManager = fragmentManager
        notifyDataSetChanged()
    }

    // for invite friend's item click
    fun setOnItemClickListener(listener: (User, String, ImageView) -> Unit) {
        onItemClickListener = listener
    }

    object Mode {
        const val ADD = 1
        const val DELETE = 2
        const val CHAT = 3
    }
}
