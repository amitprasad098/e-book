package com.example.ebook

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import com.example.ebook.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://ebook-fda30-default-rtdb.europe-west1.firebasedatabase.app")

        var userTypeName = ""
        val roleSpinner = findViewById<Spinner>(R.id.roleDropdownSignUp)
        val roles = arrayOf("Author", "Reader")
        val arrayAdapter = ArrayAdapter(this@SignUpActivity, android.R.layout.simple_spinner_dropdown_item, roles)
        roleSpinner.adapter = arrayAdapter

        binding.textView.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

        roleSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                userTypeName = roles[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        binding.button.setOnClickListener {
            val email = binding.emailEt.text.toString()
            val password = binding.passET.text.toString()
            val confirmPassword = binding.confirmPassEt.text.toString()
            val name = binding.nameEt.text.toString()
            val contact = binding.contactEt.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (password == confirmPassword) {

                    firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                        if (it.isSuccessful) {
                            val userId = firebaseAuth.currentUser?.uid
                            val userRef = database.reference.child("user").child(userId!!)
                            val userData = HashMap<String, Any>()
                            userData["email"] = email
                            userData["name"] = name
                            userData["contact"] = contact
                            userData["password"] = password
                            userData["user_type"] = userTypeName
                            userData["photoUrl"] = ""
                            userRef.setValue(userData).addOnCompleteListener { dbTask ->
                                if(dbTask.isSuccessful) {
                                    Toast.makeText(this, "Registration Successful.", Toast.LENGTH_LONG).show()
                                    val intent = Intent(this, SignInActivity::class.java)
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(this, "Failed to save user data", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()

                        }
                    }
                } else {
                    Toast.makeText(this, "Password is not matching", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Empty Fields Are not Allowed !!", Toast.LENGTH_SHORT).show()

            }
        }
    }
}