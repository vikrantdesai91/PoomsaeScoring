package com.vicky.poomsaescoring.adapter

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import com.vicky.poomsaescoring.R

class ScoreAdapter(
    private val context: Context,
    private val scoreValues: List<Double>, // List of possible score values
    private val onScoreSelected: (Double) -> Unit // Callback for score selection
) : ArrayAdapter<Double>(context, 0, scoreValues) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val button: Button
        if (convertView == null) {
            button = Button(context)
            button.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dpToPx(40)
            ).apply {
                rightMargin = dpToPx(4)
                gravity = Gravity.CENTER_VERTICAL
            }
        } else {
            button = convertView as Button
        }

        val score = scoreValues[position]
        button.text = formatScore(score)
        button.setTextColor(context.resources.getColor(R.color.neutral_dark_4, null))
        button.setBackgroundResource(R.drawable.score_button_bg)

        // Handle button click
        button.setOnClickListener {
            onScoreSelected(score)
        }

        return button
    }

    // Helper to convert dp to px
    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    // Format the score to one decimal place
    private fun formatScore(score: Double): String {
        return String.format("%.1f", score)
    }
}
