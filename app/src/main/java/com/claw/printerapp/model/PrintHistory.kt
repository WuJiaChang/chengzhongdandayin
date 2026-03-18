package com.claw.printerapp.model

/**
 * 打印历史记录数据类
 */
data class PrintHistory(
    val id: Long = 0,
    val sequenceNumber: Int,
    val date: String,
    val time: String,
    val carNumber: String,
    val squareWeight: String,
    val grossWeight: String,
    val tareWeight: String,
    val netWeight: String,
    val timestamp: Long = System.currentTimeMillis()
)

