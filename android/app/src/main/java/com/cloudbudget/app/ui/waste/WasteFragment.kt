package com.cloudbudget.app.ui.waste

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.cloudbudget.app.R

class WasteFragment : Fragment() {

    private val viewModel: WasteViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_waste, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.waste.observe(viewLifecycleOwner) { waste ->
            // Update with real data when available
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        viewModel.loadWaste()
    }
}
