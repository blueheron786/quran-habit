package com.quranhabit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

// com.quranhabit.ui.MainActivity
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Navigation is handled via nav_graph.xml
    }
}