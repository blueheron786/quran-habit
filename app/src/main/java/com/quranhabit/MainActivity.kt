package com.quranhabit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.quranhabit.ui.hideWithAnimation
import com.quranhabit.ui.statistics.StatisticsFragment
import com.quranhabit.ui.showWithAnimation
import com.quranhabit.ui.surah.SurahListFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, SurahListFragment())
                .commit()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_progress -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, StatisticsFragment())
                        .addToBackStack("progress") // This adds the transaction to back stack
                        .commit()
                    true
                }
                else -> false
            }
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            // If there are fragments in the back stack, pop them
            supportFragmentManager.popBackStack()
        } else {
            // If no fragments in back stack, proceed with default back button behavior
            super.onBackPressed()
        }
    }

    fun setBottomNavVisibility(visible: Boolean) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        if (visible) {
            bottomNav.showWithAnimation()
        } else {
            bottomNav.hideWithAnimation()
        }
    }
}