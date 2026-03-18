package com.claw.printerapp.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.claw.printerapp.model.CarTareInfo

/**
 * 车号-皮重数据库管理类
 */
class CarTareDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "carTare.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "car_tare"

        private const val COLUMN_ID = "id"
        private const val COLUMN_CAR_NUMBER = "car_number"
        private const val COLUMN_TARE_WEIGHT = "tare_weight"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CAR_NUMBER TEXT NOT NULL UNIQUE,
                $COLUMN_TARE_WEIGHT REAL NOT NULL
            )
        """.trimIndent()
        db?.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    /**
     * 插入或更新车号-皮重信息
     */
    fun insertOrUpdate(carTareInfo: CarTareInfo): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CAR_NUMBER, carTareInfo.carNumber)
            put(COLUMN_TARE_WEIGHT, carTareInfo.tareWeight)
        }

        return try {
            val rows = db.insertWithOnConflict(
                TABLE_NAME,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
            rows != -1L
        } catch (e: Exception) {
            false
        } finally {
            db.close()
        }
    }

    /**
     * 根据车号查询皮重
     */
    fun getTareByCarNumber(carNumber: String): Double? {
        val db = readableDatabase
        val selection = "$COLUMN_CAR_NUMBER = ?"
        val selectionArgs = arrayOf(carNumber)

        val cursor = db.query(
            TABLE_NAME,
            arrayOf(COLUMN_TARE_WEIGHT),
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        var tareWeight: Double? = null
        if (cursor.moveToFirst()) {
            tareWeight = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TARE_WEIGHT))
        }
        cursor.close()
        db.close()

        return tareWeight
    }

    /**
     * 获取所有车号-皮重信息
     */
    fun getAllCarTareInfo(): List<CarTareInfo> {
        val list = mutableListOf<CarTareInfo>()
        val db = readableDatabase

        val cursor: Cursor = db.query(
            TABLE_NAME,
            arrayOf(COLUMN_CAR_NUMBER, COLUMN_TARE_WEIGHT),
            null,
            null,
            null,
            null,
            COLUMN_CAR_NUMBER
        )

        while (cursor.moveToNext()) {
            val carNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAR_NUMBER))
            val tareWeight = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TARE_WEIGHT))
            list.add(CarTareInfo(carNumber, tareWeight))
        }
        cursor.close()
        db.close()

        return list
    }

    /**
     * 删除车号-皮重信息
     */
    fun delete(carNumber: String): Boolean {
        val db = writableDatabase
        val selection = "$COLUMN_CAR_NUMBER = ?"
        val selectionArgs = arrayOf(carNumber)

        return try {
            val rows = db.delete(TABLE_NAME, selection, selectionArgs)
            rows > 0
        } catch (e: Exception) {
            false
        } finally {
            db.close()
        }
    }
}
