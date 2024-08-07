package com.example.ebook

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class AuthorProfileFragment : Fragment() {

    private lateinit var tvAuthorName: TextView
    private lateinit var tvAuthorEmail: TextView
    private lateinit var tvAuthorContact: TextView
    private lateinit var etAuthorBio: EditText
    private lateinit var tvAuthorBio: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var storage: FirebaseStorage



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_author_profile, container, false)

        // Initialize views
        tvAuthorName = view.findViewById(R.id.tvAuthorName)
        tvAuthorEmail = view.findViewById(R.id.tvAuthorEmail)
        tvAuthorContact = view.findViewById(R.id.tvAuthorContact)
        etAuthorBio = view.findViewById(R.id.etAuthorBio)
        tvAuthorBio = view.findViewById(R.id.tvAuthorBio)
        profileImageView = view.findViewById(R.id.profileImageView)
        val btnSaveBio = view.findViewById<Button>(R.id.btnSaveBio)
        val btnSignOut = view.findViewById<Button>(R.id.btnSignOut)
        storage = FirebaseStorage.getInstance()

        val userId = FirebaseAuth.getInstance().currentUser?.uid

        // Fetch user data and author bio
        FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("user/$userId").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tvAuthorName.text = snapshot.child("name").value as String?
                tvAuthorEmail.text = snapshot.child("email").value as String?
                tvAuthorContact.text = snapshot.child("contact").value as String?
                val photoUrl = snapshot.child("photoUrl").getValue(String::class.java) ?: ""

                Glide.with(this@AuthorProfileFragment)
                    .load(photoUrl.ifEmpty { null })
                    .placeholder(R.drawable.ic_profile_default)
                    .error(R.drawable.ic_profile_default)
                    .into(profileImageView)
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("author/$userId").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bio = snapshot.child("bio").value as String?
                tvAuthorBio.text = bio
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val storageRef = storage.reference.child("profile_pictures/${userId}.jpg")
                val uploadTask = storageRef.putFile(uri)
                uploadTask.addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("user").child(userId!!).child("photoUrl").setValue(downloadUri.toString())
                        Glide.with(this).load(downloadUri).placeholder(R.drawable.ic_profile_default).error(R.drawable.ic_profile_default).into(profileImageView)
                    }
                }
            }
        }

        profileImageView.setOnClickListener {
            getContent.launch("image/*")
        }

        btnSaveBio.setOnClickListener {
            saveBio()
        }

        btnSignOut.setOnClickListener {
            val intent = Intent(requireContext(), SignInActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    private fun saveBio() {
        val bio = etAuthorBio.text.toString()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val authorData = mapOf(
                "name" to tvAuthorName.text.toString(),
                "email" to tvAuthorEmail.text.toString(),
                "contact" to tvAuthorContact.text.toString(),
                "bio" to bio
            )
            FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("author/$userId").setValue(authorData).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Bio saved successfully", Toast.LENGTH_SHORT).show()
                    // Redirect to AuthorHomeFragment
                    fragmentManager?.beginTransaction()?.replace(R.id.frame_container, AuthorHomeFragment())?.commit()
                } else {
                    Toast.makeText(context, "Failed to save bio", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
