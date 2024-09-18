package com.example.checkmaterework.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.ActivityMainBinding
import com.example.checkmaterework.ui.fragments.AnalysisFragment
import com.example.checkmaterework.ui.fragments.CheckFragment
import com.example.checkmaterework.ui.fragments.EditAnswerKeyFragment
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
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, fragment)
            .addToBackStack(null) // Ensure fragments are added to the back stack
            .commit()

        supportActionBar?.title = title

        // Determine if the back button should be shown or hidden
        val isMainFragment = fragment is HomeFragment || fragment is KeyFragment ||
                fragment is CheckFragment || fragment is RecordsFragment || fragment is AnalysisFragment

        if (isMainFragment) {
            // Disable the back button for main fragments
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            supportActionBar?.setDisplayShowHomeEnabled(false)
        } else {
            // Enable the back button for non-main fragments
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            // Set the white back arrow icon
            supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_arrow_back_24)
        }

    }

    override fun onBackPressed() {
        val fragmentManager = supportFragmentManager
        if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack() // Go back to the previous fragment
        } else {
            super.onBackPressed() // Default back press behavior
        }

        // Reset the toolbar when not in EditAnswerKeyFragment
        val fragment = fragmentManager.findFragmentById(R.id.frameContainer)
        if (fragment !is EditAnswerKeyFragment) {
            supportActionBar?.setDisplayHomeAsUpEnabled(false) // Hide the back button
            supportActionBar?.setDisplayShowHomeEnabled(false)
            supportActionBar?.title = getString(R.string.app_name) // Reset title
        }
    }
}