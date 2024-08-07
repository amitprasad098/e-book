package com.example.ebook

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FavoritesFragment : Fragment() {
    private lateinit var favoritesLayout: LinearLayout
    private lateinit var database: FirebaseDatabase
    private lateinit var editTextSearch: EditText
    private var allAuthors: MutableList<Author> = mutableListOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_favorites, container, false)
        favoritesLayout = view.findViewById(R.id.rv_favorites)
        editTextSearch = view.findViewById(R.id.et_searchFavorites)
        database = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app")

        loadFavorites()
        setupSearchFilter()

        return view
    }

    private fun loadFavorites() {
        val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserID == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val favoritesRef = database.getReference("user_favorites/$currentUserID/authors")
        favoritesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allAuthors.clear()
                snapshot.children.forEach { authorSnapshot ->
                    val authorName = authorSnapshot.child("authorName").getValue(String::class.java)
                    val authorId = authorSnapshot.key  // Assuming the authorId is the key
                    if (authorName != null && authorId != null) {
                        allAuthors.add(Author(id = authorId, name = authorName))
                    }
                }
                filterAuthors("")  // Show all authors initially
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load favorites: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupSearchFilter() {
        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterAuthors(s.toString())
            }
        })
    }

    private fun filterAuthors(filter: String) {
        favoritesLayout.removeAllViews()
        val filteredAuthors = allAuthors.filter { it.name.contains(filter, ignoreCase = true) }
        filteredAuthors.forEach { author -> addAuthorView(author) }
    }

    private fun addAuthorView(author: Author) {
        if (!isAdded) {
            // Fragment not attached, exit the method to avoid using a null context
            return
        }
        val textView = TextView(context).apply {
            text = "Author: ${author.name}"
            textSize = 16f
            setPadding(8, 8, 8, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                val intent = Intent(context, AuthorDetailActivity::class.java).apply {
                    putExtra("authorId", author.id) // Pass the author's ID to the detail activity
                }
                startActivity(intent)
            }
        }
        favoritesLayout.addView(textView)
    }
}
