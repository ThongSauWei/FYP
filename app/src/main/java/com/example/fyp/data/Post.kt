package com.example.fyp.data

import android.os.Parcel
import android.os.Parcelable

data class Post(
    var postID: String,
    val postDateTime: String,
    val postDescription: String,
    val postTitle: String,
    val userID: String,
    val active: Int,
    val postType: String
) : Parcelable {
    constructor() : this("", "", "", "", "", 0, "")

    // Write data to the parcel
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(postID)
        parcel.writeString(postDateTime)
        parcel.writeString(postDescription)
        parcel.writeString(postTitle)
        parcel.writeString(userID)
        parcel.writeInt(active)
        parcel.writeString(postType)
    }

    // Describe the contents (usually just return 0)
    override fun describeContents(): Int {
        return 0
    }

    // Creator for creating Post from Parcel
    companion object CREATOR : Parcelable.Creator<Post> {
        override fun createFromParcel(parcel: Parcel): Post {
            return Post(
                postID = parcel.readString() ?: "",
                postDateTime = parcel.readString() ?: "",
                postDescription = parcel.readString() ?: "",
                postTitle = parcel.readString() ?: "",
                userID = parcel.readString() ?: "",
                active = parcel.readInt(),
                postType = parcel.readString() ?: ""
            )
        }

        override fun newArray(size: Int): Array<Post?> {
            return arrayOfNulls(size)
        }
    }
}
