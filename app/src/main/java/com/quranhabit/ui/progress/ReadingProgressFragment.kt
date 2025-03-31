package com.quranhabit.ui.progress

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quranhabit.data.QuranDatabase
import com.quranhabit.data.dao.ReadingSessionDao
import com.quranhabit.databinding.FragmentProgressBinding

class ReadingProgressFragment : Fragment() {
    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding accessed after onDestroyView")

    private val viewModel: ReadingProgressViewModel by viewModels {
        ReadingProgressViewModelFactory(
            QuranDatabase.getDatabase(requireContext()).readingSessionDao()
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.daysProgress.observe(viewLifecycleOwner) { pages ->
            _binding?.todayProgress?.text = "Today: $pages pages"
        }

        viewModel.totalProgress.observe(viewLifecycleOwner) { total ->
            _binding?.totalProgress?.text = "Total: $total pages"
        }

        binding.resetButton.setOnClickListener {
            showResetConfirmationDialog()
        }
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Reset Statistics")
            .setMessage("Are you sure you want to reset all reading statistics? This cannot be undone.")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.resetStatistics()
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ReadingProgressViewModelFactory(
    private val readingSessionDao: ReadingSessionDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ReadingProgressViewModel(readingSessionDao) as T
    }
}