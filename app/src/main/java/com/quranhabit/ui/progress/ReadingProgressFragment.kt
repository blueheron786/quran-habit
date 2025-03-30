package com.quranhabit.ui.progress

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
    private val binding get() = _binding!!
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

        viewModel.todayProgress.observe(viewLifecycleOwner) { pages ->
            binding.todayProgress.text = "Today: $pages pages"
        }

        viewModel.totalProgress.observe(viewLifecycleOwner) { total ->
            binding.totalProgress.text = "Total: $total pages"
        }

        viewModel.weeklyStreak.observe(viewLifecycleOwner) { streak ->
            binding.streakProgress.text = "Current streak: $streak days"
        }
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