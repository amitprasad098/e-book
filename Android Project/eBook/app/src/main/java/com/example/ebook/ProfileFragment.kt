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

class ProfileFragment : Fragment() {

    private lateinit var tvReaderName: TextView
    private lateinit var tvReaderEmail: TextView
    private lateinit var tvReaderContact: TextView
    private lateinit var etReaderBio: EditText
    private lateinit var tvReaderBio: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var storage: FirebaseStorage


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Initialize views
        tvReaderName = view.findViewById(R.id.tvReaderName)
        tvReaderEmail = view.findViewById(R.id.tvReaderEmail)
        tvReaderContact = view.findViewById(R.id.tvReaderContact)
        etReaderBio = view.findViewById(R.id.etReaderBio)
        tvReaderBio = view.findViewById(R.id.tvReaderBio)
        val btnSaveBio = view.findViewById<Button>(R.id.btnSaveReaderBio)
        val btnReaderSignUp = view.findViewById<Button>(R.id.btnReaderSignOut)
        profileImageView = view.findViewById(R.id.profileImageView)
        storage = FirebaseStorage.getInstance()

        val userId = FirebaseAuth.getInstance().currentUser?.uid


        btnReaderSignUp.setOnClickListener {
            val intent = Intent(requireContext(), LandingActivity::class.java)
            startActivity(intent)
        }

        // Fetch user data and Reader bio
        FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("user/$userId").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tvReaderName.text = snapshot.child("name").value as String?
                tvReaderEmail.text = snapshot.child("email").value as String?
                tvReaderContact.text = snapshot.child("contact").value as String?
                val photoUrl = snapshot.child("photoUrl").getValue(String::class.java) ?: ""

                Glide.with(this@ProfileFragment)
                    .load(photoUrl.ifEmpty { null })
                    .placeholder(R.drawable.ic_profile_default)
                    .error(R.drawable.ic_profile_default)
                    .into(profileImageView)
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("reader/$userId").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bio = snapshot.child("bio").value as String?
                tvReaderBio.text = bio
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

        btnSaveBio.setOnClickListener {
            saveBio()
        }

        profileImageView.setOnClickListener {
            getContent.launch("image/*")
        }



        return view
    }





    private fun saveBio() {
        val bio = etReaderBio.text.toString()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val ReaderData = mapOf(
                "name" to tvReaderName.text.toString(),
                "email" to tvReaderEmail.text.toString(),
                "contact" to tvReaderContact.text.toString(),
                "bio" to bio
            )
            FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app").getReference("reader/$userId").setValue(ReaderData).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Bio saved successfully", Toast.LENGTH_SHORT).show()
                    // Redirect to ReaderHomeFragment
                    fragmentManager?.beginTransaction()?.replace(R.id.frame_container, ProfileFragment())?.commit()
                } else {
                    Toast.makeText(context, "Failed to save bio", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
