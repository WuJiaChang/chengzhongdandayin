package com.claw.printerapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.claw.printerapp.adapter.BluetoothDeviceAdapter
import com.claw.printerapp.adapter.CarTareAdapter
import com.claw.printerapp.adapter.PrintHistoryAdapter
import com.claw.printerapp.bluetooth.BluetoothManager
import com.claw.printerapp.database.CarTareDatabase
import com.claw.printerapp.database.PrintHistoryDatabase
import com.claw.printerapp.model.CarTareInfo
import com.claw.printerapp.model.PrintHistory
import com.claw.printerapp.print.EscPosPrinter
import com.google.android.material.textfield.TextInputEditText
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var carTareDatabase: CarTareDatabase
    private lateinit var printHistoryDatabase: PrintHistoryDatabase
    private lateinit var escPosPrinter: EscPosPrinter

    private lateinit var tvBluetoothStatus: TextView
    private lateinit var btnConnectBluetooth: Button
    private lateinit var etCarNumber: TextInputEditText
    private lateinit var etTareWeight: TextInputEditText
    private lateinit var etSquareWeight: TextInputEditText
    private lateinit var tvNetWeight: TextView
    private lateinit var tvGrossWeight: TextView
    private lateinit var etDate: TextInputEditText
    private lateinit var etTime: TextInputEditText
    private lateinit var btnManageCarTare: Button
    private lateinit var btnPrintHistory: Button
    private lateinit var btnPrint: Button
    private lateinit var tvSequenceNumber: TextView

    private lateinit var carTareAdapter: CarTareAdapter
    private lateinit var printHistoryAdapter: PrintHistoryAdapter

    private val REQUEST_PERMISSION_CODE = 1001
    private var printSequenceNumber = 1  // 每日打印序号
    private var lastPrintDate = ""  // 上次打印的日期

    // 日期选择器
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initManagers()
        checkPermissions()
        setupListeners()
    }

    private fun initViews() {
        tvBluetoothStatus = findViewById(R.id.tvBluetoothStatus)
        btnConnectBluetooth = findViewById(R.id.btnConnectBluetooth)
        etCarNumber = findViewById(R.id.etCarNumber)
        etTareWeight = findViewById(R.id.etTareWeight)
        etSquareWeight = findViewById(R.id.etSquareWeight)
        tvNetWeight = findViewById(R.id.tvNetWeight)
        tvGrossWeight = findViewById(R.id.tvGrossWeight)
        etDate = findViewById(R.id.etDate)
        etTime = findViewById(R.id.etTime)
        btnManageCarTare = findViewById(R.id.btnManageCarTare)
        btnPrintHistory = findViewById(R.id.btnPrintHistory)
        btnPrint = findViewById(R.id.btnPrint)
        tvSequenceNumber = findViewById(R.id.tvSequenceNumber)
    }

    private fun initManagers() {
        bluetoothManager = BluetoothManager(this)
        carTareDatabase = CarTareDatabase(this)
        printHistoryDatabase = PrintHistoryDatabase(this)
        escPosPrinter = EscPosPrinter()
        updateBluetoothStatus()
        initDateTime()
    }

    private fun setupListeners() {
        // 蓝牙连接按钮
        btnConnectBluetooth.setOnClickListener {
            if (bluetoothManager.isConnected()) {
                // 已连接，询问是否断开
                AlertDialog.Builder(this)
                    .setTitle("断开连接")
                    .setMessage("是否断开当前打印机连接？")
                    .setPositiveButton("断开") { _, _ ->
                        bluetoothManager.disconnect()
                        updateBluetoothStatus()
                        Toast.makeText(this, "已断开连接", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            } else {
                // 未连接，显示设备列表
                showBluetoothDeviceDialog()
            }
        }

        // 车号输入监听，自动匹配皮重
        etCarNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val carNumber = s.toString().trim()
                if (carNumber.isNotEmpty()) {
                    val tareWeight = carTareDatabase.getTareByCarNumber(carNumber)
                    if (tareWeight != null) {
                        etTareWeight.setText(tareWeight.toString())
                        Toast.makeText(this@MainActivity, "自动匹配皮重：$tareWeight", Toast.LENGTH_SHORT).show()
                    }
                }
                calculateWeights()
            }
        })

        // 方量输入监听
        etSquareWeight.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                calculateWeights()
            }
        })

        // 皮重输入监听
        etTareWeight.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                calculateWeights()
            }
        })

        // 车号皮重管理
        btnManageCarTare.setOnClickListener {
            showCarTareManageDialog()
        }


        // 打印按钮
        btnPrint.setOnClickListener {
            printReceipt()
        }

        // 打印历史按钮
        btnPrintHistory.setOnClickListener {
            showPrintHistoryDialog()
        }

        // 日期选择
        etDate.setOnClickListener {
            showDatePicker()
        }

        // 时间点击聚焦时显示当前时间（可选）
        etTime.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && etTime.text.toString().isEmpty()) {
                setCurrentTime()
            }
        }
    }

    /**
     * 计算净重和毛重
     * 净重 = 方量 × 随机系数(2.36-2.37)
     * 毛重 = 净重 + 皮重
     */
    private fun calculateWeights() {
        val squareWeightStr = etSquareWeight.text.toString()
        val tareWeightStr = etTareWeight.text.toString()

        if (squareWeightStr.isNotEmpty()) {
            val squareWeight = squareWeightStr.toDoubleOrNull() ?: 0.0

            // 生成2360-2370之间的随机系数
            val randomCoefficient = (2360..2370).random()

            // 净重 = 方量 × 随机系数
            val netWeight = squareWeight * randomCoefficient

            // 毛重 = 净重 + 皮重
            val tareWeight = tareWeightStr.toDoubleOrNull() ?: 0.0
            val grossWeight = netWeight + tareWeight

            // 格式化显示（保留2位小数）
            val df = DecimalFormat("#0.00")
            tvNetWeight.text = df.format(netWeight)
            tvGrossWeight.text = df.format(grossWeight)
        } else {
            tvNetWeight.text = "0.00"
            tvGrossWeight.text = "0.00"
        }
    }

    /**
     * 初始化日期和时间
     */
    private fun initDateTime() {
        setCurrentDate()
        setCurrentTime()
        checkAndResetDailySequence()
        updateSequenceNumber()
    }

    /**
     * 设置当前日期
     */
    private fun setCurrentDate() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        etDate.setText(sdf.format(Date()))
    }

    /**
     * 设置当前时间
     */
    private fun setCurrentTime() {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        etTime.setText(sdf.format(Date()))
    }

    /**
     * 检查并重置每日序号
     */
    private fun checkAndResetDailySequence() {
        val currentDate = etDate.text.toString()
        if (currentDate != lastPrintDate) {
            // 日期变化，重置序号为1
            printSequenceNumber = 1
            lastPrintDate = currentDate
        }
    }

    /**
     * 获取格式化序号（年月日+序号）
     */
    private fun getFormattedSequenceNumber(): String {
        val date = etDate.text.toString()
        val datePart = date.replace("-", "")  // 去掉横线，变成 20260318
        val sequencePart = String.format("%04d", printSequenceNumber)
        return "$datePart$sequencePart"  // 202603180001
    }

    /**
     * 显示日期选择器
     */
    @SuppressLint("SetTextI18n")
    private fun showDatePicker() {
        val year = selectedDate.get(Calendar.YEAR)
        val month = selectedDate.get(Calendar.MONTH)
        val day = selectedDate.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                val formattedDate = String.format("%04d-%02d-%02d",
                    selectedYear,
                    selectedMonth + 1,
                    selectedDay
                )
                etDate.setText(formattedDate)
            },
            year,
            month,
            day
        )

        datePickerDialog.show()
    }

    /**
     * 更新序号显示
     */
    private fun updateSequenceNumber() {
        tvSequenceNumber.text = getFormattedSequenceNumber()
    }

    /**
     * 更新蓝牙状态
     */
    private fun updateBluetoothStatus() {
        if (bluetoothManager.isConnected()) {
            val device = bluetoothManager.getConnectedDevice()
            tvBluetoothStatus.text = "已连接: ${device?.name}"
            tvBluetoothStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            btnConnectBluetooth.text = "断开连接"
        } else {
            tvBluetoothStatus.text = getString(R.string.bluetooth_not_connected)
            tvBluetoothStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            btnConnectBluetooth.text = getString(R.string.connect_bluetooth)
        }
    }

    /**
     * 显示蓝牙设备选择对话框
     */
    @SuppressLint("MissingPermission")
    private fun showBluetoothDeviceDialog() {
        if (!bluetoothManager.isBluetoothEnabled()) {
            Toast.makeText(this, "请先开启蓝牙", Toast.LENGTH_SHORT).show()
            return
        }

        val devices = bluetoothManager.getPairedDevices().toList()
        if (devices.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_device), Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_bluetooth_devices, null)
        dialog.setView(dialogView)

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.rvBluetoothDevices)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        val adapter = BluetoothDeviceAdapter(devices) { device ->
            bluetoothManager.connectDevice(device, object : BluetoothManager.BluetoothConnectionCallback {
                override fun onConnecting() {
                    Toast.makeText(this@MainActivity, getString(R.string.connecting), Toast.LENGTH_SHORT).show()
                }

                override fun onConnected(device: BluetoothDevice) {
                    updateBluetoothStatus()
                    Toast.makeText(this@MainActivity, "${getString(R.string.connected)}: ${device.name}", Toast.LENGTH_SHORT).show()
                    alertDialog?.dismiss()
                }

                override fun onConnectionFailed(error: String) {
                    Toast.makeText(this@MainActivity, "${getString(R.string.connect_failed)}: $error", Toast.LENGTH_SHORT).show()
                }
            })
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnCancel.setOnClickListener {
            alertDialog?.dismiss()
        }

        alertDialog = dialog.create()
        alertDialog?.show()
    }

    private var alertDialog: AlertDialog? = null

    /**
     * 显示车号皮重管理对话框
     */
    private fun showCarTareManageDialog() {
        val dialog = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_manage_car_tare, null)
        dialog.setView(dialogView)

        val etNewCarNumber = dialogView.findViewById<TextInputEditText>(R.id.etNewCarNumber)
        val etNewTareWeight = dialogView.findViewById<TextInputEditText>(R.id.etNewTareWeight)
        val btnAdd = dialogView.findViewById<Button>(R.id.btnAdd)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.rvCarTareList)
        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)

        val carTareList = carTareDatabase.getAllCarTareInfo().toMutableList()

        carTareAdapter = CarTareAdapter(carTareList) { carNumber ->
            carTareDatabase.delete(carNumber)
            val newList = carTareDatabase.getAllCarTareInfo()
            carTareAdapter.updateList(newList.toMutableList())
            Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = carTareAdapter

        // 添加新车号皮重
        btnAdd.setOnClickListener {
            val carNumber = etNewCarNumber.text.toString().trim()
            val tareWeight = etNewTareWeight.text.toString().trim()

            if (carNumber.isEmpty() || tareWeight.isEmpty()) {
                Toast.makeText(this, "请输入完整信息", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tareWeightValue = tareWeight.toDoubleOrNull()
            if (tareWeightValue == null) {
                Toast.makeText(this, "请输入有效的皮重", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val success = carTareDatabase.insertOrUpdate(CarTareInfo(carNumber, tareWeightValue))
            if (success) {
                Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
                etNewCarNumber.text?.clear()
                etNewTareWeight.text?.clear()

                val newList = carTareDatabase.getAllCarTareInfo()
                carTareAdapter.updateList(newList.toMutableList())
            } else {
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
            }
        }

        btnClose.setOnClickListener {
            alertDialog?.dismiss()
        }

        alertDialog = dialog.create()
        alertDialog?.show()
    }

    /**
     * 显示打印历史对话框
     */
    private fun showPrintHistoryDialog() {
        val dialog = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_print_history, null)
        dialog.setView(dialogView)

        val rvPrintHistory = dialogView.findViewById<RecyclerView>(R.id.rvPrintHistory)
        val btnClearAll = dialogView.findViewById<Button>(R.id.btnClearAll)
        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)

        val printHistoryList = printHistoryDatabase.getAllPrintHistory().toMutableList()

        printHistoryAdapter = PrintHistoryAdapter(
            printHistoryList,
            onDelete = { printHistory ->
                AlertDialog.Builder(this)
                    .setTitle("删除确认")
                    .setMessage("确定要删除这条打印记录吗？")
                    .setPositiveButton("删除") { _, _ ->
                        printHistoryDatabase.delete(printHistory.id)
                        val newList = printHistoryDatabase.getAllPrintHistory()
                        printHistoryAdapter.updateList(newList)
                        Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            },
            onReprint = { printHistory ->
                reprintHistory(printHistory)
            }
        )

        rvPrintHistory.layoutManager = LinearLayoutManager(this)
        rvPrintHistory.adapter = printHistoryAdapter

        // 清空所有记录
        btnClearAll.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("清空确认")
                .setMessage("确定要清空所有打印记录吗？")
                .setPositiveButton("清空") { _, _ ->
                    printHistoryDatabase.clearAll()
                    printHistoryAdapter.updateList(emptyList())
                    Toast.makeText(this, "已清空所有记录", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        // 关闭对话框
        btnClose.setOnClickListener {
            alertDialog?.dismiss()
        }

        alertDialog = dialog.create()
        alertDialog?.show()
    }

    /**
     * 重新打印历史记录
     */
    private fun reprintHistory(printHistory: PrintHistory) {
        // 检查蓝牙连接
        if (!bluetoothManager.isConnected()) {
            Toast.makeText(this, "请先连接打印机", Toast.LENGTH_SHORT).show()
            return
        }

        // 生成ESC/POS打印数据
        val printData = escPosPrinter.generateWeightReceipt(
            sequenceNumber = String.format("%04d", printHistory.sequenceNumber),
            date = printHistory.date,
            time = printHistory.time,
            carNumber = printHistory.carNumber,
            squareWeight = printHistory.squareWeight,
            grossWeight = printHistory.grossWeight,
            tareWeight = printHistory.tareWeight,
            netWeight = printHistory.netWeight
        )

        // 发送打印数据
        val success = bluetoothManager.sendData(printData)
        if (success) {
            Toast.makeText(this, "重打成功", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "重打失败", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 打印称重单
     */
    private fun printReceipt() {
        // 检查蓝牙连接
        if (!bluetoothManager.isConnected()) {
            Toast.makeText(this, "请先连接打印机", Toast.LENGTH_SHORT).show()
            return
        }

        // 检查并重置每日序号
        checkAndResetDailySequence()

        // 获取输入数据
        val carNumber = etCarNumber.text.toString().trim()
        val tareWeight = etTareWeight.text.toString().trim()
        val squareWeight = etSquareWeight.text.toString().trim()
        val netWeight = tvNetWeight.text.toString()
        val grossWeight = tvGrossWeight.text.toString()
        val date = etDate.text.toString().trim()
        val time = etTime.text.toString().trim()

        // 验证输入
        if (carNumber.isEmpty() || tareWeight.isEmpty() || squareWeight.isEmpty()) {
            Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show()
            return
        }

        // 验证日期和时间
        if (date.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "请填写日期和时间", Toast.LENGTH_SHORT).show()
            return
        }

        // 格式化重量（添加(kg)）
        val grossWeightFormatted = "${grossWeight.replace(" kg", "")}(kg)"
        val tareWeightFormatted = "${tareWeight}(kg)"
        val netWeightFormatted = "${netWeight.replace(" kg", "")}(kg)"
        val squareWeightFormatted = "${squareWeight}m³"

        // 获取格式化序号
        val formattedSequenceNumber = getFormattedSequenceNumber()

        // 生成ESC/POS打印数据
        val printData = escPosPrinter.generateWeightReceipt(
            sequenceNumber = formattedSequenceNumber,
            date = date,
            time = time,
            carNumber = carNumber,
            squareWeight = squareWeightFormatted,
            grossWeight = grossWeightFormatted,
            tareWeight = tareWeightFormatted,
            netWeight = netWeightFormatted
        )

        // 发送打印数据
        val success = bluetoothManager.sendData(printData)
        if (success) {
            Toast.makeText(this, getString(R.string.print_success), Toast.LENGTH_SHORT).show()

            // 保存到打印历史
            val printHistory = PrintHistory(
                sequenceNumber = printSequenceNumber,
                date = date,
                time = time,
                carNumber = carNumber,
                squareWeight = squareWeightFormatted,
                grossWeight = grossWeightFormatted,
                tareWeight = tareWeightFormatted,
                netWeight = netWeightFormatted
            )
            printHistoryDatabase.insert(printHistory)

            // 打印成功后序号自增
            printSequenceNumber++
            updateSequenceNumber()
        } else {
            Toast.makeText(this, getString(R.string.print_failed), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 检查权限
     */
    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ 需要请求的权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            // Android 12 之前的权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            var allGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "需要蓝牙权限才能使用此功能", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.disconnect()
    }
}
