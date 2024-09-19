package com.example.checkmaterework.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.ActivityMainBinding
import com.example.checkmaterework.ui.fragments.AnalysisFragment
import com.example.checkmaterework.ui.fragments.CheckFragment
import com.example.checkmaterework.ui.fragments.ClassesFragment
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
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, fragment)

            // Add to the back stack only if the fragment is not one of the main fragments
            if (fragment !is HomeFragment && fragment !is KeyFragment &&
                fragment !is CheckFragment && fragment !is RecordsFragment &&
                fragment !is AnalysisFragment) {
                transaction.addToBackStack(title) // Add a title tag to the back stack entry
            }

            transaction.commit()

        // Update the toolbar title
        supportActionBar?.title = title

        // Show or hide the back button depending on the fragment
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

            // After popping, get the current top fragment
            val currentFragment = fragmentManager.findFragmentById(R.id.frameContainer)

            if (currentFragment != null && fragmentManager.backStackEntryCount > 0) {
                // Get the tag from the previous fragment in the back stack
                val fragmentTag = fragmentManager.getBackStackEntryAt(fragmentManager.backStackEntryCount - 1).name
                supportActionBar?.title = fragmentTag ?: getString(R.string.app_name) // Set title to fragment tag or app name
            }

            // Handle back button visibility based on the current fragment
//            val currentFragmentTag = currentFragment?.javaClass?.simpleName
            val isMainFragment = currentFragment is HomeFragment || currentFragment is KeyFragment ||
                    currentFragment is CheckFragment || currentFragment is RecordsFragment || currentFragment is AnalysisFragment

            if (isMainFragment) {
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
                supportActionBar?.setDisplayShowHomeEnabled(false)
            } else {
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.setDisplayShowHomeEnabled(true)
            }
        } else {
            super.onBackPressed() // Default back press behavior
        }
    }
}