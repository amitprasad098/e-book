package com.example.ebook

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LibraryFragment : Fragment() {
    private var allStories: MutableList<Story> = mutableListOf()
    private lateinit var editTextSearch: EditText

    private lateinit var recyclerView: RecyclerView
    var mchatAdapter: ChatAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_library_new, container, false)
        recyclerView=view.findViewById(R.id.rv_favorites)
        mchatAdapter = ChatAdapter(requireActivity())

        editTextSearch = view.findViewById(R.id.et_searchFavorites)

        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if(s.toString().length==0){
                    mchatAdapter?.setmEntityList(allStories)

                    return
                }
                searchStories(s.toString())
            }
        })





        // 定义一个线性布局管理器
        // 定义一个线性布局管理器
        val manager = LinearLayoutManager(activity)
        // 设置布局管理器
        // 设置布局管理器
        recyclerView.layoutManager = manager
        // 设置adapter
        // 设置adapter
        recyclerView.adapter = mchatAdapter




        return  view
    }

    override fun onStart() {
        super.onStart()
        findAllBook()
//        allStories.clear()

    }
    private fun searchStories(keyword: String) {
//        LibraryList.removeAllViews()
        var  searchStories: MutableList<Story> = mutableListOf()
        for (story in allStories) {
            if (story.name.contains(keyword, ignoreCase = true) || story.description.contains(keyword, ignoreCase = true)) {
                searchStories.add(story)
                mchatAdapter?.setmEntityList(searchStories)

            }
        }
    }


    private fun findAllBook() {
        val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
//        val database = FirebaseDatabase.getInstance()

        Log.i("TAG", "findAllBook:=== "+currentUserID)
        val   database = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app")

        val userLibraryRef = database.getReference("user_library/$currentUserID")
        userLibraryRef.addValueEventListener(object :ValueEventListener

        {


            override fun onDataChange(snapshot: DataSnapshot) {
                allStories.clear()
                for (storySnapshot in snapshot.children) {
                    val story = storySnapshot.getValue(Story::class.java)

                    if (story != null) {
                        allStories.add(story)
                        Log.i("TAG", "onDataChange11:="+
                                allStories.size+
                                story.name+"==="+story.description)
                    }
                    mchatAdapter?.setmEntityList(allStories)
                }
            }


            override fun onCancelled(error: DatabaseError) {
            }
        }
        )

    }

    class ChatAdapter(activity: FragmentActivity) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {
        //        private var mEntityList: List<String>? = ArrayList()
        private var mEntityList: MutableList<Story> = mutableListOf()

        private val mActivity: FragmentActivity

        init {
            mActivity = activity
        }
        fun setmEntityList(mEntityList: MutableList<Story>) {
            this.mEntityList = mEntityList
            //一定要记得加，否则视图不会更新！！！
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
            val view: View =
                LayoutInflater.from(parent.context).inflate(R.layout.favorites_item, parent, false)
            return ChatViewHolder(view)
        }

        override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
            holder.mText.text ="name:" +mEntityList!![position].name
            holder.mTextDes.text ="storyDescription:" +mEntityList!![position].description

            holder.mLayoutItem.setTag(position)
//            Log.d(TAG, "onBindViewHolder() called with: holder = $holder, position = $position")
            Glide.with(mActivity).load(mEntityList!![position].coverUrl).into(holder.imageViewCover)

            loadReaderState(mEntityList!![position].id)
            Log.i("TAG", "onBindViewHolder: "+mEntityList!![position].id)
            holder.mLayoutItem.setOnClickListener(View.OnClickListener { view ->
                val post = view!!.tag as Int
                val story = mEntityList.get(post)
//                val intent = Intent(activity, StoryDetailActivity::class.java)
//                intent.putExtra("storyName", story.name)
//                intent.putExtra("storyDescription", story.description)
//                Log.i("TAG", "displayStory:===== "+story.category)
//                startActivity(intent)

                val intent = Intent(mActivity, StoryDetailActivity::class.java)
                intent.putExtra("storyName", story.name)
                intent.putExtra("storyDescription", story.description)
                intent.putExtra("coverUrl", story.coverUrl)


                Log.i("TAG", "displayStory:===== "+story.category)
                mActivity.startActivity(intent)



            })



        }
        private fun loadReaderState(storyId:String) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            Log.i("TAG", "onBindViewHolder=----userId---: "+userId+"=="+storyId)

            val readerStateRef = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("users/$userId/library/$storyId")
            readerStateRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val state = snapshot.value as Map<String, Any>
//                    currentPageId = state["currentPageId"] as? String
                        val path = state["path"] as? List<String>
                        Log.i("TAG", "onBindViewHolder=--path-----: "+path)
                        if (path != null) {
                            Log.i("TAG", "onBindViewHolder=-------: "+path)


                        }

                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }

        override fun getItemCount(): Int {
            return if (mEntityList == null) 0 else mEntityList!!.size
        }

        inner class ChatViewHolder(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            val mText: TextView
            val mTextDes: TextView
            val mLayoutItem: LinearLayout
            val  imageViewCover:ImageView

            init {
                mLayoutItem = itemView.findViewById<View>(R.id.layout_item) as LinearLayout

                imageViewCover = itemView.findViewById<View>(R.id.img_storyCoverpage) as ImageView
                mTextDes = itemView.findViewById<View>(R.id.tv_des) as TextView
                mText = itemView.findViewById<View>(R.id.tv_name) as TextView
            }
        }
    }



}