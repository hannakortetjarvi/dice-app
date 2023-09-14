package com.example.gameapp

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlin.math.roundToInt

class Statistics(private val db : FirebaseFirestore, private val uid : String) {

    // Textviews for showing statistics
    private lateinit var t1 : TextView
    private lateinit var t2 : TextView
    private lateinit var t3 : TextView
    private lateinit var t4 : TextView

    /**
     * Create PopupWindow for showing the statistics
     */
    fun createPopUp(context : Context, parentView : ConstraintLayout) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.stats_popup, null)

        t1 = popupView.findViewById(R.id.textCount)
        t2 = popupView.findViewById(R.id.textStyle)
        t3 = popupView.findViewById(R.id.textPercentage)
        t4 = popupView.findViewById(R.id.textMost)

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        popupView.setOnClickListener { popupWindow.dismiss() }
        popupWindow.isFocusable = true

        // Animate the PopupWindow so that it fades in and out
        popupWindow.animationStyle = R.style.PopupAnimation

        // PopupWindow is in the middle of the screen
        popupWindow.showAtLocation(parentView, Gravity.CENTER, 0, 0)

        // Get data for the statistics view
        getData()
    }

    /**
     * Collect user's data from the firebase and turn it into statistics
     */
    private fun getData() {
        // Used statistics
        var count = 0
        val mostOf : ArrayList<Int> = ArrayList()
        val percentage : ArrayList<Int> = ArrayList()
        val favStyle : ArrayList<Int> = ArrayList()

        // Init ArrayLists
        mostOf.add(0)
        mostOf.add(0)
        mostOf.add(0)
        mostOf.add(0)
        mostOf.add(0)
        mostOf.add(0)

        percentage.add(0)
        percentage.add(0)
        percentage.add(0)
        percentage.add(0)
        percentage.add(0)
        percentage.add(0)

        favStyle.add(0)
        favStyle.add(0)
        favStyle.add(0)

        // Initialize textviews
        updateFields(0, 0, "", percentage)

        // Collect data
        db.collection(uid).orderBy("time", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    Log.d(TAG, "${document.id} => ${document.data}")

                    // Increase count every time a new document is found -> a new roll is also found
                    count++

                    // Count the numbers of the dices
                    val nums : ArrayList<Int> = document.data["numbers"] as ArrayList<Int>
                    for (num : Int in nums) {
                        mostOf[num - 1] = mostOf[num - 1] + 1
                    }

                    // count the used throwing style
                    if (document.data["style"]?.equals("Clicking recognition") == true) { favStyle[0] = favStyle[0] + 1 }
                    if (document.data["style"]?.equals("Speech recognition") == true) { favStyle[1] = favStyle[1] + 1 }
                    if (document.data["style"]?.equals("Motion recognition") == true) { favStyle[2] = favStyle[2] + 1 }
                }

                // If there is atleast one throw, collect statistics
                if (count != 0) {
                    val maxNum = mostOf.indexOf(mostOf.max()) + 1
                    val style = when (favStyle.indexOf(favStyle.max())) {
                        0 -> "Clicking"
                        1 -> "Speech"
                        else -> "Motion"
                    }

                    val sum = mostOf.sum()
                    percentage[0] = (((mostOf[0].toDouble() / sum) * 100.0).roundToInt())
                    percentage[1] = (((mostOf[1].toDouble() / sum) * 100.0).roundToInt())
                    percentage[2] = (((mostOf[2].toDouble() / sum) * 100.0).roundToInt())
                    percentage[3] = (((mostOf[3].toDouble() / sum) * 100.0).roundToInt())
                    percentage[4] = (((mostOf[4].toDouble() / sum) * 100.0).roundToInt())
                    percentage[5] = (((mostOf[5].toDouble() / sum) * 100.0).roundToInt())

                    updateFields(count, maxNum, style, percentage)
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents.", exception)
            }
    }

    /**
     * Update UI to show the collected statistics information
     */
    private fun updateFields(count : Int, maxNum: Int, style : String, percentage: ArrayList<Int>) {
        val perString = "1: ${percentage[0]}%\n2: ${percentage[1]}%\n3: ${percentage[2]}%\n4: ${percentage[3]}%\n5: ${percentage[4]}%\n6: ${percentage[5]}%"

        t1.text = count.toString()
        t2.text = style
        t3.text = perString
        t4.text = maxNum.toString()
    }

    companion object {
        private const val TAG = "Statistics"
    }
}