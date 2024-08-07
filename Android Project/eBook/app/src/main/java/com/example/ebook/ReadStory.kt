package com.example.ebook

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.Stack

class ReadStoryActivity : AppCompatActivity() {
    private lateinit var pageTextView: TextView
    private lateinit var choicesLayout: LinearLayout
    private lateinit var backButton: Button
    private lateinit var backToFirstPageButton: Button
    private lateinit var exitButton: Button

    private lateinit var storyId: String
    private var currentPageId: String? = null
    private val readerPath = mutableListOf<String>()
    private val pageStack = Stack<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_story)

        pageTextView = findViewById(R.id.pageTextView)
        choicesLayout = findViewById(R.id.choicesLayout)
        backButton = findViewById(R.id.backButton)
        backToFirstPageButton = findViewById(R.id.backToFirstPageButton)
        exitButton = findViewById(R.id.exitButton)

        val storyName = intent.getStringExtra("STORY_NAME") ?: ""
        extractStoryId(storyName)

        backButton.setOnClickListener { goBack() }
        backToFirstPageButton.setOnClickListener { resetToFirstPage() }
        exitButton.setOnClickListener { exitStory() }
    }

    private fun extractStoryId(storyName: String) {
        val storiesRef = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("stories")
        storiesRef.orderByChild("name").equalTo(storyName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (dataSnapshot in snapshot.children) {
                        val story = dataSnapshot.getValue(NewStory::class.java)
                        if (story != null) {
                            storyId = story.id
                            logStoryStart()  // Log story start when the story is identified
                            loadReaderState()
                            break
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun loadReaderState() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val readerStateRef = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("users/$userId/library/$storyId")
        readerStateRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val state = snapshot.value as Map<String, Any>
                    currentPageId = state["currentPageId"] as? String
                    val path = state["path"] as? List<String>
                    if (path != null) {
                        readerPath.clear()
                        readerPath.addAll(path)
                    }
                    // Rebuild the pageStack
                    rebuildPageStack()
                    if (currentPageId != null) {
                        loadPage(currentPageId!!, isNavigatingForward = false) // Prevent adding current page to path
                    } else {
                        loadFirstPage()
                    }
                } else {
                    loadFirstPage()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun rebuildPageStack() {
        pageStack.clear()
        // Rebuild the stack based on the current path
        if (readerPath.isNotEmpty()) {
            val pagesRef = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("stories/$storyId/pages")
            for (pageText in readerPath) {
                pagesRef.orderByChild("text").equalTo(pageText)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (dataSnapshot in snapshot.children) {
                                val page = dataSnapshot.getValue(NewPage::class.java)
                                if (page != null && !pageStack.contains(page.id)) {
                                    pageStack.push(page.id)
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle error
                        }
                    })
            }
        }
    }

    private fun loadFirstPage() {
        val firstPageRef = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("stories/$storyId/pages").orderByChild("firstPage").equalTo(true)
        firstPageRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dataSnapshot in snapshot.children) {
                    val firstPage = dataSnapshot.getValue(NewPage::class.java)
                    if (firstPage != null) {
                        currentPageId = firstPage.id
                        displayPage(firstPage, isNavigatingForward = true)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun displayPage(page: NewPage, isNavigatingForward: Boolean) {
        pageTextView.text = page.text
        choicesLayout.removeAllViews()

        // Add the current page text to the path only if navigating forward and not already in the path
        if (isNavigatingForward && (readerPath.isEmpty() || readerPath.last() != page.text)) {
            readerPath.add(page.text)
            pageStack.push(page.id)  // Push to stack only when navigating forward
        }

        // Log page visit
        logPageVisit(page.id)
        logCurrentPath()

        // Hide back button on first page
        backButton.visibility = if (pageStack.size > 1) View.VISIBLE else View.GONE

        if (page.choices.isEmpty()) {
            // End of the story
            displayPath()
            backToFirstPageButton.visibility = View.VISIBLE
            logStoryCompletion()  // Log story completion
            logReaderPath()       // Log reader path
        } else {
            backToFirstPageButton.visibility = View.GONE
            for ((choiceText, nextPageId) in page.choices) {
                val choiceButton = Button(this)
                choiceButton.text = choiceText
                choiceButton.setOnClickListener {
                    logChoiceMade(choiceText)  // Log choice made
                    loadPage(nextPageId)
                }
                choicesLayout.addView(choiceButton)
            }
        }
    }

    private fun loadPage(pageId: String, isNavigatingForward: Boolean = true) {
        val pageRef = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("stories/$storyId/pages/$pageId")
        pageRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val page = snapshot.getValue(NewPage::class.java)
                if (page != null) {
                    currentPageId = page.id
                    displayPage(page, isNavigatingForward)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun goBack() {
        if (pageStack.size > 1) {
            // Pop the current page
            pageStack.pop()
            // Get the previous page
            val previousPageId = pageStack.peek()
            // Remove the last entry from the path
            if (readerPath.isNotEmpty()) {
                readerPath.removeAt(readerPath.size - 1)
            }
            loadPage(previousPageId, isNavigatingForward = false)  // Set isNavigatingForward to false
        }
    }

    private fun resetToFirstPage() {
        readerPath.clear()
        pageStack.clear()
        logCurrentPath()
        loadFirstPage()
    }

    private fun exitStory() {
        // Handle exiting the story (e.g., go back to the main activity)
        saveReaderState()
        finish()
    }

    override fun onPause() {
        super.onPause()
        saveReaderState()
    }

    override fun onDestroy() {
        super.onDestroy()
        saveReaderState()
    }

    private fun saveReaderState() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val readerStateRef = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("users/$userId/library/$storyId")
        val readerState = mapOf(
            "currentPageId" to currentPageId,
            "path" to readerPath
        )
        readerStateRef.setValue(readerState)
    }

    private fun displayPath() {
        // Display the path taken by the reader at the end of the story
        val pathTextView = TextView(this)
        pathTextView.text = "Path taken: ${readerPath.joinToString(" -> ")}"
        choicesLayout.addView(pathTextView)
    }

    private fun logStoryStart() {
        val storyStartRef = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("stories/$storyId/statistics/storyStarts")
        storyStartRef.push().setValue(System.currentTimeMillis())

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val uniqueReaderRef = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("stories/$storyId/statistics/uniqueReaders/$userId")
        uniqueReaderRef.setValue(true)
    }

    private fun logPageVisit(pageId: String) {
        val pageVisitRef = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("stories/$storyId/statistics/pageVisits/$pageId")
        pageVisitRef.push().setValue(System.currentTimeMillis())
    }

    private fun logChoiceMade(choiceText: String) {
        val choiceMadeRef = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("stories/$storyId/statistics/choicesMade/$choiceText")
        choiceMadeRef.push().setValue(System.currentTimeMillis())
    }

    private fun logStoryCompletion() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val storyCompletionRef = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("stories/$storyId/statistics/completions/$userId")
        storyCompletionRef.setValue(System.currentTimeMillis())
    }

    private fun logReaderPath() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val pathRef = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("stories/$storyId/statistics/paths")
        val pathKey = pathRef.push().key ?: return
        pathRef.child(pathKey).setValue(readerPath)
    }

    private fun logCurrentPath() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val currentPathRef = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("stories/$storyId/statistics/currentPaths/$userId")
        currentPathRef.setValue(readerPath)
    }


}
