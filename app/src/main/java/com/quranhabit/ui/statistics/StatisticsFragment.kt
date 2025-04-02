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
import androidx.lifecycle.lifecycleScope
import com.quranhabit.MainActivity
import com.quranhabit.data.QuranDatabase
import com.quranhabit.data.dao.StatisticsDao
import com.quranhabit.databinding.FragmentStatisticsBinding
import com.quranhabit.utils.DateUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Collections

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding accessed after onDestroyView")

    private val viewModel: StatisticsViewModel by viewModels {
        ReadingProgressViewModelFactory(
            QuranDatabase.getDatabase(requireContext()).statisticsDao()
        )
    }

    private val dateFormatter = DateTimeFormatter.ISO_DATE

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.setBottomNavVisibility(false)

        // Text fields (observe real data)
        viewModel.pagesReadToday.observe(viewLifecycleOwner) { pages ->
            binding.pagesToday.text = "Today: $pages pages"
        }

        viewModel.pagesReadMonth.observe(viewLifecycleOwner) { pages ->
            binding.pagesMonth.text = "30 days: $pages pages"
        }

        viewModel.totalPagesRead.observe(viewLifecycleOwner) { pages ->
            binding.totalPages.text = "Lifetime: $pages pages"
        }

        viewModel.timeReadToday.observe(viewLifecycleOwner) { seconds ->
            val minutes = seconds / 60
            binding.timeToday.text = "Time Today: $minutes" + "m"
        }

        viewModel.totalTimeRead.observe(viewLifecycleOwner) { seconds ->
            val minutes = seconds / 60
            val hours = minutes / 60
            val displayMinutes = minutes % 60
            binding.totalTime.text = "Total Time: $hours" + "h" + ", $displayMinutes" + "m"
        }

        // Observe monthly pages data
        lifecycleScope.launch {
            viewModel.monthlyData.collectLatest { dailyDataList ->
                val displayDays = binding.monthlyPagesChart.displayDays
                val latestDataDate = dailyDataList.maxByOrNull { LocalDate.parse(it.date, dateFormatter) }
                    ?.let { LocalDate.parse(it.date, dateFormatter) } ?: LocalDate.now()
                val startDate = latestDataDate.minusDays(displayDays.toLong() - 1)
                val endDate = latestDataDate // Use the latest data date as the end

                val allDatesInRange = generateDateRange(startDate, endDate)
                val paddedDataMap = padDataWithZerosToMap(dailyDataList)

                val pagesReadList = allDatesInRange.map { paddedDataMap[it] ?: 0 }
                val dateList = allDatesInRange

                binding.monthlyPagesChart.goal = 20
                binding.monthlyPagesChart.useNumericLabels = true
                binding.monthlyPagesChart.roundBars = true
                binding.monthlyPagesChart.labelInterval = 5
                binding.monthlyPagesChart.setData(pagesReadList, dateList)
            }
        }

        // Observe weekly time data
        lifecycleScope.launch {
            viewModel.weeklyTimeData.collectLatest { dailyDataList ->
                val displayDays = binding.weeklyTimeChart.displayDays // Should be 7
                val latestDataDate = dailyDataList.maxByOrNull { LocalDate.parse(it.date, dateFormatter) }
                    ?.let { LocalDate.parse(it.date, dateFormatter) } ?: LocalDate.now()
                val startDate = latestDataDate.minusDays(displayDays.toLong() - 1)
                val endDate = latestDataDate // Use the latest data date as the end

                val allDatesInRange = generateDateRange(startDate, endDate)
                val paddedDataMap = padTimeDataWithZerosToMap(dailyDataList)

                val timeSpentList = allDatesInRange.map { paddedDataMap[it] ?: 0 }
                val dateListWeekly = allDatesInRange

                binding.weeklyTimeChart.goal = 30
                binding.weeklyTimeChart.useNumericLabels = true
                binding.weeklyTimeChart.barSpacingFactor = 2.5f
                binding.weeklyTimeChart.displayDays = 7 // Ensure displayDays is set correctly here
                binding.weeklyTimeChart.setData(timeSpentList, dateListWeekly)
            }
        }

        // Buttonz
        binding.resetButton.setOnClickListener {
            showResetConfirmationDialog()
        }
    }

    private fun generateDateRange(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            dates.add(currentDate)
            currentDate = currentDate.plusDays(1)
        }
        return dates
    }

    private fun padDataWithZerosToMap(actualData: List<DailyData>): Map<LocalDate, Int> {
        return actualData.associateBy<DailyData, LocalDate, Int>(
            keySelector = { LocalDate.parse(it.date, dateFormatter) },
            valueTransform = { it.pagesRead }
        )
    }

    private fun padTimeDataWithZerosToMap(actualData: List<DailyData>): Map<LocalDate, Int> {
        return actualData.associateBy<DailyData, LocalDate, Int>(
            keySelector = { LocalDate.parse(it.date, dateFormatter) },
            valueTransform = { it.secondsReading / 60 } // Corrected typo here
        )
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