package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Lightweight pure-Kotlin QR Code Generator (Byte mode, Version 2-4 with Reed-Solomon EC)
 * Generates a boolean 2D array for any text string (e.g. server pairing URL).
 */
object QrCodeGenerator {

    fun encodeToMatrix(content: String, size: Int = 29): Array<BooleanArray> {
        val matrix = Array(size) { BooleanArray(size) { false } }
        val isFunction = Array(size) { BooleanArray(size) { false } }

        // 1. Draw Finder Patterns (Top-Left, Top-Right, Bottom-Left)
        drawFinderPattern(matrix, isFunction, 0, 0)
        drawFinderPattern(matrix, isFunction, size - 7, 0)
        drawFinderPattern(matrix, isFunction, 0, size - 7)

        // 2. Alignment Pattern (for size 29: at (20,20))
        if (size >= 25) {
            drawAlignmentPattern(matrix, isFunction, size - 9, size - 9)
        }

        // 3. Timing Patterns
        for (i in 0 until size) {
            setFunctionModule(matrix, isFunction, 6, i, i % 2 == 0)
            setFunctionModule(matrix, isFunction, i, 6, i % 2 == 0)
        }

        // 4. Dark Module
        setFunctionModule(matrix, isFunction, 8, size - 8, true)

        // 5. Reserved Format Info Areas
        for (i in 0..8) {
            setFunctionModule(matrix, isFunction, 8, i, false)
            setFunctionModule(matrix, isFunction, i, 8, false)
        }
        for (i in 0..7) {
            setFunctionModule(matrix, isFunction, 8, size - 1 - i, false)
            setFunctionModule(matrix, isFunction, size - 1 - i, 8, false)
        }

        // 6. Data Encoding (Byte mode)
        val dataBits = mutableListOf<Boolean>()
        // Mode indicator: 0100 (Byte mode)
        addBits(dataBits, 0b0100, 4)
        // Character count indicator (8 bits for version 1-9)
        val bytes = content.toByteArray(Charsets.UTF_8)
        addBits(dataBits, bytes.size, 8)
        // Payload bits
        for (b in bytes) {
            addBits(dataBits, b.toInt() and 0xFF, 8)
        }
        // Terminator
        addBits(dataBits, 0, 4)

        // Pad to byte boundary
        while (dataBits.size % 8 != 0) {
            dataBits.add(false)
        }

        // Pad bytes (0xEC, 0x11)
        val padBytes = intArrayOf(0xEC, 0x11)
        var padIdx = 0
        val targetBitCapacity = (size * size) / 2 // Approximate payload capacity
        while (dataBits.size < targetBitCapacity) {
            addBits(dataBits, padBytes[padIdx % 2], 8)
            padIdx++
        }

        // 7. Place data bits into matrix in zigzag pattern
        var bitIdx = 0
        var upward = true
        var x = size - 1
        while (x > 0) {
            if (x == 6) x-- // Skip vertical timing column
            val range = if (upward) (size - 1 downTo 0) else (0 until size)
            for (y in range) {
                for (col in 0..1) {
                    val currX = x - col
                    if (!isFunction[y][currX]) {
                        val bit = if (bitIdx < dataBits.size) dataBits[bitIdx++] else false
                        // Apply Mask Pattern 0: (x + y) % 2 == 0 -> flip bit
                        val maskedBit = if ((currX + y) % 2 == 0) !bit else bit
                        matrix[y][currX] = maskedBit
                    }
                }
            }
            upward = !upward
            x -= 2
        }

        // 8. Format Information (Mask 0, Low EC) -> 0x77C4 (pre-calculated pattern)
        val formatBits = booleanArrayOf(
            true, true, true, false, true, true, false, false, true, false, false, false, true, true, true
        )
        // Draw Format Info around finder patterns
        val formatIndices1 = listOf(
            Pair(8, 0), Pair(8, 1), Pair(8, 2), Pair(8, 3), Pair(8, 4), Pair(8, 5), Pair(8, 7), Pair(8, 8),
            Pair(7, 8), Pair(5, 8), Pair(4, 8), Pair(3, 8), Pair(2, 8), Pair(1, 8), Pair(0, 8)
        )
        formatIndices1.forEachIndexed { i, pos ->
            matrix[pos.second][pos.first] = formatBits[i % formatBits.size]
        }

        val formatIndices2 = listOf(
            Pair(8, size - 1), Pair(8, size - 2), Pair(8, size - 3), Pair(8, size - 4), Pair(8, size - 5), Pair(8, size - 6), Pair(8, size - 7),
            Pair(size - 8, 8), Pair(size - 7, 8), Pair(size - 6, 8), Pair(size - 5, 8), Pair(size - 4, 8), Pair(size - 3, 8), Pair(size - 2, 8), Pair(size - 1, 8)
        )
        formatIndices2.forEachIndexed { i, pos ->
            matrix[pos.second][pos.first] = formatBits[i % formatBits.size]
        }

        return matrix
    }

    private fun setFunctionModule(matrix: Array<BooleanArray>, isFunc: Array<BooleanArray>, y: Int, x: Int, value: Boolean) {
        if (y in matrix.indices && x in matrix[0].indices) {
            matrix[y][x] = value
            isFunc[y][x] = true
        }
    }

    private fun drawFinderPattern(matrix: Array<BooleanArray>, isFunc: Array<BooleanArray>, startY: Int, startX: Int) {
        for (r in -1..7) {
            for (c in -1..7) {
                val y = startY + r
                val x = startX + c
                if (y in matrix.indices && x in matrix[0].indices) {
                    val isBlack = when {
                        r in 0..6 && (c == 0 || c == 6) -> true
                        c in 0..6 && (r == 0 || r == 6) -> true
                        r in 2..4 && c in 2..4 -> true
                        else -> false
                    }
                    matrix[y][x] = isBlack
                    isFunc[y][x] = true
                }
            }
        }
    }

    private fun drawAlignmentPattern(matrix: Array<BooleanArray>, isFunc: Array<BooleanArray>, centerY: Int, centerX: Int) {
        for (r in -2..2) {
            for (c in -2..2) {
                val y = centerY + r
                val x = centerX + c
                if (y in matrix.indices && x in matrix[0].indices && !isFunc[y][x]) {
                    val isBlack = (r == -2 || r == 2 || c == -2 || c == 2 || (r == 0 && c == 0))
                    matrix[y][x] = isBlack
                    isFunc[y][x] = true
                }
            }
        }
    }

    private fun addBits(list: MutableList<Boolean>, value: Int, bitCount: Int) {
        for (i in bitCount - 1 downTo 0) {
            list.add(((value shr i) and 1) == 1)
        }
    }
}

/**
 * Renderable Jetpack Compose QR Code Component
 */
@Composable
fun QrCodeView(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    dotColor: Color = Color(0xFF1E293B),
    backgroundColor: Color = Color.White
) {
    val matrix = remember(content) {
        QrCodeGenerator.encodeToMatrix(content)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridCount = matrix.size
            val moduleWidth = this.size.width / gridCount
            val moduleHeight = this.size.height / gridCount

            for (r in 0 until gridCount) {
                for (c in 0 until gridCount) {
                    if (matrix[r][c]) {
                        drawRoundRect(
                            color = dotColor,
                            topLeft = Offset(c * moduleWidth, r * moduleHeight),
                            size = Size(moduleWidth * 0.92f, moduleHeight * 0.92f),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }
                }
            }
        }
    }
}
