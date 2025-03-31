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

    private val _timeReadToday = MutableLiveData<Int>()
    val timeReadToday: LiveData<Int> = _timeReadToday

    private val _totalTimeRead = MutableLiveData<Int>()
    val totalTimeRead: LiveData<Int> = _totalTimeRead

    init {
        loadProgress()
    }

    private fun loadProgress() {
        viewModelScope.launch {
            val todayDate = DateUtils.getTodayDate()

            val pagesReadToday = withContext(Dispatchers.IO) {
                statisticsDao.getPagesReadOnDay(todayDate) ?: 0
            }

            val totalPagesRead = withContext(Dispatchers.IO) {
                statisticsDao.getTotalPagesRead()
            }

            val timeReadToday = withContext(Dispatchers.IO) {
                statisticsDao.getTimeReadToday(todayDate) ?: 0
            }

            val totalTimeRead = withContext(Dispatchers.IO) {
                statisticsDao.getTotalTimeRead()
            }

            _pagesReadToday.value = pagesReadToday
            _totalPagesRead.value = totalPagesRead
            _timeReadToday.value = timeReadToday
            _totalTimeRead.value = totalTimeRead
        }
    }

    fun resetStatistics() {
        viewModelScope.launch {
            statisticsDao.resetAllStatistics()
            _pagesReadToday.value = 0
            _totalPagesRead.value = 0
            _timeReadToday.value = 0
            _totalTimeRead.value = 0
        }
    }
}
