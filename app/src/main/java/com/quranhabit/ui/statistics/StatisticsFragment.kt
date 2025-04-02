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
import com.quranhabit.MainActivity
import com.quranhabit.data.QuranDatabase
import com.quranhabit.data.dao.StatisticsDao
import com.quranhabit.data.entity.PagesReadOnDay
import com.quranhabit.databinding.FragmentStatisticsBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Random

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
        (activity as? MainActivity)?.setBottomNavVisibility(false)

        // Text fields (observe real data if available)
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
            val minutes = (seconds ?: 0) / 60;
            binding.timeToday.text = "Time Today: $minutes" + "m"
        }

        viewModel.totalTimeRead.observe(viewLifecycleOwner) { seconds ->
            val minutes = (seconds ?: 0) / 60;
            val hours = minutes / 60;
            var displayMinutes = minutes % 60;
            binding.totalTime.text = "Total Time: $hours" + "h" + ", $displayMinutes" + "m"
        }

        // Generate fake data for testing the view
        val fakeDays = 30
        val fakeData = generateFakeReadingDataForView(fakeDays)
        val fakePagesReadList = fakeData.map { it.pagesRead }
        val fakeDateList = fakeData.map { LocalDate.parse(it.date, DateTimeFormatter.ISO_DATE) }

        // Set the fake data to the monthly pages chart
        binding.monthlyPagesChart.goal = 20
        binding.monthlyPagesChart.displayDays = fakeDays
        binding.monthlyPagesChart.useNumericLabels = true
        binding.monthlyPagesChart.roundBars = true
        // Custom label interval for the top chart
        binding.monthlyPagesChart.labelInterval = 5
        binding.monthlyPagesChart.setData(fakePagesReadList, fakeDateList)

        // Generate fake data for the weekly time chart (7 days)
        val fakeDaysWeekly = 7
        val fakeDataWeekly = generateFakeReadingDataForView(fakeDaysWeekly)
        val fakeTimeSpentList = fakeDataWeekly.map { it.secondsSpendReading / 60 }
        val fakeDateListWeekly = fakeDataWeekly.map { LocalDate.parse(it.date, DateTimeFormatter.ISO_DATE) }

        // Set the fake data to the weekly time chart
        binding.weeklyTimeChart.goal = 30
        binding.weeklyTimeChart.displayDays = fakeDaysWeekly
        binding.weeklyTimeChart.useNumericLabels = true
        // Increased bar spacing for the bottom chart
        binding.weeklyTimeChart.barSpacingFactor = 2.5f
        binding.weeklyTimeChart.setData(fakeTimeSpentList, fakeDateListWeekly)

        // Buttonz
        binding.resetButton.setOnClickListener {
            showResetConfirmationDialog()
        }
    }

    private fun generateFakeReadingDataForView(days: Int): List<PagesReadOnDay> {
        val fakeData = mutableListOf<PagesReadOnDay>()
        val random = Random()
        val torontoTimeZone = java.time.ZoneId.of("America/Toronto")
        val currentDate = LocalDate.now(torontoTimeZone)
        val dateFormatter = DateTimeFormatter.ISO_DATE

        for (i in 0 until days) {
            val date = currentDate.minusDays(i.toLong()) // Ensure 'i' is converted to Long
            val pagesRead = random.nextInt(25)
            val secondsSpendReading = random.nextInt(3600)

            fakeData.add(
                PagesReadOnDay(
                    date = date.format(dateFormatter),
                    pagesRead = pagesRead,
                    secondsSpendReading = secondsSpendReading
                )
            )
        }
        return fakeData
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