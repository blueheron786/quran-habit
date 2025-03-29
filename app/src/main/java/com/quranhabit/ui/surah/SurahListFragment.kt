package com.quranhabit.ui.surah

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
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
            findNavController().navigate(
                R.id.action_surahListFragment_to_quranReaderFragment,
                Bundle().apply {
                    putInt("surahNumber", surah.number)
                }
            )
        }

        surahRecyclerView.addItemDecoration(
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
                setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider)!!)
            }
        )

        surahRecyclerView.adapter = surahAdapter
    }

    private fun getSurahList(): List<Surah> = listOf(
        Surah(1, "Al-Fatihah", "الفاتحة"),
        Surah(2, "Al-Baqarah", "البقرة"),
        Surah(3, "Āli 'Imrān", "آل عمران"),
        Surah(4, "An-Nisā", "النساء"),
        Surah(5, "Al-Mā'idah", "المائدة"),
        Surah(6, "Al-An'ām", "الأنعام"),
        Surah(7, "Al-A'rāf", "الأعراف"),
        Surah(8, "Al-Anfāl", "الأنفال"),
        Surah(9, "At-Tawbah", "التوبة"),
        Surah(10, "Yūnus", "يونس"),
        Surah(11, "Hūd", "هود"),
        Surah(12, "Yūsuf", "يوسف"),
        Surah(13, "Ar-Ra'd", "الرعد"),
        Surah(14, "Ibrāhīm", "إبراهيم"),
        Surah(15, "Al-Hijr", "الحجر"),
        Surah(16, "An-Nahl", "النحل"),
        Surah(17, "Al-Isrā", "الإسراء"),
        Surah(18, "Al-Kahf", "الكهف"),
        Surah(19, "Maryam", "مريم"),
        Surah(20, "Tāhā", "طه"),
        Surah(21, "Al-Anbiyā", "الأنبياء"),
        Surah(22, "Al-Hajj", "الحج"),
        Surah(23, "Al-Mu'minūn", "المؤمنون"),
        Surah(24, "An-Nūr", "النور"),
        Surah(25, "Al-Furqān", "الفرقان"),
        Surah(26, "Ash-Shu'arā", "الشعراء"),
        Surah(27, "An-Naml", "النمل"),
        Surah(28, "Al-Qasas", "القصص"),
        Surah(29, "Al-'Ankabūt", "العنكبوت"),
        Surah(30, "Ar-Rūm", "الروم"),
        Surah(31, "Luqmān", "لقمان"),
        Surah(32, "As-Sajdah", "السجدة"),
        Surah(33, "Al-Ahzāb", "الأحزاب"),
        Surah(34, "Saba", "سبأ"),
        Surah(35, "Fātir", "فاطر"),
        Surah(36, "Yā-Sīn", "يس"),
        Surah(37, "As-Sāffāt", "الصافات"),
        Surah(38, "Sād", "ص"),
        Surah(39, "Az-Zumar", "الزمر"),
        Surah(40, "Ghāfir", "غافر"),
        Surah(41, "Fussilat", "فصلت"),
        Surah(42, "Ash-Shūrā", "الشورى"),
        Surah(43, "Az-Zukhruf", "الزخرف"),
        Surah(44, "Ad-Dukhān", "الدخان"),
        Surah(45, "Al-Jāthiyah", "الجاثية"),
        Surah(46, "Al-Ahqāf", "الأحقاف"),
        Surah(47, "Muhammad", "محمد"),
        Surah(48, "Al-Fath", "الفتح"),
        Surah(49, "Al-Hujurāt", "الحجرات"),
        Surah(50, "Qāf", "ق"),
        Surah(51, "Adh-Dhāriyāt", "الذاريات"),
        Surah(52, "At-Tūr", "الطور"),
        Surah(53, "An-Najm", "النجم"),
        Surah(54, "Al-Qamar", "القمر"),
        Surah(55, "Ar-Rahmān", "الرحمن"),
        Surah(56, "Al-Wāqi'ah", "الواقعة"),
        Surah(57, "Al-Hadīd", "الحديد"),
        Surah(58, "Al-Mujādilah", "المجادلة"),
        Surah(59, "Al-Hashr", "الحشر"),
        Surah(60, "Al-Mumtahanah", "الممتحنة"),
        Surah(61, "As-Saff", "الصف"),
        Surah(62, "Al-Jumu'ah", "الجمعة"),
        Surah(63, "Al-Munāfiqūn", "المنافقون"),
        Surah(64, "At-Taghābun", "التغابن"),
        Surah(65, "At-Talāq", "الطلاق"),
        Surah(66, "At-Tahrīm", "التحريم"),
        Surah(67, "Al-Mulk", "الملك"),
        Surah(68, "Al-Qalam", "القلم"),
        Surah(69, "Al-Hāqqah", "الحاقة"),
        Surah(70, "Al-Ma'ārij", "المعارج"),
        Surah(71, "Nūh", "نوح"),
        Surah(72, "Al-Jinn", "الجن"),
        Surah(73, "Al-Muzzammil", "المزمل"),
        Surah(74, "Al-Muddaththir", "المدثر"),
        Surah(75, "Al-Qiyāmah", "القيامة"),
        Surah(76, "Al-Insān", "الإنسان"),
        Surah(77, "Al-Mursalāt", "المرسلات"),
        Surah(78, "An-Naba", "النبأ"),
        Surah(79, "An-Nāzi'āt", "النازعات"),
        Surah(80, "'Abasa", "عبس"),
        Surah(81, "At-Takwīr", "التكوير"),
        Surah(82, "Al-Infitār", "الانفطار"),
        Surah(83, "Al-Mutaffifīn", "المطففين"),
        Surah(84, "Al-Inshiqāq", "الانشقاق"),
        Surah(85, "Al-Burūj", "البروج"),
        Surah(86, "At-Tāriq", "الطارق"),
        Surah(87, "Al-A'lā", "الأعلى"),
        Surah(88, "Al-Ghāshiyah", "الغاشية"),
        Surah(89, "Al-Fajr", "الفجر"),
        Surah(90, "Al-Balad", "البلد"),
        Surah(91, "Ash-Shams", "الشمس"),
        Surah(92, "Al-Layl", "الليل"),
        Surah(93, "Ad-Duhā", "الضحى"),
        Surah(94, "Ash-Sharh", "الشرح"),
        Surah(95, "At-Tīn", "التين"),
        Surah(96, "Al-'Alaq", "العلق"),
        Surah(97, "Al-Qadr", "القدر"),
        Surah(98, "Al-Bayyinah", "البينة"),
        Surah(99, "Az-Zalzalah", "الزلزلة"),
        Surah(100, "Al-'Ādiyāt", "العاديات"),
        Surah(101, "Al-Qāri'ah", "القارعة"),
        Surah(102, "At-Takāthur", "التكاثر"),
        Surah(103, "Al-'Asr", "العصر"),
        Surah(104, "Al-Humazah", "الهمزة"),
        Surah(105, "Al-Fīl", "الفيل"),
        Surah(106, "Quraysh", "قريش"),
        Surah(107, "Al-Mā'ūn", "الماعون"),
        Surah(108, "Al-Kawthar", "الكوثر"),
        Surah(109, "Al-Kāfirūn", "الكافرون"),
        Surah(110, "An-Nasr", "النصر"),
        Surah(111, "Al-Masad", "المسد"),
        Surah(112, "Al-Ikhlās", "الإخلاص"),
        Surah(113, "Al-Falaq", "الفلق"),
        Surah(114, "An-Nās", "الناس")
    )
}