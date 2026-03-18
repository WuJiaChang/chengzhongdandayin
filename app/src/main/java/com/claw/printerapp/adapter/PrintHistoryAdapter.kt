package com.claw.printerapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.claw.printerapp.R
import com.claw.printerapp.model.PrintHistory
import java.text.SimpleDateFormat
import java.util.*

/**
 * 打印历史记录适配器
 */
class PrintHistoryAdapter(
    private var printHistoryList: List<PrintHistory>,
    private val onDelete: (PrintHistory) -> Unit,
    private val onReprint: (PrintHistory) -> Unit
) : RecyclerView.Adapter<PrintHistoryAdapter.PrintHistoryViewHolder>() {

    inner class PrintHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSequenceNumber: TextView = itemView.findViewById(R.id.tvSequenceNumber)
        val tvDateTime: TextView = itemView.findViewById(R.id.tvDateTime)
        val tvCarNumber: TextView = itemView.findViewById(R.id.tvCarNumber)
        val tvWeights: TextView = itemView.findViewById(R.id.tvWeights)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
        val btnReprint: Button = itemView.findViewById(R.id.btnReprint)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrintHistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_print_history, parent, false)
        return PrintHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: PrintHistoryViewHolder, position: Int) {
        val printHistory = printHistoryList[position]

        // 序号
        holder.tvSequenceNumber.text = String.format("%04d", printHistory.sequenceNumber)

        // 日期时间
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val fullDateTime = "${printHistory.date} ${printHistory.time}"
        holder.tvDateTime.text = fullDateTime

        // 车号
        holder.tvCarNumber.text = "车号: ${printHistory.carNumber}"

        // 重量信息
        holder.tvWeights.text = "毛重: ${printHistory.grossWeight} | 皮重: ${printHistory.tareWeight} | 净重: ${printHistory.netWeight}"

        // 删除按钮
        holder.btnDelete.setOnClickListener {
            onDelete(printHistory)
        }

        // 重打按钮
        holder.btnReprint.setOnClickListener {
            onReprint(printHistory)
        }
    }

    override fun getItemCount(): Int = printHistoryList.size

    /**
     * 更新列表数据
     */
    fun updateList(newList: List<PrintHistory>) {
        printHistoryList = newList
        notifyDataSetChanged()
    }
}
