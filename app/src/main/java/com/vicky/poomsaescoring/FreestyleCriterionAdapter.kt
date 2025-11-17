package com.vicky.poomsaescoring

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.recyclerview.widget.RecyclerView
import com.vicky.poomsaescoring.databinding.ItemFreestyleCriterionBinding

class FreestyleCriterionAdapter(
    private val items: List<FreestyleCriterion>,
    private val onScoreChanged: () -> Unit
) : RecyclerView.Adapter<FreestyleCriterionAdapter.VH>() {

    inner class VH(val binding: ItemFreestyleCriterionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemFreestyleCriterionBinding.inflate(inflater, parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        with(holder.binding) {
            tvLabel.text = item.label
            tvMax.text = String.format("Max %.1f", item.maxScore)

            // SeekBar is 0–maxScore in 0.1 steps
            val maxProgress = (item.maxScore * 10).toInt()
            sbScore.max = maxProgress
//            sxbScore.progress = (item.score * 10).toInt()
            tvCurrent.text = String.format("%.1f", item.score)

            sbScore.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val value = progress / 10.0
                    item.score = value
                    tvCurrent.text = String.format("%.1f", value)
                    onScoreChanged()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
    }

    override fun getItemCount(): Int = items.size
}
