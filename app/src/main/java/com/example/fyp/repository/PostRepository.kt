package com.example.fyp.repository

import com.example.fyp.dao.PostCategoryDAO
import com.example.fyp.dao.PostCommentDAO
import com.example.fyp.dao.PostDAO
import com.example.fyp.dao.PostImageDAO
import com.example.fyp.data.Post

class PostRepository(private val postDao : PostDAO) {

    fun addPost(post: Post, onComplete: (String?, Exception?) -> Unit) {
        postDao.addPost(post) { postID, exception ->
            if (exception == null) {
                // Post was successfully added
                onComplete(postID, null)
            } else {
                // Handle error
                onComplete(null, exception)
            }
        }
    }

    suspend fun getPostByUser(userID : String) : List<Post> {
        return postDao.getPostByUser(userID)
    }

    suspend fun searchPost(searchText : String) : List<Post> {
        return postDao.searchPost(searchText)
    }

    suspend fun getAllPosts(): List<Post> {
        return postDao.getAllPost()
    }


    fun deletePost(postID: String, onComplete: (Boolean, Exception?) -> Unit) {
        postDao.deletePost(postID, onComplete)
    }

    fun deletePostWithAssociations(
        postID: String,
        postImageDAO: PostImageDAO,
        postCategoryDAO: PostCategoryDAO,
        postCommentDAO: PostCommentDAO,
        onComplete: (Boolean, Exception?) -> Unit
    ) {
        postDao.deletePostWithAssociations(postID, postImageDAO, postCategoryDAO, postCommentDAO, onComplete)
    }


    suspend fun getPostByID(postID: String): Post? {
        return postDao.getPostByID(postID)
    }

    suspend fun getPostsForUser(currentUserID: String): List<Post> {
        val allPosts = postDao.getAllPost()  // Fetch all posts
        val visiblePosts = mutableListOf<Post>()

        for (post in allPosts) {
            when (post.postType) {
                "Public" -> {
                    // Public posts are visible to everyone
                    visiblePosts.add(post)
                }
                "Private" -> {
                    // Private posts are visible only to the user who created it
                    if (post.userID == currentUserID) {
                        visiblePosts.add(post)
                    }
                }
                "Restricted" -> {
                    // Restricted posts require checking the PostShared table
                    val hasAccess = postDao.checkIfUserHasAccessToPost(currentUserID, post.postID)
                    if (hasAccess) {
                        visiblePosts.add(post)
                    }
                }
            }
        }

        return visiblePosts
    }

    suspend fun checkIfUserHasAccessToPost(userID: String, postID: String): Boolean {
        return postDao.checkIfUserHasAccessToPost(userID, postID)
    }

}