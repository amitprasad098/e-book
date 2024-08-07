package com.example.ebook

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CreatePageActivity : AppCompatActivity() {
    private lateinit var pageText: EditText
    private lateinit var choicesLayout: LinearLayout
    private lateinit var addChoiceButton: Button
    private lateinit var createPageButton: Button

    private lateinit var storyId: String
    private var pageId: String? = null
    private var isFirstPage: Boolean = false
    private val choices = mutableListOf<Pair<EditText, String>>()
    private val existingChoices = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_page)

        pageText = findViewById(R.id.pageText)
        choicesLayout = findViewById(R.id.choicesLayout)
        addChoiceButton = findViewById(R.id.addChoiceButton)
        createPageButton = findViewById(R.id.createPageButton)

        storyId = intent.getStringExtra("STORY_ID") ?: ""
        pageId = intent.getStringExtra("PAGE_ID")
        isFirstPage = intent.getBooleanExtra("IS_FIRST_PAGE", false)
        val backButton = findViewById<ImageView>(R.id.ic_backDetail)

        addChoiceButton.setOnClickListener { addChoiceField() }
        createPageButton.setOnClickListener { createOrUpdatePage() }

        // If editing an existing page, load its data
        pageId?.let { loadPageData(it) }

        backButton.setOnClickListener {
            val intent = Intent(this, AddPagesActivity::class.java)
            intent.putExtra("STORY_ID", storyId)
            startActivity(intent)
        }
    }

    private fun addChoiceField(choiceText: String = "", nextPageId: String = "") {
        val choiceTextField = EditText(this)
        choiceTextField.hint = "Choice Text"
        choiceTextField.setText(choiceText)
        choicesLayout.addView(choiceTextField)
        choices.add(Pair(choiceTextField, nextPageId))
    }

    private fun createOrUpdatePage() {
        val text = pageText.text.toString().trim()
        val choicesMap = mutableMapOf<String, String>()
        val updatedChoices = mutableMapOf<String, String>()

        for (choice in choices) {
            val choiceText = choice.first.text.toString().trim()
            val nextPageId = choice.second

            if (choiceText.isNotEmpty()) {
                if (nextPageId.isEmpty()) {
                    // Create a new page for new choices
                    val newNextPageId = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("stories/$storyId/pages").push().key
                    choicesMap[choiceText] = newNextPageId ?: ""

                    // Create a new page with the choice text as its content
                    val newPage = NewPage(newNextPageId!!, choiceText, emptyMap(), isFirstPage = false)
                    FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("stories/$storyId/pages/$newNextPageId").setValue(newPage)
                } else {
                    // Use the existing page ID for existing choices
                    choicesMap[choiceText] = nextPageId
                    updatedChoices[choiceText] = nextPageId

                    // Update the text in the linked choice page
                    val choicePageRef = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("stories/$storyId/pages/$nextPageId")
                    choicePageRef.child("text").setValue(choiceText)
                }
            }
        }

        // Update existing choices that have changed
        for ((oldChoiceText, oldNextPageId) in existingChoices) {
            if (!updatedChoices.containsValue(oldNextPageId)) {
                // Find the new choice text for the existing nextPageId
                val newChoiceText = choicesMap.filterValues { it == oldNextPageId }.keys.firstOrNull()
                if (newChoiceText != null) {
                    // Update the choice text in the database
                    val choiceUpdateRef = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("stories/$storyId/pages/$pageId/choices/$oldChoiceText")
                    choiceUpdateRef.removeValue().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val newChoiceUpdateRef = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("stories/$storyId/pages/$pageId/choices/$newChoiceText")
                            newChoiceUpdateRef.setValue(oldNextPageId)

                            // Also update the text in the linked choice page
                            val choicePageRef = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("stories/$storyId/pages/$oldNextPageId")
                            choicePageRef.child("text").setValue(newChoiceText)
                        }
                    }
                }
            }
        }

        if (pageId == null) {
            // Creating a new page
            pageId = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("stories/$storyId/pages").push().key
        }

        val page = NewPage(pageId!!, text, choicesMap, isFirstPage)
        FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("stories/$storyId/pages/$pageId").setValue(page)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Page created/updated successfully", Toast.LENGTH_SHORT).show()
                    // Optionally navigate back to AddPagesActivity or stay on this page for further edits
                    val intent = Intent(this, AddPagesActivity::class.java)
                    intent.putExtra("STORY_ID", storyId)
                    intent.putExtra("IS_FIRST_PAGE", true)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to create/update page", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loadPageData(pageId: String) {
        val pageRef = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("stories/$storyId/pages/$pageId")
        pageRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val page = snapshot.getValue(NewPage::class.java)
                if (page != null) {
                    pageText.setText(page.text)
                    isFirstPage = page.isFirstPage // Retain the isFirstPage value
                    choices.clear()
                    choicesLayout.removeAllViews()
                    for ((choiceText, nextPageId) in page.choices) {
                        existingChoices[choiceText] = nextPageId
                        addChoiceField(choiceText, nextPageId)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }
}
