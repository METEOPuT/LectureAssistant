package com.smarttranslator.lectures.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smarttranslator.lectures.databinding.ItemLectureBinding
import com.smarttranslator.lectures.db.entities.Lecture
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LectureListAdapter(
    private val onPick: (Lecture) -> Unit,
    private val onDelete: (Lecture) -> Unit
) : RecyclerView.Adapter<LectureListAdapter.VH>() {
    private val items = mutableListOf<Lecture>()
    private val fmt = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())

    fun submit(list: List<Lecture>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemLectureBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val lec = items[position]
        holder.binding.tvTitle.text = lec.title
        holder.binding.tvDate.text = fmt.format(Date(lec.startedAt))

        // Обработка клика по самой лекции
        holder.binding.root.setOnClickListener { onPick(lec) }

        // Обработка клика по кнопке удаления
        holder.binding.btnDelete.setOnClickListener { onDelete(lec) }
    }

    override fun getItemCount() = items.size
    class VH(val binding: ItemLectureBinding) : RecyclerView.ViewHolder(binding.root)
}