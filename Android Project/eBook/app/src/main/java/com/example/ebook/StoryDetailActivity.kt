package com.example.ebook

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide  // Ensure you have Glide imported
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class StoryDetailActivity : AppCompatActivity() {

    private lateinit var storyNameTextView: TextView
    private lateinit var storyDescriptionTextView: TextView
    private lateinit var storyAuthorTextView: TextView
    private lateinit var storyCoverImageView: ImageView  // Add this for the ImageView
    private var authorId: String? = null

    private var storeId: String? = null

    private lateinit var coverUrl: String


    private lateinit var collection: String
    private lateinit var database: FirebaseDatabase
    private lateinit var userLibraryRef: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story_detail)

        val backButton = findViewById<ImageView>(R.id.ic_backDetail)
        storyNameTextView = findViewById(R.id.tv_storyTitle)
        storyDescriptionTextView = findViewById(R.id.tv_storyDescription)
        storyAuthorTextView = findViewById(R.id.tv_storyAuthor)
        storyCoverImageView = findViewById(R.id.img_storyCoverpage)  // Reference to the ImageView
        val storyReadButton = findViewById<Button>(R.id.btn_startRead)

        val addlibrary = findViewById<ImageView>(R.id.ic_addLibrary_new)

        // Retrieve story and author details from intent
        val storyName = intent.getStringExtra("storyName")
        val storyAuthor = intent.getStringExtra("storyAuthor")
        val storyDescription = intent.getStringExtra("storyDescription")
         coverUrl = intent.getStringExtra("coverUrl")!!  // Get the cover URL

        authorId = intent.getStringExtra("authorId")

        // Set the text views with the story details
        storyNameTextView.text = storyName
        storyDescriptionTextView.text = storyDescription
        storyAuthorTextView.text = storyAuthor

        // Load the cover image using Glide
        Glide.with(this)
            .load(coverUrl)
            .into(storyCoverImageView)
        addlibrary.setOnClickListener {
            Log.i("TAG", "addToLibrary: "+collection)
            addToLibrary(storyName, storyDescription)
        }
        backButton.setOnClickListener {
            finish()
        }

        storyAuthorTextView.setOnClickListener {
            val intent = Intent(this, AuthorDetailActivity::class.java)
            intent.putExtra("authorId", authorId) // Pass the author ID to the detail activity
            startActivity(intent)
        }

        storyReadButton.setOnClickListener {
            val intent = Intent(this, ReadStoryActivity::class.java)
            intent.putExtra("STORY_NAME", storyName)
            startActivity(intent)
        }
        database = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app")

        val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
        collection="0"
        userLibraryRef = database.getReference("user_library/$currentUserID")
        userLibraryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (storySnapshot in snapshot.children) {
                    val story = storySnapshot.getValue(Story::class.java)
                    if (story != null) {
                        Log.i("TAG", "onDataChangeqq:== "+story.id+"==="+story.description)
                        if(story.name.equals(storyName)){
                            collection="1";
                            storeId=story.id
                        }

                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun addToLibrary(storyName: String?, storyDescription: String?) {
        val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserID != null) {

            if(collection=="1"){

                userLibraryRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (storySnapshot in snapshot.children) {
                            val story = storySnapshot.getValue(Story::class.java)
                            if (story != null) {
                                if(story.name.equals(storyName)){
                                    storySnapshot.getRef().removeValue();
                                    collection="0";
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })

                Toast.makeText(this, "Story delete to library", Toast.LENGTH_SHORT).show()

            }else{
                val userLibraryRef = database.reference.child("user_library/$currentUserID").push()

                val story = Story(storeId?:"", authorId?:"",
                    storyName?:"", storyDescription?:"","",coverUrl)

                userLibraryRef.setValue(story).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.i("TAG", "addToLibrary: 33"+currentUserID)

                        Toast.makeText(this, "Story added to library", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.i("TAG", "addToLibrary: 222"+currentUserID)

                        Toast.makeText(this, "Failed to add story to library", Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }
    }
}
