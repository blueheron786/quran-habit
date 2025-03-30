package com.quranhabit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.quranhabit.ui.progress.ReadingProgressFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<FloatingActionButton>(R.id.fab_progress).setOnClickListener {
            // Replace current fragment with ReadingProgressFragment
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ReadingProgressFragment())
                .addToBackStack(null) // Optional: Add to back stack for navigation
                .commit()
        }
    }
}