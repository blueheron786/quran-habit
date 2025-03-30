package com.quranhabit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.quranhabit.ui.progress.ReadingProgressFragment
import com.quranhabit.ui.surah.SurahListFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, SurahListFragment()) // Replace with your main fragment
                .commit()
        }

        // Bottom nav setup
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_progress -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, ReadingProgressFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
}