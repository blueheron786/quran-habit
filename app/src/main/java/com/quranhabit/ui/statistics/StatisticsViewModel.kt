import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranhabit.data.dao.StatisticsDao
import com.quranhabit.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatisticsViewModel(private val statisticsDao: StatisticsDao) : ViewModel() {
    private val _pagesReadToday = MutableLiveData<Int>()
    val pagesReadToday: LiveData<Int> = _pagesReadToday

    private val _totalPagesRead = MutableLiveData<Int>()
    val totalPagesRead: LiveData<Int> = _totalPagesRead

    init {
        loadProgress()
    }

    private fun loadProgress() {
        viewModelScope.launch {
            val todayDate = DateUtils.getTodayDate()

            val todaysProgress = withContext(Dispatchers.IO) {
                statisticsDao.getDaysProgress(todayDate) ?: 0
            }

            val totalProgress = withContext(Dispatchers.IO) {
                statisticsDao.getTotalPagesRead()
            }

            _pagesReadToday.value = todaysProgress
            _totalPagesRead.value = totalProgress
        }
    }

    fun resetStatistics() {
        viewModelScope.launch {
            statisticsDao.resetAllStatistics()
            _pagesReadToday.value = 0
            _totalPagesRead.value = 0
        }
    }
}