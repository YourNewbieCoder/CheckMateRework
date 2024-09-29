package com.example.checkmaterework.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.ActivityMainBinding
import com.example.checkmaterework.ui.fragments.AnalysisFragment
import com.example.checkmaterework.ui.fragments.CheckFragment
import com.example.checkmaterework.ui.fragments.HomeFragment
import com.example.checkmaterework.ui.fragments.KeyFragment
import com.example.checkmaterework.ui.fragments.RecordsFragment
import com.example.checkmaterework.ui.fragments.ToolbarTitleProvider

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
        val transaction = supportFragmentManager.beginTransaction().replace(R.id.frameContainer, fragment)

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

            // Update the toolbar title after navigating back
            val currentFragment = fragmentManager.fragments.lastOrNull()
            if (currentFragment is ToolbarTitleProvider) {
                supportActionBar?.title = (currentFragment as ToolbarTitleProvider).getFragmentTitle()
            }
        } else {
            super.onBackPressed() // Default back press behavior
        }
    }
}