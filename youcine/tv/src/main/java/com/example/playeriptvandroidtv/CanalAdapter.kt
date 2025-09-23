// src/main/java/com/seuprojeto/playeriptv/CanalAdapter.kt
package com.seuprojeto.playeriptv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.seuprojeto.playeriptv.data.Canal

class CanalAdapter(
    private val canais: List<Canal>,
    private val onItemClick: (Canal) -> Unit
) : RecyclerView.Adapter<CanalAdapter.CanalViewHolder>() {

    class CanalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNomeCanal: TextView = view.findViewById(R.id.tvNomeCanal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CanalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_canal, parent, false)
        return CanalViewHolder(view)
    }

    override fun onBindViewHolder(holder: CanalViewHolder, position: Int) {
        val canal = canais[position]
        holder.tvNomeCanal.text = canal.titulo
        holder.itemView.setOnClickListener {
            onItemClick(canal)
        }
    }

    override fun getItemCount() = canais.size
}