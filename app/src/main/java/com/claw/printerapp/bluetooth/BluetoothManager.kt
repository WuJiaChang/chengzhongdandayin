package com.claw.printerapp.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class BluetoothManager(private val context: Context) {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    // SPP UUID
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val TAG = "BluetoothManager"

    init {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    }

    /**
     * 检查蓝牙是否启用
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * 获取已配对的设备列表
     */
    fun getPairedDevices(): Set<BluetoothDevice> {
        return if (checkBluetoothPermission()) {
            bluetoothAdapter?.bondedDevices ?: emptySet()
        } else {
            emptySet()
        }
    }

    /**
     * 连接蓝牙设备（SPP协议）
     */
    @SuppressLint("MissingPermission")
    fun connectDevice(device: BluetoothDevice, callback: BluetoothConnectionCallback) {
        // 在后台线程中处理连接逻辑
        Thread {
            // 确保先完全断开之前的连接
            if (bluetoothSocket?.isConnected == true) {
                disconnect()
                // 等待 socket 完全关闭
                try {
                    Thread.sleep(500)
                } catch (e: InterruptedException) {
                    Log.e(TAG, "等待断开被中断", e)
                }
            }

            try {
                Log.d(TAG, "开始连接设备: ${device.name}")

                // 切换回主线程显示连接状态
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    callback.onConnecting()
                }

                // 尝试多种方式创建 socket
                var socket: BluetoothSocket? = null
                var exception: IOException? = null

                try {
                    // 方式1: 标准 SPP UUID
                    socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                } catch (e: IOException) {
                    exception = e
                    try {
                        // 方式2: 使用反射方式创建 socket（兼容更多打印机）
                        val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        socket = m.invoke(device, 1) as BluetoothSocket
                    } catch (e2: Exception) {
                        Log.e(TAG, "反射创建 socket 失败", e2)
                    }
                }

                if (socket == null) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        callback.onConnectionFailed("无法创建 socket: ${exception?.message}")
                    }
                    return@Thread
                }

                bluetoothSocket = socket

                try {
                    bluetoothSocket?.connect()

                    // 连接成功后在主线程回调
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        try {
                            outputStream = bluetoothSocket?.outputStream
                            Log.d(TAG, "连接成功: ${device.name}")
                            callback.onConnected(device)
                        } catch (e: Exception) {
                            Log.e(TAG, "连接后处理失败", e)
                            disconnect()
                            callback.onConnectionFailed(e.message ?: "连接后处理失败")
                        }
                    }

                } catch (e: IOException) {
                    Log.e(TAG, "连接失败", e)
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        disconnect()
                        callback.onConnectionFailed(e.message ?: "连接失败")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "连接过程异常", e)
                disconnect()
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    callback.onConnectionFailed(e.message ?: "连接异常")
                }
            }
        }.start()
    }

    /**
     * 发送数据到蓝牙设备
     */
    fun sendData(data: ByteArray): Boolean {
        return try {
            outputStream?.write(data)
            outputStream?.flush()
            true
        } catch (e: IOException) {
            Log.e(TAG, "发送数据失败", e)
            false
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        try {
            outputStream?.flush()
        } catch (e: IOException) {
            Log.e(TAG, "刷新输出流失败", e)
        }

        try {
            outputStream?.close()
        } catch (e: IOException) {
            Log.e(TAG, "关闭输出流失败", e)
        }

        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "关闭 socket 失败", e)
        } finally {
            outputStream = null
            bluetoothSocket = null
        }
    }

    /**
     * 检查是否已连接
     */
    fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true
    }

    /**
     * 获取当前连接的设备
     */
    fun getConnectedDevice(): BluetoothDevice? {
        return if (isConnected()) {
            bluetoothSocket?.remoteDevice
        } else {
            null
        }
    }

    /**
     * 检查蓝牙权限
     */
    @SuppressLint("NewApi")
    private fun checkBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 获取蓝牙适配器
     */
    fun getBluetoothAdapter(): BluetoothAdapter? {
        return bluetoothAdapter
    }

    interface BluetoothConnectionCallback {
        fun onConnecting()
        fun onConnected(device: BluetoothDevice)
        fun onConnectionFailed(error: String)
    }
}
