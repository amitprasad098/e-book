package com.example.ebook

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AuthorStatisticsActivity : AppCompatActivity() {
    private lateinit var statisticsTextView: TextView
    private lateinit var storyId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_author_statistics)

        statisticsTextView = findViewById(R.id.statisticsTextView)
        val backButton = findViewById<ImageView>(R.id.ic_backDetail)
        storyId = intent.getStringExtra("STORY_ID") ?: ""

        loadStatistics()

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun loadStatistics() {
        val statisticsRef = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("stories/$storyId/statistics")
        statisticsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val statistics = StringBuilder()

                // Total Reads
                val totalReads = snapshot.child("storyStarts").childrenCount
                statistics.append("<b>Total Reads:</b> $totalReads<br><br>")

                // Unique Readers
                val uniqueReaders = snapshot.child("uniqueReaders").childrenCount
                statistics.append("<b>Unique Readers:</b> $uniqueReaders<br><br>")

                // Choice Popularity
                val choicesMadeSnapshot = snapshot.child("choicesMade")
                statistics.append("<b>Choice Popularity:</b><br>")
                for (choiceSnapshot in choicesMadeSnapshot.children) {
                    val choiceText = choiceSnapshot.key
                    val choices = choiceSnapshot.childrenCount
                    statistics.append("$choiceText: $choices times<br>")
                }
                statistics.append("<br>")

                // Completion Rate
                val completions = snapshot.child("completions").childrenCount
                val completionRate = if (uniqueReaders > 0) (completions.toDouble() / uniqueReaders * 100).toInt() else 0
                statistics.append("<b>Completion Rate:</b> $completionRate%<br><br>")

                // Distinct Story Paths and Current Users on Each Path
                val pathsSnapshot = snapshot.child("currentPaths")
                val pathCounts = mutableMapOf<List<String>, Int>()
                for (userPathSnapshot in pathsSnapshot.children) {
                    val path = userPathSnapshot.value as? List<String>
                    if (path != null) {
                        pathCounts[path] = pathCounts.getOrDefault(path, 0) + 1
                    }
                }

                statistics.append("<b>Current Paths Taken by Users:</b><br>")
                for ((path, count) in pathCounts) {
                    statistics.append("Path: ${path.joinToString(" -> ")}<br>Chosen by: $count user(s)<br><br>")
                }

                statisticsTextView.text = android.text.Html.fromHtml(statistics.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }
}
