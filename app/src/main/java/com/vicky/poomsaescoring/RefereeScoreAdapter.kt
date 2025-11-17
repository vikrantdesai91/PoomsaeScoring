package com.vicky.poomsaescoring

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vicky.poomsaescoring.databinding.ItemRefereeScoreBinding

class RefereeScoreAdapter : RecyclerView.Adapter<RefereeScoreAdapter.VH>() {

    private val items = mutableListOf<RefereeScore>()
    private var highestTotal: Double? = null
    private var lowestTotal: Double? = null

    fun submitList(list: List<RefereeScore>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemRefereeScoreBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRefereeScoreBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvRefName.text = item.name
        holder.binding.tvRefScorePlayer1.text = String.format("%.3f", item.player1Total)
        holder.binding.tvRefScorePlayer2.text = String.format("%.3f", item.player2Total)

    }

    override fun getItemCount(): Int = items.size
}

