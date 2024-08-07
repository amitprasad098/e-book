package com.example.ebook

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
class AuthorHomeActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_author_home)

        bottomNavigationView = findViewById(R.id.bottom_nav)

        bottomNavigationView.setOnItemSelectedListener{menuItem ->
            when(menuItem.itemId){
                R.id.bottom_home -> {
                    replaceFragment(AuthorHomeFragment())
                    true
                }
                R.id.bottom_libriray -> {
                    replaceFragment(LibraryFragment())
                    true
                }
                R.id.bottom_Favorites -> {
                    replaceFragment(AuthorStatisticsFragment())
                    true
                }
                R.id.bottom_profile -> {
                    replaceFragment(AuthorProfileFragment())
                    true
                }

                else -> false
            }
        }
        replaceFragment(AuthorHomeFragment())
    }

    private fun replaceFragment(fragment: Fragment){
        supportFragmentManager.beginTransaction().replace(R.id.frame_container,fragment).commit()

    }
}