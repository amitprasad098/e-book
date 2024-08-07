package com.example.ebook

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AddPagesActivity : AppCompatActivity() {
    private lateinit var storyPagesLabel: TextView
    private lateinit var pagesListView: ListView
    private lateinit var storyId: String
    private lateinit var pagesAdapter: NewPageAdapter
    private val pages = mutableListOf<NewPage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_pages)

        storyId = intent.getStringExtra("STORY_ID") ?: ""

        storyPagesLabel = findViewById(R.id.storyPagesLabel)
        pagesListView = findViewById(R.id.pagesListView)
        val backButton = findViewById<ImageView>(R.id.ic_backDetail)

        pagesAdapter = NewPageAdapter(this, pages)
        pagesListView.adapter = pagesAdapter

        pagesListView.setOnItemClickListener { parent, view, position, id ->
            val selectedPage = pages[position]
            val intent = Intent(this, CreatePageActivity::class.java)
            intent.putExtra("STORY_ID", storyId)
            intent.putExtra("PAGE_ID", selectedPage.id)
            startActivity(intent)
        }

        // Load and display pages for this story
        loadPages()

        backButton.setOnClickListener {
            val intent = Intent(this, AuthorHomeActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadPages() {
        val pagesRef = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("stories/$storyId/pages")
        pagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                pages.clear()
                for (dataSnapshot in snapshot.children) {
                    val page = dataSnapshot.getValue(NewPage::class.java)
                    if (page != null) {
                        pages.add(page)
                    }
                }
                pagesAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }
}
