package com.example.ebook

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class CreateStoryActivity : AppCompatActivity() {
    private lateinit var storyTitle: EditText
    private lateinit var storyDescription: EditText
    private lateinit var createStoryButton: Button
    private lateinit var storyCategoryName: String
    private lateinit var coverImageView: ImageView
    private lateinit var selectCoverButton: Button
    private var coverImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_story)

        storyTitle = findViewById(R.id.storyTitle)
        storyDescription = findViewById(R.id.storyDescription)
        createStoryButton = findViewById(R.id.createStoryButton)
        val backButton = findViewById<ImageView>(R.id.ic_backDetail)
        storyCategoryName = ""
        val storyCategorySpinner = findViewById<Spinner>(R.id.storyCategoryDropdown)
        val storyCategories = arrayOf("Fiction", "Action", "Romantic", "Horror")
        val arrayAdapter = ArrayAdapter(this@CreateStoryActivity, android.R.layout.simple_spinner_dropdown_item, storyCategories)
        storyCategorySpinner.adapter = arrayAdapter

        storyCategorySpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                storyCategoryName = storyCategories[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        coverImageView = findViewById(R.id.coverImageView)
        selectCoverButton = findViewById(R.id.selectCoverButton)
        createStoryButton = findViewById(R.id.createStoryButton)
        selectCoverButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        }

        createStoryButton.setOnClickListener {
            val name = storyTitle.text.toString().trim()
            val description = storyDescription.text.toString().trim()
            val category = storyCategoryName.trim()
            if (coverImageUri != null) {
                uploadCoverImageAndCreateStory(name, description, category, coverImageUri!!)
            } else {
                Toast.makeText(this, "Please select a cover image", Toast.LENGTH_SHORT).show()
            }
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            coverImageUri = data.data
            coverImageView.setImageURI(coverImageUri)
        }
    }

    private fun uploadCoverImageAndCreateStory(name: String, description: String, category: String, imageUri: Uri) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val storyId = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("stories").push().key

        lateinit var authorName: String
        FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("user/$userId").addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                authorName = (snapshot.child("name").value as String?).toString()

            }

            override fun onCancelled(error: DatabaseError) {}
        })

        if (storyId != null && userId != null) {
            val storageRef = FirebaseStorage.getInstance().getReference("story_covers/$storyId.jpg")
            storageRef.putFile(imageUri).addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val coverUrl = uri.toString()
                    val story = NewStory(storyId, userId, name, description, category, coverUrl, authorName)
                    FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("stories/$storyId").setValue(story)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Story created successfully", Toast.LENGTH_SHORT).show()
                                sendEmailToFavorites()
                                // Navigate to Create First Page
                                val intent = Intent(this, CreatePageActivity::class.java)
                                intent.putExtra("STORY_ID", storyId)
                                intent.putExtra("IS_FIRST_PAGE", true)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this, "Failed to create story", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to upload cover image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendEmailToFavorites() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("user/$userId")
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val authorId = snapshot.key ?: return
                val authorName = snapshot.child("name").value.toString()
                val favouritesRef = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("user_favorites")
                favouritesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val userEmails = mutableListOf<String>()
                        for (userSnapshot in snapshot.children) {
                            for (authorsSnapshot in userSnapshot.children) {
                                for (authorSnapshot in authorsSnapshot.children) {
                                    if (authorSnapshot.key == authorId) {
                                        val userEmailRef =
                                            FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app")
                                                .getReference("user/${userSnapshot.key}/email")
                                        userEmailRef.addListenerForSingleValueEvent(object :
                                            ValueEventListener {
                                            override fun onDataChange(snapshot: DataSnapshot) {
                                                val email = snapshot.value.toString()
                                                userEmails.add(email)
                                                generateEmailSubject(email, authorName)
                                            }

                                            override fun onCancelled(error: DatabaseError) {}
                                        })
                                    }
                                }
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun generateEmailSubject(email: String, authorName: String) {
        val subject = "Update from your favorite author $authorName"
        val body = "Hello,\n\nYour favorite author $authorName has upload a new story. Check it out now!"
            sendEmail(email, subject, body)
        Toast.makeText(this, "Emails sent to all favorite readers", Toast.LENGTH_SHORT).show()
    }
    private fun sendEmail(recipient: String, subject: String, body: String) {
        val emailSender = EmailSender()
        emailSender.sendEmail(recipient, subject, body)
    }
}

data class NewStory(
    val id: String = "",
    val authorId: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val coverUrl: String = "",
    val authorName: String = "",
    val pages: Map<String, NewPage> = emptyMap()
)