package com.example.ebook

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class HomeFragment : Fragment() {

    private lateinit var linearLayoutStories: LinearLayout
    private lateinit var editTextSearch: EditText
    private lateinit var tvUsername: TextView
    private lateinit var database: FirebaseDatabase
    private var currentUser: FirebaseUser? = null
    private var allStories: MutableList<Story> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        linearLayoutStories = view.findViewById(R.id.rv_home)
        editTextSearch = view.findViewById(R.id.et_searchHome)
        tvUsername = view.findViewById(R.id.tv_username)

        currentUser = FirebaseAuth.getInstance().currentUser
        database = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app")

        if (currentUser != null) {
            val userId = currentUser!!.uid
            val userRef = database.reference.child("user").child(userId)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val username = snapshot.child("name").getValue(String::class.java)
                    tvUsername.text = "Welcome, $username!"
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        } else {
            tvUsername.text = "Welcome!"
        }

        database = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app")

        readAndDisplayAllStories()

        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                searchStories(s.toString())
            }
        })

        // 为所有类别标签设置点击监听器
        view.findViewById<TextView>(R.id.allCategory).setOnClickListener {
            filterStoriesByCategory("")
        }

        view.findViewById<TextView>(R.id.fictionCategory).setOnClickListener {
            filterStoriesByCategory("Fiction")
        }

        view.findViewById<TextView>(R.id.actionCategory).setOnClickListener {
            filterStoriesByCategory("Action")
        }

        view.findViewById<TextView>(R.id.HorrorCategory).setOnClickListener {
            filterStoriesByCategory("Horror")
        }

        view.findViewById<TextView>(R.id.RomanticCategory).setOnClickListener {
            filterStoriesByCategory("Romantic")
        }



        return view
    }

    private fun readAndDisplayAllStories() {
        val storiesRef = database.reference.child("stories")
        storiesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allStories.clear()
                linearLayoutStories.removeAllViews()

                for (storySnapshot in snapshot.children) {
                    val story = storySnapshot.getValue(Story::class.java)
                    if (story != null) {
                        allStories.add(story)
                        displayStory(story)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun searchStories(keyword: String) {
        linearLayoutStories.removeAllViews()

        for (story in allStories) {
            if (story.name.contains(keyword, ignoreCase = true) || story.description.contains(keyword, ignoreCase = true)) {
                displayStory(story)
            }
        }
    }

    private fun filterStoriesByCategory(category: String) {
        linearLayoutStories.removeAllViews()

        for (story in allStories) {
            if (category.isEmpty() || story.category.equals(category, ignoreCase = true)) {
                displayStory(story)
            }
        }
    }

    private fun displayStory(story: Story) {
        if (!isAdded) return
        // 整体布局，水平方向
        val storyLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_VERTICAL
        }

        // 封面图片
        val imageViewCover = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(200, 300).apply {
                setMargins(0, 0, 16, 0)
            }
        }
        Glide.with(this).load(story.coverUrl).into(imageViewCover)
        storyLayout.addView(imageViewCover)

        // 文本信息布局，垂直方向
        val textLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        // 故事名称
        val textViewStoryName = TextView(requireContext()).apply {
            text = "Name: ${story.name}"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
        }
        textLayout.addView(textViewStoryName)

        // 类别
        val textViewCategory = TextView(requireContext()).apply {
            text = "Category: ${story.category}"
            textSize = 16f
        }
        textLayout.addView(textViewCategory)

        // 作者名
        val textViewAuthorName = TextView(requireContext()).apply {
            text = "Author: ${story.authorName}"
            textSize = 16f
        }
        textLayout.addView(textViewAuthorName)

        // 添加文本布局到主布局
        storyLayout.addView(textLayout)

        // 将整个故事布局添加到线性布局中
        linearLayoutStories.addView(storyLayout)

        // 设置点击整个故事布局的监听器
        storyLayout.setOnClickListener {
            val intent = Intent(requireContext(), StoryDetailActivity::class.java).apply {
                putExtra("storyName", story.name)
                putExtra("storyAuthor", story.authorName)
                putExtra("storyDescription", story.description)
                putExtra("authorId", story.authorId)
//                putExtra("storyId", story.id)
                putExtra("coverUrl", story.coverUrl)


            }
            startActivity(intent)
        }
    }

}

