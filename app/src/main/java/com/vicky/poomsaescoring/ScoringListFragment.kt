package com.vicky.poomsaescoring

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.vicky.poomsaescoring.databinding.FragmentScoringListBinding

class ScoringListFragment : Fragment() {

    private var _binding: FragmentScoringListBinding? = null
    private val b get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentScoringListBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewListener()
    }

    private fun viewListener() {
        b.apply {
            cdCutoffSystem.setOnClickListener {
                findNavController().navigate(R.id.cutoffFragment)
            }

            cdSingleEliminationSystem.setOnClickListener {
                findNavController().navigate(R.id.singleEliminationFragment)
            }

            cdFreeStyleSystem.setOnClickListener {
                findNavController().navigate(R.id.freeStyleFragment)
            }
        }
    }

}