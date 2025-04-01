package com.quranhabit.ui.statistics

import StatisticsViewModel
import android.app.AlertDialog
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quranhabit.MainActivity
import com.quranhabit.data.QuranDatabase
import com.quranhabit.data.dao.StatisticsDao
import com.quranhabit.databinding.FragmentStatisticsBinding
import java.util.Locale

class StatisticsFragment : Fragment() {

    companion object {
        const val PAGES_PER_DAY_GOAL = 20 // Make sure this is public (no 'private')
    }

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
        (activity as? MainActivity)?.setBottomNavVisibility(false)

        // Text fields
        viewModel.pagesReadToday.observe(viewLifecycleOwner) { pages ->
            binding.pagesToday.text = "Today: $pages pages"
        }

        viewModel.totalPagesRead.observe(viewLifecycleOwner) { pages ->
            binding.totalPages.text = "Lifetime: $pages pages"
        }

        viewModel.timeReadToday.observe(viewLifecycleOwner) { seconds ->
            val minutes = (seconds ?: 0) / 60;
            binding.timeToday.text = "Time Today: $minutes" + "m"
        }

        viewModel.totalTimeRead.observe(viewLifecycleOwner) { seconds ->
            val minutes = (seconds ?: 0) / 60;
            val hours = minutes / 60;
            var displayMinutes = minutes % 60;
            binding.totalTime.text = "Total Time: $hours" + "h" + ", $displayMinutes" + "m"
        }

        // Graphs
        viewModel.weeklyData.observe(viewLifecycleOwner) { weeklyData ->
            val pagesRead = weeklyData.map { it.pagesRead }
            val secondsReading = weeklyData.map { it.secondsReading }
            val labels = weeklyData.map {
                SimpleDateFormat("EEE", Locale.getDefault())
                    .format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.date))
            }

            binding.weeklyPagesChart.setData(pagesRead, labels)
            binding.weeklyTimeChart.setData(secondsReading, labels)
        }

        // Buttonz
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
        (activity as? MainActivity)?.setBottomNavVisibility(true)
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