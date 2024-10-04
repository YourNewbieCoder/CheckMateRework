package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentScannedKeyBinding

class ScannedKeyFragment : Fragment() {

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