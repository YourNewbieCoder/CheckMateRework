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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.myToolbar)
        replaceFragment(HomeFragment(), getString(R.string.home_title))

        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when(menuItem.itemId){
                R.id.button_home -> {
                    replaceFragment(HomeFragment(), getString(R.string.home_title))
                    true
                }

                R.id.button_key -> {
                    replaceFragment(KeyFragment(), getString(R.string.key_title))
                    true
                }

                R.id.button_check -> {
                    replaceFragment(CheckFragment(), getString(R.string.check_title))
                    true
                }

                R.id.button_records -> {
                    replaceFragment(RecordsFragment(), getString(R.string.records_title))
                    true
                }

                R.id.button_analysis -> {
                    replaceFragment(AnalysisFragment(), getString(R.string.analysis_title))
                    true
                }

                else -> false

            }
        }
    }

    private fun replaceFragment(fragment: Fragment, title: String) {
        supportFragmentManager.beginTransaction().replace(R.id.frameContainer, fragment).commit()
        supportActionBar?.title = title
    }
}