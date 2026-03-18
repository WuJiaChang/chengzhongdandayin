package com.claw.printerapp.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.claw.printerapp.model.PrintHistory

/**
 * 打印历史数据库管理类
 */
class PrintHistoryDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "printHistory.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "print_history"

        private const val COLUMN_ID = "id"
        private const val COLUMN_SEQUENCE_NUMBER = "sequence_number"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_TIME = "time"
        private const val COLUMN_CAR_NUMBER = "car_number"
        private const val COLUMN_GROSS_WEIGHT = "gross_weight"
        private const val COLUMN_TARE_WEIGHT = "tare_weight"
        private const val COLUMN_NET_WEIGHT = "net_weight"
        private const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_SEQUENCE_NUMBER INTEGER NOT NULL,
                $COLUMN_DATE TEXT NOT NULL,
                $COLUMN_TIME TEXT NOT NULL,
                $COLUMN_CAR_NUMBER TEXT NOT NULL,
                $COLUMN_GROSS_WEIGHT TEXT NOT NULL,
                $COLUMN_TARE_WEIGHT TEXT NOT NULL,
                $COLUMN_NET_WEIGHT TEXT NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL
            )
        """.trimIndent()
        db?.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    /**
     * 插入打印历史记录
     */
    fun insert(printHistory: PrintHistory): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SEQUENCE_NUMBER, printHistory.sequenceNumber)
            put(COLUMN_DATE, printHistory.date)
            put(COLUMN_TIME, printHistory.time)
            put(COLUMN_CAR_NUMBER, printHistory.carNumber)
            put(COLUMN_GROSS_WEIGHT, printHistory.grossWeight)
            put(COLUMN_TARE_WEIGHT, printHistory.tareWeight)
            put(COLUMN_NET_WEIGHT, printHistory.netWeight)
            put(COLUMN_TIMESTAMP, printHistory.timestamp)
        }

        return try {
            val rows = db.insert(TABLE_NAME, null, values)
            rows != -1L
        } catch (e: Exception) {
            false
        } finally {
            db.close()
        }
    }

    /**
     * 获取所有打印历史记录（按时间倒序）
     */
    fun getAllPrintHistory(): List<PrintHistory> {
        val list = mutableListOf<PrintHistory>()
        val db = readableDatabase

        val cursor: Cursor = db.query(
            TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP DESC"
        )

        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val sequenceNumber = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SEQUENCE_NUMBER))
            val date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE))
            val time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME))
            val carNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAR_NUMBER))
            val grossWeight = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GROSS_WEIGHT))
            val tareWeight = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TARE_WEIGHT))
            val netWeight = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NET_WEIGHT))
            val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))

            list.add(PrintHistory(id, sequenceNumber, date, time, carNumber, grossWeight, tareWeight, netWeight, timestamp))
        }
        cursor.close()
        db.close()

        return list
    }

    /**
     * 根据ID删除打印历史记录
     */
    fun delete(id: Long): Boolean {
        val db = writableDatabase
        val selection = "$COLUMN_ID = ?"
        val selectionArgs = arrayOf(id.toString())

        return try {
            val rows = db.delete(TABLE_NAME, selection, selectionArgs)
            rows > 0
        } catch (e: Exception) {
            false
        } finally {
            db.close()
        }
    }

    /**
     * 清空所有打印历史记录
     */
    fun clearAll(): Boolean {
        val db = writableDatabase
        return try {
            db.delete(TABLE_NAME, null, null) > 0
        } catch (e: Exception) {
            false
        } finally {
            db.close()
        }
    }
}
