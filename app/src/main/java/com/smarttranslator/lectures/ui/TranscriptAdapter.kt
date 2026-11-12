package com.smarttranslator.lectures.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smarttranslator.lectures.databinding.ItemTranscriptBinding
import com.smarttranslator.lectures.db.entities.TranscriptSegment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TranscriptAdapter : RecyclerView.Adapter<TranscriptAdapter.VH>() {
    private val items = mutableListOf<TranscriptSegment>()
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun submit(list: List<TranscriptSegment>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemTranscriptBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val seg = items[position]
        holder.binding.tvTime.text = fmt.format(Date(seg.timestamp))
        holder.binding.tvText.text = seg.text
    }

    override fun getItemCount() = items.size
    class VH(val binding: ItemTranscriptBinding) : RecyclerView.ViewHolder(binding.root)
}