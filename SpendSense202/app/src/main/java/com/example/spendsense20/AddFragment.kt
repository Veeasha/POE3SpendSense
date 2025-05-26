package com.example.spendsense20

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import java.util.*

class AddFragment : Fragment() {

    private lateinit var adapterExpense: FinanceAdapter
    private lateinit var adapterIncome: FinanceAdapter
    private lateinit var adapterFiltered: FinanceAdapter

    private lateinit var databaseRef: DatabaseReference
    private var startDate: String? = null
    private var endDate: String? = null

    private val allEntries = mutableListOf<FinanceEntity>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_add, container, false)

        databaseRef = FirebaseDatabase.getInstance().getReference("finances")

        val totalBalanceTextView = view.findViewById<TextView>(R.id.totalBalanceTextView)
        val balanceSeekBar = view.findViewById<SeekBar>(R.id.balanceSeekBar)
        val saveButton = view.findViewById<Button>(R.id.saveBalanceButton)
        val editButton = view.findViewById<Button>(R.id.editBalanceButton)

        val sharedPref = requireContext().getSharedPreferences("SpendSensePrefs", 0)
        var isEditing = sharedPref.getBoolean("budget_editable", true)
        val savedBalance = sharedPref.getInt("total_balance", 0)

        totalBalanceTextView.text = "R$savedBalance"
        balanceSeekBar.progress = savedBalance
        balanceSeekBar.isEnabled = isEditing
        saveButton.visibility = if (isEditing) View.VISIBLE else View.GONE
        editButton.visibility = if (isEditing) View.GONE else View.VISIBLE

        balanceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                totalBalanceTextView.text = "R$progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        saveButton.setOnClickListener {
            val newBalance = balanceSeekBar.progress
            totalBalanceTextView.text = "R$newBalance"
            balanceSeekBar.isEnabled = false
            saveButton.visibility = View.GONE
            editButton.visibility = View.VISIBLE

            with(sharedPref.edit()) {
                putInt("total_balance", newBalance)
                putBoolean("budget_editable", false)
                apply()
            }
        }

        editButton.setOnClickListener {
            balanceSeekBar.isEnabled = true
            saveButton.visibility = View.VISIBLE
            editButton.visibility = View.GONE

            with(sharedPref.edit()) {
                putBoolean("budget_editable", true)
                apply()
            }
        }

        val recyclerViewIncome = view.findViewById<RecyclerView>(R.id.recyclerViewIncome)
        val recyclerViewExpense = view.findViewById<RecyclerView>(R.id.recyclerViewExpense)
        val recyclerViewFiltered = view.findViewById<RecyclerView>(R.id.recyclerViewFilteredExpenses)

        adapterIncome = FinanceAdapter(
            onImageClick = { imageUri ->
                startActivity(Intent(requireContext(), ImageViewActivity::class.java).apply {
                    putExtra("imageUri", imageUri)
                })
            },
            onDeleteClick = { finance ->
                databaseRef.child(finance.id).removeValue()
                allEntries.removeIf { it.id == finance.id }
                refreshAdapters()
            }
        )

        adapterExpense = FinanceAdapter(
            onImageClick = { imageUri ->
                startActivity(Intent(requireContext(), ImageViewActivity::class.java).apply {
                    putExtra("imageUri", imageUri)
                })
            },
            onDeleteClick = { finance ->
                databaseRef.child(finance.id).removeValue()
                allEntries.removeIf { it.id == finance.id }
                refreshAdapters()
            }
        )

        adapterFiltered = FinanceAdapter(
            onImageClick = { imageUri ->
                startActivity(Intent(requireContext(), ImageViewActivity::class.java).apply {
                    putExtra("imageUri", imageUri)
                })
            },
            onDeleteClick = { finance ->
                databaseRef.child(finance.id).removeValue()
                allEntries.removeIf { it.id == finance.id }
                refreshAdapters()
            }
        )

        recyclerViewIncome.adapter = adapterIncome
        recyclerViewExpense.adapter = adapterExpense
        recyclerViewFiltered.adapter = adapterFiltered

        recyclerViewIncome.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewExpense.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewFiltered.layoutManager = LinearLayoutManager(requireContext())

        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allEntries.clear()
                for (child in snapshot.children) {
                    val finance = child.getValue(FinanceEntity::class.java)
                    if (finance != null) {
                        allEntries.add(finance)
                    }
                }
                refreshAdapters()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AddFragment", "Database error: ${error.message}")
            }
        })

        val btnStartDate = view.findViewById<Button>(R.id.btnStartDate)
        val btnEndDate = view.findViewById<Button>(R.id.btnEndDate)
        val btnShowSummary = view.findViewById<Button>(R.id.btnShowSummary)
        val tvSummary = view.findViewById<TextView>(R.id.tvCategorySummary)

        btnStartDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                startDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                btnStartDate.text = "From: $startDate"
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnEndDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                endDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                btnEndDate.text = "To: $endDate"
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnShowSummary.setOnClickListener {
            if (startDate != null && endDate != null) {
                val summary = allEntries.filter {
                    it.date >= startDate!! && it.date <= endDate!! && it.type.equals("expense", ignoreCase = true)
                }.groupBy { it.name }
                    .mapValues { (_, entries) -> entries.sumOf { it.amount } }

                val summaryText = summary.entries.joinToString("\n") { "${it.key}: R${it.value}" }
                tvSummary.text = if (summaryText.isNotBlank()) summaryText else "No category totals found in the selected period."
                tvSummary.visibility = View.VISIBLE
                recyclerViewFiltered.visibility = View.VISIBLE

                databaseRef.get().addOnSuccessListener { snapshot ->
                    val filteredList = mutableListOf<FinanceEntity>()
                    for (item in snapshot.children) {
                        val finance = item.getValue(FinanceEntity::class.java)
                        if (
                            finance != null &&
                            finance.date.isNotEmpty() &&
                            finance.type.equals("expense", ignoreCase = true) &&
                            finance.date >= startDate!! && finance.date <= endDate!!
                        ) {
                            finance.id = item.key ?: ""
                            filteredList.add(finance)
                        }
                    }
                    adapterFiltered.submitList(filteredList)
                }.addOnFailureListener {
                    Log.e("AddFragment", "Failed to load filtered data: ${it.message}")
                }
            } else {
                tvSummary.text = "Please select both dates."
                tvSummary.visibility = View.VISIBLE
                recyclerViewFiltered.visibility = View.GONE
            }
        }

        view.findViewById<Button>(R.id.addExpenseButton).setOnClickListener {
            startActivity(Intent(requireContext(), CameraPage::class.java))
        }

        view.findViewById<Button>(R.id.addIncomeButton).setOnClickListener {
            startActivity(Intent(requireContext(), CameraPage::class.java))
        }

        view.findViewById<ImageButton>(R.id.btnCalculator).setOnClickListener {
            startActivity(Intent(requireContext(), CalculatorPage::class.java))
        }

        return view
    }

    private fun refreshAdapters() {
        adapterIncome.submitList(allEntries.filter { it.type.equals("income", ignoreCase = true) })
        adapterExpense.submitList(allEntries.filter { it.type.equals("expense", ignoreCase = true) })
        // Removed refreshFilteredData()
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AddFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }

        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"
    }
}
