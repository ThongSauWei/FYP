package com.example.fyp

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FilterPost : Fragment() {

    private val selectedCategories = mutableSetOf<String>() // Store selected categories
    private var selectedDateRange: Pair<Long?, Long?>? = null
    private var startDate: Long? = null // Start date
    private var endDate: Long? = null // End date
    private var selectedCard: CardView? = null

    private val cardToTextViewMap = mapOf(
        R.id.allCard to R.id.tvAll,
        R.id.bussCard to R.id.tvBuss,
        R.id.sciCard to R.id.tvSci,
        R.id.artCard to R.id.tvArt,
        R.id.itCard to R.id.tvIt,
        R.id.sportCard to R.id.tvSport,
        R.id.langCard to R.id.tvLang,
        R.id.assCard to R.id.tvAss,
        R.id.revisionCardCard to R.id.tvRevision,
        R.id.studyBuddyCard to R.id.tvStudyBuddy,
        R.id.helperCard to R.id.tvHelper,
        R.id.otherCard to R.id.tvOther,
        R.id.fewDayCard to R.id.tvFewDay,
        R.id.weekCard to R.id.tvWeek,
        R.id.monthCard to R.id.tvMonth
    )

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_filter_post, container, false)

        //toolbar setting
        (activity as MainActivity).setToolbar(R.layout.toolbar_with_annouce_and_title)
        val titleTextView = activity?.findViewById<TextView>(R.id.titleTextView)
        titleTextView?.text = ""

        val navIcon = activity?.findViewById<ImageView>(R.id.navIcon)
        navIcon?.setImageResource(R.drawable.baseline_arrow_back_ios_24) // Set the navigation icon
        navIcon?.setOnClickListener { activity?.onBackPressed() } // Set click behavior

        val btnNotification = activity?.findViewById<ImageView>(R.id.btnNotification)
        btnNotification?.visibility = View.GONE

        val btnSearchToolbarWithAnnouce = activity?.findViewById<ImageView>(R.id.btnSearchToolbarWithAnnouce)
        btnSearchToolbarWithAnnouce?.visibility = View.GONE

        // Example categories mapping
        val categoryMap = mapOf(
            R.id.bussCard to "Business",
            R.id.sciCard to "Science",
            R.id.artCard to "Art / Design",
            R.id.itCard to "IT",
            R.id.sportCard to "Sports",
            R.id.langCard to "Languages",
            R.id.assCard to "Assignments",
            R.id.revisionCardCard to "Revision",
            R.id.studyBuddyCard to "Study Buddy",
            R.id.helperCard to "Helper",
            R.id.otherCard to "Other"
        )

        // Handle category card selection
        categoryMap.forEach { (cardId, categoryName) ->
            val card = view.findViewById<CardView>(cardId)
            val textView = view.findViewById<TextView>(cardToTextViewMap[cardId] ?: return@forEach)

            card.setOnClickListener {
                if (selectedCategories.contains(categoryName)) {
                    selectedCategories.remove(categoryName)
                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.box))
                    textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                } else {
                    selectedCategories.add(categoryName)
                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_selected))
                    textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.selected_category))
                }
            }
        }

        // Attach click listeners for date range cards
        view.findViewById<CardView>(R.id.fewDayCard).setOnClickListener { setDateRange(R.id.fewDayCard, 3 * 24 * 60 * 60 * 1000L, null) }
        view.findViewById<CardView>(R.id.weekCard).setOnClickListener { setDateRange(R.id.weekCard, 7 * 24 * 60 * 60 * 1000L, null) }
        view.findViewById<CardView>(R.id.monthCard).setOnClickListener { setDateRange(R.id.monthCard, 30 * 24 * 60 * 60 * 1000L, null) }

        // Handle custom date range input
        handleDateInputs(view)

        // Handle filter button click
        view.findViewById<Button>(R.id.btnFilterFilterPost).setOnClickListener {
            applyFilters()
        }

        return view
    }

    private fun handleDateInputs(view: View) {
        val editTextDate = view.findViewById<EditText>(R.id.editTextDate)
        val calendarFromDate = view.findViewById<ImageView>(R.id.calenderFromDate)
        val editTextUntil = view.findViewById<EditText>(R.id.editTextUntil)
        val calenderUntilDate = view.findViewById<ImageView>(R.id.calenderUntilDate)

        val calendar = Calendar.getInstance()

        fun showDatePicker(isStartDate: Boolean) {
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(year, month, dayOfMonth)
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    if (isStartDate) {
                        startDate = selectedDate.timeInMillis
                        editTextDate.setText(dateFormat.format(selectedDate.time))
                    } else {
                        endDate = selectedDate.timeInMillis
                        editTextUntil.setText(dateFormat.format(selectedDate.time))
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        editTextDate.setOnClickListener { showDatePicker(isStartDate = true) }
        calendarFromDate.setOnClickListener { showDatePicker(isStartDate = true) }
        editTextUntil.setOnClickListener { showDatePicker(isStartDate = false) }
        calenderUntilDate.setOnClickListener { showDatePicker(isStartDate = false) }
    }

    private fun setDateRange(cardId: Int, startOffset: Long?, endOffset: Long?) {
        val cards = listOf(R.id.fewDayCard, R.id.weekCard, R.id.monthCard)
        val selectedCardView = view?.findViewById<CardView>(cardId)

        // Deselect all cards
        cards.forEach { id ->
            val card = view?.findViewById<CardView>(id)
            card?.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.box))
            val textView = view?.findViewById<TextView>(cardToTextViewMap[id] ?: return@forEach)
            textView?.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        }

        // Select the clicked card
        selectedCardView?.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_selected))
        val selectedTextView = cardToTextViewMap[cardId]?.let { view?.findViewById<TextView>(it) }
        selectedTextView?.setTextColor(ContextCompat.getColor(requireContext(), R.color.selected_category))

        val currentTime = System.currentTimeMillis()
        selectedDateRange = Pair(
            startOffset?.let { currentTime - it },
            endOffset?.let { currentTime }
        )
    }

    private fun applyFilters() {
        val filteredCategories = ArrayList(selectedCategories)
        val filterStartDate = selectedDateRange?.first ?: startDate
        val filterEndDate = selectedDateRange?.second ?: endDate

        val fragment = Home()
        val bundle = Bundle()
        bundle.putStringArrayList("selectedCategories", filteredCategories)
        bundle.putSerializable("selectedDateRange", Pair(filterStartDate, filterEndDate))
        fragment.arguments = bundle

        val fragmentManager = (context as FragmentActivity).supportFragmentManager
        fragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, fragment)
            .addToBackStack(null)
            .commit()
    }
}

