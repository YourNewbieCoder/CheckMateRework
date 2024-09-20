package com.example.checkmaterework.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.example.checkmaterework.R
import com.example.checkmaterework.databinding.FragmentEditAnswerKeyBinding
import com.example.checkmaterework.models.AnswerSheetEntity

class EditAnswerKeyFragment(private val answerSheet: AnswerSheetEntity) : Fragment() {

    private lateinit var editAnswerKeyBinding: FragmentEditAnswerKeyBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        editAnswerKeyBinding = FragmentEditAnswerKeyBinding.inflate(inflater, container, false)
        return editAnswerKeyBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Here you can set up your view logic, such as loading the data of the answer sheet
        loadAnswerKeyData(answerSheet)
    }

    private fun loadAnswerKeyData(answerSheet: AnswerSheetEntity) {

    }

}