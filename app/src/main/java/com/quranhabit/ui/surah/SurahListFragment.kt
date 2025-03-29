package com.quranhabit.ui.surah

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.quranhabit.R

class SurahListFragment : Fragment() {

    private lateinit var surahRecyclerView: RecyclerView
    private lateinit var surahAdapter: SurahAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_surah_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        surahRecyclerView = view.findViewById(R.id.surahRecyclerView)
        surahRecyclerView.layoutManager = LinearLayoutManager(context)

        val surahList = getSurahList()
        surahAdapter = SurahAdapter(surahList) { surah ->
            // Manual navigation (no Directions class required)
            findNavController().navigate(
                R.id.action_surahListFragment_to_quranReaderFragment,
                Bundle().apply { putInt("surahNumber", surah.number) }
            )
        }

        surahRecyclerView.adapter = surahAdapter
    }

    private fun getSurahList(): List<Surah> = listOf(
        Surah(1, "Al-Fatihah"),
        Surah(2, "Al-Baqarah"),
        Surah(3, "Āli 'Imrān"),
        Surah(4, "An-Nisā"),
        Surah(5, "Al-Mā'idah"),
        Surah(6, "Al-An'ām"),
        Surah(7, "Al-A'rāf"),
        Surah(8, "Al-Anfāl"),
        Surah(9, "At-Tawbah"),
        Surah(10, "Yūnus"),
        Surah(11, "Hūd"),
        Surah(12, "Yūsuf"),
        Surah(13, "Ar-Ra'd"),
        Surah(14, "Ibrāhīm"),
        Surah(15, "Al-Hijr"),
        Surah(16, "An-Nahl"),
        Surah(17, "Al-Isrā"),
        Surah(18, "Al-Kahf"),
        Surah(19, "Maryam"),
        Surah(20, "Tāhā"),
        Surah(21, "Al-Anbiyā"),
        Surah(22, "Al-Hajj"),
        Surah(23, "Al-Mu'minūn"),
        Surah(24, "An-Nūr"),
        Surah(25, "Al-Furqān"),
        Surah(26, "Ash-Shu'arā"),
        Surah(27, "An-Naml"),
        Surah(28, "Al-Qasas"),
        Surah(29, "Al-'Ankabūt"),
        Surah(30, "Ar-Rūm"),
        Surah(31, "Luqmān"),
        Surah(32, "As-Sajdah"),
        Surah(33, "Al-Ahzāb"),
        Surah(34, "Saba"),
        Surah(35, "Fātir"),
        Surah(36, "Yā-Sīn"),
        Surah(37, "As-Sāffāt"),
        Surah(38, "Sād"),
        Surah(39, "Az-Zumar"),
        Surah(40, "Ghāfir"),
        Surah(41, "Fussilat"),
        Surah(42, "Ash-Shūrā"),
        Surah(43, "Az-Zukhruf"),
        Surah(44, "Ad-Dukhān"),
        Surah(45, "Al-Jāthiyah"),
        Surah(46, "Al-Ahqāf"),
        Surah(47, "Muhammad"),
        Surah(48, "Al-Fath"),
        Surah(49, "Al-Hujurāt"),
        Surah(50, "Qāf"),
        Surah(51, "Adh-Dhāriyāt"),
        Surah(52, "At-Tūr"),
        Surah(53, "An-Najm"),
        Surah(54, "Al-Qamar"),
        Surah(55, "Ar-Rahmān"),
        Surah(56, "Al-Wāqi'ah"),
        Surah(57, "Al-Hadīd"),
        Surah(58, "Al-Mujādilah"),
        Surah(59, "Al-Hashr"),
        Surah(60, "Al-Mumtahanah"),
        Surah(61, "As-Saff"),
        Surah(62, "Al-Jumu'ah"),
        Surah(63, "Al-Munāfiqūn"),
        Surah(64, "At-Taghābun"),
        Surah(65, "At-Talāq"),
        Surah(66, "At-Tahrīm"),
        Surah(67, "Al-Mulk"),
        Surah(68, "Al-Qalam"),
        Surah(69, "Al-Hāqqah"),
        Surah(70, "Al-Ma'ārij"),
        Surah(71, "Nūh"),
        Surah(72, "Al-Jinn"),
        Surah(73, "Al-Muzzammil"),
        Surah(74, "Al-Muddaththir"),
        Surah(75, "Al-Qiyāmah"),
        Surah(76, "Al-Insān"),
        Surah(77, "Al-Mursalāt"),
        Surah(78, "An-Naba"),
        Surah(79, "An-Nāzi'āt"),
        Surah(80, "'Abasa"),
        Surah(81, "At-Takwīr"),
        Surah(82, "Al-Infitār"),
        Surah(83, "Al-Mutaffifīn"),
        Surah(84, "Al-Inshiqāq"),
        Surah(85, "Al-Burūj"),
        Surah(86, "At-Tāriq"),
        Surah(87, "Al-A'lā"),
        Surah(88, "Al-Ghāshiyah"),
        Surah(89, "Al-Fajr"),
        Surah(90, "Al-Balad"),
        Surah(91, "Ash-Shams"),
        Surah(92, "Al-Layl"),
        Surah(93, "Ad-Duhā"),
        Surah(94, "Ash-Sharh"),
        Surah(95, "At-Tīn"),
        Surah(96, "Al-'Alaq"),
        Surah(97, "Al-Qadr"),
        Surah(98, "Al-Bayyinah"),
        Surah(99, "Az-Zalzalah"),
        Surah(100, "Al-'Ādiyāt"),
        Surah(101, "Al-Qāri'ah"),
        Surah(102, "At-Takāthur"),
        Surah(103, "Al-'Asr"),
        Surah(104, "Al-Humazah"),
        Surah(105, "Al-Fīl"),
        Surah(106, "Quraysh"),
        Surah(107, "Al-Mā'ūn"),
        Surah(108, "Al-Kawthar"),
        Surah(109, "Al-Kāfirūn"),
        Surah(110, "An-Nasr"),
        Surah(111, "Al-Masad"),
        Surah(112, "Al-Ikhlās"),
        Surah(113, "Al-Falaq"),
        Surah(114, "An-Nās")
    )
}