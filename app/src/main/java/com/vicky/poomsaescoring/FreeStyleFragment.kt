package com.vicky.poomsaescoring

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.vicky.poomsaescoring.databinding.FragmentFreeStyleBinding


class FreeStyleFragment : Fragment() {

    private var _binding: FragmentFreeStyleBinding? = null
    private val b get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFreeStyleBinding.inflate(inflater, container, false)
        return b.root
    }
}