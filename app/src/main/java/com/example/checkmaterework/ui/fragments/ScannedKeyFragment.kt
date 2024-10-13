package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentScannedKeyBinding

class ScannedKeyFragment : Fragment(), ToolbarTitleProvider {

    private lateinit var scannedKeyBinding: FragmentScannedKeyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        scannedKeyBinding = FragmentScannedKeyBinding.inflate(inflater, container, false)
        return scannedKeyBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the recognized text passed from the EditAnswerKeyFragment
        val recognizedText = arguments?.getString("recognizedText")
        scannedKeyBinding.textViewRecognizedText.text = recognizedText
    }

    override fun getFragmentTitle(): String {
        return getString(R.string.scanned_key_title)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar()
    }

    private fun setupToolbar() {
        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(activity.findViewById(R.id.myToolbar))

        val canGoBack = parentFragmentManager.backStackEntryCount > 0
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(canGoBack)
        activity.supportActionBar?.setDisplayShowHomeEnabled(canGoBack)

        activity.supportActionBar?.title = getFragmentTitle()

        if (canGoBack) {
            activity.findViewById<Toolbar>(R.id.myToolbar).setNavigationOnClickListener {
                activity.onBackPressed()
            }
        }
    }

    companion object {
        fun newInstance(recognizedText: String): ScannedKeyFragment {
            val fragment = ScannedKeyFragment()
            val bundle = Bundle()
            bundle.putString("recognizedText", recognizedText)
            fragment.arguments = bundle
            return fragment
        }
    }
}