import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quranhabit.data.dao.ReadingSessionDao
import com.quranhabit.data.entity.ReadingSession
import com.quranhabit.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReadingProgressViewModel(private val readingSessionDao: ReadingSessionDao) : ViewModel() {
    private val _todayProgress = MutableLiveData<Int>()
    val todayProgress: LiveData<Int> = _todayProgress

    private val _totalProgress = MutableLiveData<Int>()
    val totalProgress: LiveData<Int> = _totalProgress

    init {
        loadProgress()
    }

    private fun loadProgress() {
        viewModelScope.launch {
            val todayDate = DateUtils.getTodayDate()

            val todaysProgress = withContext(Dispatchers.IO) {
                readingSessionDao.getDaysProgress(todayDate) ?: 0
            }

            val totalProgress = withContext(Dispatchers.IO) {
                readingSessionDao.getTotalPagesRead()
            }

            _todayProgress.value = todaysProgress
            _totalProgress.value = totalProgress
        }
    }

    fun addTestData() {
        viewModelScope.launch {
            try {
                val todayDate = DateUtils.getTodayDate()
                withContext(Dispatchers.IO) {
                    // Either use the simple upsert
                    readingSessionDao.upsert(
                        ReadingSession(
                            date = todayDate,
                            pagesRead = 99
                        )
                    )
                }
                loadProgress() // Refresh the view
            } catch (e: Exception) {
                Log.e("Failed to save: ${e.message}", e.toString())
            }
        }
    }

    fun resetStatistics() {
        viewModelScope.launch {
            readingSessionDao.resetAllStatistics()
            _todayProgress.value = 0
            _totalProgress.value = 0
        }
    }
}