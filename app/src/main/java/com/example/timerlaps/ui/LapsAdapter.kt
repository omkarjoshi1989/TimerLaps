package com.example.timerlaps.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Color // For alternating colors
import com.example.timerlaps.Lap
import com.example.timerlaps.R

class LapsAdapter(private val laps: MutableList<Lap>) : RecyclerView.Adapter<LapsAdapter.LapViewHolder>() {

    class LapViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvLapNumber: TextView = itemView.findViewById(R.id.tv_lap_number)
        val tvLapTimeDuration: TextView = itemView.findViewById(R.id.tv_lap_time_duration)
        val tvLapTotalTime: TextView = itemView.findViewById(R.id.tv_lap_total_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LapViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_lap, parent, false)
        return LapViewHolder(view)
    }

    override fun onBindViewHolder(holder: LapViewHolder, position: Int) {
        // We want to show laps in reverse order (newest at top)
        val lap = laps[laps.size - 1 - position] // Access from end of list

        holder.tvLapNumber.text = "Namaskar ${lap.lapNumber}"
        holder.tvLapTimeDuration.text = formatTime(lap.lapDurationMillis)
        holder.tvLapTotalTime.text = formatTime(lap.totalTimeAtLapMillis)

        // Alternate background color for better readability
        if (position % 2 == 0) {
            holder.itemView.setBackgroundColor(Color.parseColor("#F5F5F5")) // Light gray
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE)
        }
    }

    override fun getItemCount(): Int = laps.size

    // Helper function to format milliseconds into HH:MM:SS:ms
    private fun formatTime(millis: Long): String {
        val minutes = (millis / (1000 * 60)) % 60
        val seconds = (millis / 1000) % 60
        val hundredths = (millis % 1000) / 10 // Get hundredths of a second

        return String.format("%02d:%02d:%02d", minutes, seconds, hundredths)
    }

    fun addLap(lap: Lap) {
        laps.add(lap)
        notifyItemInserted(0) // Notify adapter that an item was inserted at the top
    }

    fun clearLaps() {
        laps.clear()
        notifyDataSetChanged() // Notify adapter that the entire dataset has changed
    }
}