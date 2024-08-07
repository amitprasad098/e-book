package com.example.ebook

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AuthorDetailActivity : AppCompatActivity() {

    private lateinit var tvAuthorName: TextView
    private lateinit var tvAuthorEmail: TextView
    private lateinit var tvAuthorBio: TextView
    private lateinit var llStoriesContainer: LinearLayout
    private lateinit var addToFavoritesButton: ImageView
    private lateinit var btnBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_author_detail)

        tvAuthorName = findViewById(R.id.tvAuthorName)
        tvAuthorEmail = findViewById(R.id.tvAuthorEmail)
        tvAuthorBio = findViewById(R.id.tvAuthorBio)
        llStoriesContainer = findViewById(R.id.llStoriesContainer)
        addToFavoritesButton = findViewById(R.id.ic_addFavorite)
        btnBack = findViewById(R.id.ic_backAuthorDetail)

        val authorId = intent.getStringExtra("authorId") ?: return

        loadAuthorDetails(authorId)
        loadAuthorStories(authorId)

        addToFavoritesButton.setOnClickListener {
            addToFavorites(authorId)
        }

        btnBack.setOnClickListener{
            finish()
        }
    }

    private fun loadAuthorDetails(authorId: String) {
        val database = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app")
        val authorRef = database.getReference("author/$authorId")

        authorRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val name = dataSnapshot.child("name").getValue(String::class.java)
                val email = dataSnapshot.child("email").getValue(String::class.java)
                val bio = dataSnapshot.child("bio").getValue(String::class.java)

                tvAuthorName.text = name ?: "Unknown"
                tvAuthorEmail.text = email ?: "No email provided"
                tvAuthorBio.text = bio ?: "No biography available"
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(applicationContext, "Failed to load author details", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun loadAuthorStories(authorId: String) {
        val database = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app")
        val storiesRef = database.getReference("stories").orderByChild("authorId").equalTo(authorId)

        storiesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                llStoriesContainer.removeAllViews() // Clear existing views
                for (storySnapshot in dataSnapshot.children) {
                    val story = storySnapshot.getValue(Story::class.java)
                    if (story != null) {
                        val storyView = createStoryView(story)
                        llStoriesContainer.addView(storyView)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(applicationContext, "Failed to load stories", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun createStoryView(story: Story): View {
        val storyView = layoutInflater.inflate(R.layout.story_item_layout, llStoriesContainer, false)
        val imageView = storyView.findViewById<ImageView>(R.id.story_cover_image)
        val nameView = storyView.findViewById<TextView>(R.id.story_name)
        val categoryView = storyView.findViewById<TextView>(R.id.story_category)

        nameView.text = story.name
        categoryView.text = story.category
        Glide.with(this).load(story.coverUrl).into(imageView)

        storyView.setOnClickListener {
            val intent = Intent(this, StoryDetailActivity::class.java).apply {
                putExtra("storyId", story.id)
                putExtra("coverUrl", story.coverUrl)
                putExtra("storyName", story.name)
                putExtra("storyAuthor", story.authorName)
                putExtra("storyDescription", story.description)
                putExtra("authorId", story.authorId)
            }
            startActivity(intent)
        }
        return storyView
    }

    private fun addToFavorites(authorId: String) {
        val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserID == null) {
            Toast.makeText(this, "You must be logged in to add favorites.", Toast.LENGTH_SHORT).show()
            return
        }

        val database = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app")
        val userLibraryRef = database.getReference("user_favorites/$currentUserID/authors/$authorId")

        userLibraryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    userLibraryRef.removeValue().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this@AuthorDetailActivity, "Author removed from favorites", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@AuthorDetailActivity, "Failed to remove author from favorites", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    val authorData = HashMap<String, Any>()
                    authorData["authorId"] = authorId
                    authorData["authorName"] = tvAuthorName.text.toString()
                    userLibraryRef.setValue(authorData).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this@AuthorDetailActivity, "Author added to favorites", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@AuthorDetailActivity, "Failed to add author to favorites", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AuthorDetailActivity, "Error accessing the database: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
