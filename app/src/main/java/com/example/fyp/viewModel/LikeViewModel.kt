package com.example.fyp.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fyp.data.Like
import com.example.fyp.repository.LikeRepository
import kotlinx.coroutines.launch

class LikeViewModel(private val likeRepository: LikeRepository) : ViewModel() {

    // Remove onComplete callback and use coroutines directly
    suspend fun saveLike(like: Like): Boolean {
        return try {
            likeRepository.saveLike(like)
            true
        } catch (e: Exception) {
            false
        }
    }

    // Same for updating like status
    suspend fun updateLikeStatus(like: Like): Boolean {
        return try {
            likeRepository.updateLikeStatus(like)
            true
        } catch (e: Exception) {
            false
        }
    }

    // Get like by userID and postID
    fun getLikeByUserIDAndPostID(userID: String, postID: String): LiveData<Like?> {
        val likeLiveData = MutableLiveData<Like?>()
        viewModelScope.launch {
            try {
                val like = likeRepository.getLikeByUserIDAndPostID(userID, postID)
                likeLiveData.postValue(like)
            } catch (e: Exception) {
                likeLiveData.postValue(null)
            }
        }
        return likeLiveData
    }

    suspend fun getLikeCountByPostID(postID: String): Int {
        return try {
            likeRepository.getLikeCountByPostID(postID)
        } catch (e: Exception) {
            0 // Return 0 if there's an error
        }
    }
}

