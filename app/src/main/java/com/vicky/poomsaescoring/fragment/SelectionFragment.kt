package com.vicky.poomsaescoring.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.vicky.poomsaescoring.R
import com.vicky.poomsaescoring.databinding.FragmentSelectionBinding

class SelectionFragment : Fragment() {

    private var _binding: FragmentSelectionBinding? = null
    private val b get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSelectionBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewListener()
    }

    private fun viewListener() {
        b.apply {
            cdRefereeSystem.setOnClickListener {
                findNavController().navigate(R.id.action_selectionFragment_to_scoringListFragment)
                // or R.id.scoringListFragment if you prefer direct destination
            }

            cdHostSystem.setOnClickListener {
                findNavController().navigate(R.id.action_selectionFragment_to_hostFragment)
            }
        }
    }

}