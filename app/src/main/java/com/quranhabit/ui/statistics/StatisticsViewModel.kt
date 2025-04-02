import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranhabit.data.dao.StatisticsDao
import com.quranhabit.ui.statistics.DailyData
import com.quranhabit.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatisticsViewModel(private val statisticsDao: StatisticsDao) : ViewModel() {
    private val _pagesReadToday = MutableLiveData<Int>()
    val pagesReadToday: LiveData<Int> = _pagesReadToday

    private val _pagesReadMonth = MutableLiveData<Int>()
    val pagesReadMonth: LiveData<Int> = _pagesReadMonth

    private val _totalPagesRead = MutableLiveData<Int>()
    val totalPagesRead: LiveData<Int> = _totalPagesRead

    private val _timeReadToday = MutableLiveData<Int>()
    val timeReadToday: LiveData<Int> = _timeReadToday

    private val _totalTimeRead = MutableLiveData<Int>()
    val totalTimeRead: LiveData<Int> = _totalTimeRead

    private val _monthlyData = MutableLiveData<List<DailyData>>()
    val monthlyData: LiveData<List<DailyData>> = _monthlyData

    init {
        loadProgress()
    }

    private fun loadProgress() {
        viewModelScope.launch {
            // Text data
            val todayDate = DateUtils.getTodayDate()

            val dataForToday = withContext(Dispatchers.IO) {
                statisticsDao.getByDate(todayDate)
            }

            val dataForMonth = withContext(Dispatchers.IO) {
                statisticsDao.getTimeRangeStatsData(30)
            }

            val totalPagesRead = withContext(Dispatchers.IO) {
                statisticsDao.getTotalPagesRead()
            }

            val totalTimeRead = withContext(Dispatchers.IO) {
                statisticsDao.getTotalTimeRead()
            }

            _pagesReadToday.value = dataForToday?.pagesRead ?: 0
            // Calculate the sum of pages read in the last 30 days
            _pagesReadMonth.value = dataForMonth.sumOf { it.pagesRead }
            _timeReadToday.value = dataForToday?.secondsSpendReading ?: 0
            _totalPagesRead.value = totalPagesRead
            _totalTimeRead.value = totalTimeRead

            // Graph data
            _monthlyData.value = dataForMonth
        }
    }

    fun resetStatistics() {
        viewModelScope.launch {
            statisticsDao.resetAllStatistics()
            _pagesReadToday.value = 0
            _totalPagesRead.value = 0
            _timeReadToday.value = 0
            _totalTimeRead.value = 0
            _pagesReadMonth.value = 0 // Reset monthly count as well
            _monthlyData.value = emptyList() // Reset monthly data for the graph
        }
    }
}