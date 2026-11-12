package com.smarttranslator.lectures.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.smarttranslator.lectures.R
import com.smarttranslator.lectures.databinding.ItemTranscriptBinding
import com.smarttranslator.lectures.db.entities.TranscriptSegment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TranscriptAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = mutableListOf<TranscriptSegment>()
    private var partialText: String? = null
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    // Теперь принимаем и финальный список, и партиал
    fun submit(list: List<TranscriptSegment>, partial: String?) {
        items.clear()
        items.addAll(list)
        partialText = partial
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return items.size + if (partialText.isNullOrBlank()) 0 else 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == items.size && !partialText.isNullOrBlank()) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val b = ItemTranscriptBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return if (viewType == 0) FinalVH(b) else PartialVH(b)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is FinalVH) {
            val seg = items[position]
            holder.binding.tvTime.text = fmt.format(Date(seg.timestamp))
            holder.binding.tvText.text = seg.text
            // Финальный текст — обычного цвета
            holder.binding.tvText.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.textPrimary))
        } else if (holder is PartialVH) {
            // У партиала нет времени, текст серый
            holder.binding.tvTime.text = ""
            holder.binding.tvText.text = partialText
            holder.binding.tvText.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.textSecondary))
        }
    }

    inner class FinalVH(val binding: ItemTranscriptBinding) : RecyclerView.ViewHolder(binding.root)
    inner class PartialVH(val binding: ItemTranscriptBinding) : RecyclerView.ViewHolder(binding.root)
}