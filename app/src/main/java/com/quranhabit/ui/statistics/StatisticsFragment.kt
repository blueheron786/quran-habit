package com.quranhabit.ui.statistics

import StatisticsViewModel
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
import com.quranhabit.data.dao.StatisticsDao
import com.quranhabit.databinding.FragmentStatisticsBinding

class StatisticsFragment : Fragment() {
    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding accessed after onDestroyView")

    private val viewModel: StatisticsViewModel by viewModels {
        ReadingProgressViewModelFactory(
            QuranDatabase.getDatabase(requireContext()).statisticsDao()
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.pagesReadToday.observe(viewLifecycleOwner) { pages ->
            binding.todayProgress.text = "Today: $pages pages"
        }

        viewModel.totalPagesRead.observe(viewLifecycleOwner) { pages ->
            binding.totalProgress.text = "Total: $pages pages"
        }

        viewModel.timeReadToday.observe(viewLifecycleOwner) { minutes ->
            binding.todayTime.text = "Time Today: $minutes min"
        }

        viewModel.totalTimeRead.observe(viewLifecycleOwner) { minutes ->
            binding.totalTime.text = "Total Time: $minutes min"
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
    private val statisticsDao: StatisticsDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return StatisticsViewModel(statisticsDao) as T
    }
}