package com.example.ebook

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AuthorHomeFragment : Fragment() {

    private lateinit var database: FirebaseDatabase
    private lateinit var storiesRef: DatabaseReference
    private lateinit var currentUser: FirebaseAuth

    private lateinit var storiesLayout: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_author_home, container, false)
        // Inflate the layout for this fragment

        database = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app")
        storiesRef = database.getReference("stories")
        currentUser = FirebaseAuth.getInstance()

        storiesLayout = view.findViewById(R.id.stories_layout)

        // Get the current user's authorId
        val authorId = currentUser.currentUser?.uid

        // Query stories where authorId matches the logged-in user's authorId
        val query = storiesRef.orderByChild("authorId").equalTo(authorId)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Clear the existing stories
                storiesLayout.removeAllViews()

                // Iterate through each story and display it
                for (storySnapshot in dataSnapshot.children) {
                    val storyId = storySnapshot.key // Get the story ID
                    val name = storySnapshot.child("name").getValue(String::class.java) ?: ""
                    val category =
                        storySnapshot.child("category").getValue(String::class.java) ?: ""
                    val description =
                        storySnapshot.child("description").getValue(String::class.java) ?: ""
                    val coverUrl =
                        storySnapshot.child("coverUrl").getValue(String::class.java) ?: ""

                    if (isAdded) {
                    // Create a LinearLayout to hold the cover image and story details
                    val storyLayout = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, 0, 32)
                        }
                    }

                    // Create an ImageView to display the cover photo
                    val coverImageView = ImageView(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            200,
                            300
                        ).apply {
                            setMargins(0, 0, 16, 0)
                        }
                    }
                    Glide.with(this@AuthorHomeFragment).load(coverUrl).into(coverImageView)
                    storyLayout.addView(coverImageView)

                    // Create a TextView to display the story details
                    val storyTextView = TextView(requireContext()).apply {
                        text = "Name: $name\nCategory: $category\nDescription: $description\n"
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            weight = 1f
                        }
                    }
                    storyLayout.addView(storyTextView)

                    storyLayout.setOnClickListener {
                        val intent = Intent(requireContext(), AddPagesActivity::class.java).apply {
                            putExtra("STORY_ID", storyId)
                        }
                        startActivity(intent)
                    }

                    // Add the story layout to the main stories layout
                    storiesLayout.addView(storyLayout)
                }
            }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle database error
                // TODO: Handle database error appropriately
            }
        })

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val createStoryButton = view.findViewById<Button>(R.id.createStoryBtn)

        createStoryButton.setOnClickListener {
            val intent = Intent(requireContext(), CreateStoryActivity::class.java)
            startActivity(intent)
        }
    }
}
