package com.claw.printerapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.claw.printerapp.R
import com.claw.printerapp.model.CarTareInfo

class CarTareAdapter(
    private var carTareList: MutableList<CarTareInfo>,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<CarTareAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCarNumber: TextView = itemView.findViewById(R.id.tvCarNumber)
        val tvTareWeight: TextView = itemView.findViewById(R.id.tvTareWeight)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_car_tare, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val carTare = carTareList[position]
        holder.tvCarNumber.text = carTare.carNumber
        holder.tvTareWeight.text = "皮重: ${carTare.tareWeight} kg"

        holder.btnDelete.setOnClickListener {
            onDeleteClick(carTare.carNumber)
        }
    }

    override fun getItemCount(): Int = carTareList.size

    fun updateList(newList: MutableList<CarTareInfo>) {
        carTareList = newList
        notifyDataSetChanged()
    }
}
