package com.example.spendsense20

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.*

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val databaseRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("finances")

    // Insert a new finance entry
    fun insertFinance(finance: FinanceEntity) {
        val id = databaseRef.push().key ?: return
        finance.id = id
        databaseRef.child(id).setValue(finance)
    }

    // Observe finances for a specific date
    fun getFinanceByDate(selectedDate: String): LiveData<List<FinanceEntity>> {
        val liveData = MutableLiveData<List<FinanceEntity>>()

        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val filteredList = mutableListOf<FinanceEntity>()
                for (child in snapshot.children) {
                    val item = child.getValue(FinanceEntity::class.java)
                    if (item?.date == selectedDate) {
                        filteredList.add(item)
                    }
                }
                liveData.postValue(filteredList)
            }

            override fun onCancelled(error: DatabaseError) {
                // You can log or handle this as needed
                liveData.postValue(emptyList())
            }
        })

        return liveData
    }

    // Get total amount per category between dates
    fun getCategoryTotals(startDate: String, endDate: String): LiveData<List<CategorySummary>> {
        val liveData = MutableLiveData<List<CategorySummary>>()

        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val categoryMap = mutableMapOf<String, Double>()
                for (child in snapshot.children) {
                    val item = child.getValue(FinanceEntity::class.java) ?: continue
                    if (item.date >= startDate && item.date <= endDate) {
                        val amount = item.amount
                        val category = item.name
                        categoryMap[category] = categoryMap.getOrDefault(category, 0.0) + amount
                    }
                }

                val summaryList = categoryMap.map { (category, total) ->
                    CategorySummary(category, total)
                }

                liveData.postValue(summaryList)
            }

            override fun onCancelled(error: DatabaseError) {
                liveData.postValue(emptyList())
            }
        })

        return liveData
    }
}
