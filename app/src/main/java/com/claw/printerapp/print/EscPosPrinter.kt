package com.claw.printerapp.print

import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

/**
 * ESC/POS 热敏打印机指令类
 * 实现标准的ESC/POS打印指令
 */
class EscPosPrinter {

    // ESC/POS 指令常量
    companion object {
        private const val ESC = 0x1B.toByte()  // ESC
        private const val GS = 0x1D.toByte()    // GS
        private const val AT = 0x40.toByte()   // @
        private const val EXCLAMATION = 0x21.toByte()  // !
        private const val DOLLAR = 0x24.toByte()  // $
        private const val AMPERSAND = 0x26.toByte()  // &
        private const val SLASH = 0x2F.toByte()  // /
        private const val ZERO = 0x30.toByte()   // 0
        private const val TWO = 0x32.toByte()    // 2
        private const val THREE = 0x33.toByte()  // 3
        private const val A = 0x41.toByte()     // A
        private const val B = 0x42.toByte()     // B
        private const val C = 0x43.toByte()     // C
        private const val E = 0x45.toByte()     // E
        private const val L = 0x4C.toByte()     // L
        private const val M = 0x4D.toByte()     // M
        private const val Q = 0x51.toByte()     // Q
        private const val V = 0x56.toByte()     // V
        private const val W = 0x57.toByte()     // W
        private const val a = 0x61.toByte()     // a
        private const val f = 0x66.toByte()     // f
        private const val t = 0x74.toByte()     // t

        // 字符编码
        private val GBK = Charset.forName("GBK")
    }

    private val buffer = mutableListOf<Byte>()

    /**
     * 初始化打印机
     */
    fun initPrinter(): EscPosPrinter {
        addBytes(ESC, AT)  // ESC @ - 初始化
        return this
    }

    /**
     * 设置对齐方式
     * @param alignment 0-左对齐, 1-居中, 2-右对齐
     */
    fun setAlign(alignment: Int): EscPosPrinter {
        addBytes(ESC, a, alignment.toByte())
        return this
    }

    /**
     * 左对齐
     */
    fun alignLeft(): EscPosPrinter {
        return setAlign(0)
    }

    /**
     * 居中对齐
     */
    fun alignCenter(): EscPosPrinter {
        return setAlign(1)
    }

    /**
     * 右对齐
     */
    fun alignRight(): EscPosPrinter {
        return setAlign(2)
    }

    /**
     * 设置字体大小
     * @param widthScale 宽度缩放 1-8
     * @param heightScale 高度缩放 1-8
     */
    fun setFontSize(widthScale: Int, heightScale: Int): EscPosPrinter {
        addBytes(GS, EXCLAMATION, ((widthScale - 1) * 16 + (heightScale - 1)).toByte())
        return this
    }

    /**
     * 正常字体
     */
    fun normalFont(): EscPosPrinter {
        return setFontSize(1, 1)
    }

    /**
     * 双倍字体
     */
    fun doubleFont(): EscPosPrinter {
        return setFontSize(2, 2)
    }

    /**
     * 加粗
     */
    fun bold(on: Boolean = true): EscPosPrinter {
        addBytes(ESC, E, if (on) 0x0F.toByte() else 0x00.toByte())
        return this
    }

    /**
     * 下划线
     */
    fun underline(on: Boolean = true): EscPosPrinter {
        addBytes(ESC, MINUS, if (on) 0x01.toByte() else 0x00.toByte())
        return this
    }

    private val MINUS = 0x2D.toByte()  // -

    /**
     * 打印并换行
     */
    fun printLine(text: String): EscPosPrinter {
        addText(text)
        addBytes(LF)
        return this
    }

    private val LF = 0x0A.toByte()  // 换行

    /**
     * 打印不换行
     */
    fun print(text: String): EscPosPrinter {
        addText(text)
        return this
    }

    /**
     * 添加文本
     */
    private fun addText(text: String) {
        try {
            val bytes = text.toByteArray(GBK)
            buffer.addAll(bytes.toList())
        } catch (e: UnsupportedEncodingException) {
            val bytes = text.toByteArray()
            buffer.addAll(bytes.toList())
        }
    }

    /**
     * 添加字节
     */
    private fun addBytes(vararg bytes: Byte) {
        buffer.addAll(bytes.toList())
    }

    /**
     * 打印空行
     */
    fun printEmptyLine(lines: Int = 1): EscPosPrinter {
        repeat(lines) {
            addBytes(LF)
        }
        return this
    }

    /**
     * 打印分割线
     */
    fun printLineSeparator(): EscPosPrinter {
        return printLine("===============")
    }

    /**
     * 打印点分割线
     */
    fun printDotSeparator(): EscPosPrinter {
        return printLine("---------------")
    }

    /**
     * 进纸并切纸
     */
    fun feedAndCut(): EscPosPrinter {
        addBytes(ESC, THREE, 0x03.toByte())  // 进纸3行
        addBytes(GS, V, 0x01.toByte())      // 全切
        return this
    }

    /**
     * 进纸
     */
    fun feed(lines: Int): EscPosPrinter {
        addBytes(ESC, THREE, lines.toByte())
        return this
    }

    /**
     * 获取打印数据
     */
    fun getBytes(): ByteArray {
        return buffer.toByteArray()
    }

    /**
     * 清空缓冲区
     */
    fun clear(): EscPosPrinter {
        buffer.clear()
        return this
    }

    /**
     * 生成称重单打印数据
     */
    fun generateWeightReceipt(
        sequenceNumber: String,
        date: String,
        time: String,
        carNumber: String,
        squareWeight: String,
        grossWeight: String,
        tareWeight: String,
        netWeight: String
    ): ByteArray {
        // 打印机标准宽度32字符（2倍宽字体下相当于16字符）
        val lineWidth = 16  // 2倍宽字体下的有效行宽
        val labelWidth = 4    // 标签占4字符

        return clear()
            .initPrinter()
            .alignCenter()
            .setFontSize(2, 2)  // 2倍宽、2倍高的大字体用于标题
            .bold(true)
            .printLine("称重单")
            .setFontSize(2, 2)  // 2倍宽、1倍高的字体用于内容（避免过高）
            .bold(false)
            .printEmptyLine(1)
            .printLineSeparator()  // 第一条分割线：在"称重单"下方
            .alignLeft()
            // 根据数值实际长度动态计算填充空格，让数值右对齐到行末
            .printLine(formatRow("序号", sequenceNumber, lineWidth, labelWidth))
            .printLine(formatRow("日期", date, lineWidth, labelWidth))
            .printLine(formatRow("时间", time, lineWidth, labelWidth))
            .printLine(formatRow("车号", carNumber, lineWidth, labelWidth))
            .printLine(formatRow("方量", squareWeight, lineWidth, labelWidth))
            .printLine(formatRow("毛重", grossWeight, lineWidth, labelWidth))
            .printLine(formatRow("皮重", tareWeight, lineWidth, labelWidth))
            .printLine(formatRow("净重", netWeight, lineWidth, labelWidth))
            .printEmptyLine(1)
            .alignCenter()
            .printLineSeparator()  // 第二条分割线：在最底部
            .printEmptyLine(2)  // 底部留更多余量
            .feedAndCut()
            .getBytes()
    }

    /**
     * 格式化一行：标签左对齐，数值右对齐
     */
    private fun formatRow(label: String, value: String, lineWidth: Int, labelWidth: Int): String {
        // 计算需要的空格数：行宽 - 标签宽度 - 数值实际宽度 + 1（额外往右挪一格）
        val padding = lineWidth - labelWidth - value.length
        val space = if (padding > 0) " ".repeat(padding) else ""
        return "$label$space$value"
    }
}
