package com.example.checkmaterework.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.ActivityMainBinding
import com.example.checkmaterework.ui.fragments.AnalysisFragment
import com.example.checkmaterework.ui.fragments.CheckFragment
import com.example.checkmaterework.ui.fragments.HomeFragment
import com.example.checkmaterework.ui.fragments.KeyFragment
import com.example.checkmaterework.ui.fragments.RecordsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        replaceFragment(HomeFragment())

        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when(menuItem.itemId){
                R.id.button_home -> {
                    replaceFragment(HomeFragment())
                    true
                }

                R.id.button_key -> {
                    replaceFragment(KeyFragment())
                    true
                }

                R.id.button_check -> {
                    replaceFragment(CheckFragment())
                    true
                }

                R.id.button_records -> {
                    replaceFragment(RecordsFragment())
                    true
                }

                R.id.button_analysis -> {
                    replaceFragment(AnalysisFragment())
                    true
                }

                else -> false

            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.frame_container, fragment).commit()
    }
}