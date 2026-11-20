package com.erfansvsh.isocompressor

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.util.zip.Deflater
import java.util.zip.Inflater

object CsoEngine {
    private const val BLOCK_SIZE = 2048
    private const val CISO_MAGIC = 0x4F534943.toInt()
    private const val HEADER_SIZE = 24

    data class CsoHeader(
        val magic: Long,
        val headerSize: Long,
        val isoSize: Long,
        val blockSize: Long,
        val version: Int,
        val align: Int
    )

    suspend fun compressIso(
        context: Context,
        inputUri: Uri,
        outputUri: Uri,
        level: Int = 9,
        align: Int = 0,
        onProgress: (Float, String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val tempInput = File(context.cacheDir, "temp_input.iso")
        context.contentResolver.openInputStream(inputUri)?.use { input ->
            tempInput.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalArgumentException("Cannot open input file")

        val tempOutput = File(context.cacheDir, "temp_output.cso")

        try {
            val isoSize = tempInput.length()
            val totalBlocks = ((isoSize + BLOCK_SIZE - 1) / BLOCK_SIZE).toInt()
            val alignBytes = 1 shl align
            val alignMask = alignBytes - 1

            val offsets = IntArray(totalBlocks + 1)
            var currentOffset = HEADER_SIZE + (totalBlocks + 1) * 4

            RandomAccessFile(tempInput, "r").use { iso ->
                RandomAccessFile(tempOutput, "rw").use { cso ->
                    cso.write(ByteArray(HEADER_SIZE))
                    cso.write(ByteArray((totalBlocks + 1) * 4))

                    val buffer = ByteArray(BLOCK_SIZE)
                    val deflater = Deflater(level, true)

                    for (block in 0 until totalBlocks) {
                        iso.seek(block.toLong() * BLOCK_SIZE)
                        val bytesRead = iso.read(buffer)
                        val actualData = if (bytesRead == BLOCK_SIZE) buffer else buffer.copyOf(bytesRead)

                        deflater.reset()
                        deflater.setInput(actualData)
                        deflater.finish()

                        val compressedBuffer = ByteArray(BLOCK_SIZE * 2)
                        val compressedDataLen = deflater.deflate(compressedBuffer)
                        val compressed = compressedBuffer.copyOf(compressedDataLen)

                        val pad = (currentOffset and alignMask)
                        if (pad != 0) {
                            val padding = alignBytes - pad
                            cso.write(ByteArray(padding))
                            currentOffset += padding
                        }

                        if (compressed.size < actualData.size) {
                            offsets[block] = currentOffset ushr align
                            cso.write(compressed)
                            currentOffset += compressed.size
                        } else {
                            offsets[block] = ((currentOffset ushr align) or 0x80000000.toInt())
                            cso.write(actualData)
                            currentOffset += actualData.size
                        }

                        if (block % 200 == 0 || block == totalBlocks - 1) {
                            val progress = (block + 1).toFloat() / totalBlocks
                            onProgress(progress, "Compressing block ${block + 1}/$totalBlocks")
                        }
                    }

                    offsets[totalBlocks] = currentOffset ushr align

                    cso.seek(0)
                    writeHeader(cso, CISO_MAGIC, HEADER_SIZE.toLong(), isoSize, BLOCK_SIZE.toLong(), 1, align.toByte())

                    for (off in offsets) {
                        writeIntLE(cso, off)
                    }
                }
            }

            context.contentResolver.openOutputStream(outputUri)?.use { output ->
                tempOutput.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: throw IllegalArgumentException("Cannot open output file")

        } finally {
            if (tempInput.exists()) tempInput.delete()
            if (tempOutput.exists()) tempOutput.delete()
        }
    }

    suspend fun decompressCso(
        context: Context,
        inputUri: Uri,
        outputUri: Uri,
        onProgress: (Float, String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val tempInput = File(context.cacheDir, "temp_input.cso")
        context.contentResolver.openInputStream(inputUri)?.use { input ->
            tempInput.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalArgumentException("Cannot open input file")

        val tempOutput = File(context.cacheDir, "temp_output.iso")

        try {
            RandomAccessFile(tempInput, "r").use { fin ->
                val headerBytes = ByteArray(HEADER_SIZE)
                fin.readFully(headerBytes)

                val magic = readUInt32LE(headerBytes, 0)
                if (magic.toInt() != CISO_MAGIC) {
                    throw IllegalArgumentException("Not a valid CSO file")
                }

                val isoSize = readUInt64LE(headerBytes, 8)
                val blockSize = readUInt32LE(headerBytes, 16)
                val align = headerBytes[21].toInt() and 0xFF

                val totalBlocks = ((isoSize + blockSize - 1) / blockSize).toInt()
                val offsets = IntArray(totalBlocks + 1)
                for (i in 0..totalBlocks) {
                    offsets[i] = readIntLE(fin)
                }

                RandomAccessFile(tempOutput, "rw").use { fout ->
                    fout.setLength(isoSize)
                    val inflater = Inflater(true)

                    for (i in 0 until totalBlocks) {
                        var current = offsets[i].toLong() and 0xFFFFFFFFL
                        var nextOff = offsets[i + 1].toLong() and 0xFFFFFFFFL

                        val isRaw = (current and 0x80000000L) != 0L
                        current = current and 0x7FFFFFFF
                        nextOff = nextOff and 0x7FFFFFFF

                        val start = current shl align
                        val end = nextOff shl align
                        val length = (end - start).toInt()

                        fin.seek(start)
                        val compressedData = ByteArray(length)
                        fin.readFully(compressedData)

                        val blockData: ByteArray
                        if (isRaw) {
                            blockData = compressedData
                        } else {
                            inflater.reset()
                            inflater.setInput(compressedData)
                            val decompressed = ByteArray(blockSize.toInt())
                            val count = inflater.inflate(decompressed)
                            blockData = if (count == blockSize.toInt()) decompressed else decompressed.copyOf(count)
                        }

                        fout.seek(i.toLong() * blockSize)
                        fout.write(blockData, 0, minOf(blockData.size, (isoSize - i * blockSize).toInt()))

                        if (i % 200 == 0 || i == totalBlocks - 1) {
                            val progress = (i + 1).toFloat() / totalBlocks
                            onProgress(progress, "Decompressing block ${i + 1}/$totalBlocks")
                        }
                    }
                }
            }

            context.contentResolver.openOutputStream(outputUri)?.use { output ->
                tempOutput.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: throw IllegalArgumentException("Cannot open output file")

        } finally {
            if (tempInput.exists()) tempInput.delete()
            if (tempOutput.exists()) tempOutput.delete()
        }
    }

    fun readCsoHeader(context: Context, inputUri: Uri): CsoHeader? {
        try {
            val tempInput = File(context.cacheDir, "temp_header.cso")
            context.contentResolver.openInputStream(inputUri)?.use { input ->
                tempInput.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            RandomAccessFile(tempInput, "r").use { fin ->
                if (fin.length() < HEADER_SIZE) return null
                val headerBytes = ByteArray(HEADER_SIZE)
                fin.readFully(headerBytes)
                val magic = readUInt32LE(headerBytes, 0)
                if (magic.toInt() != CISO_MAGIC) return null

                val headerSize = readUInt32LE(headerBytes, 4)
                val isoSize = readUInt64LE(headerBytes, 8)
                val blockSize = readUInt32LE(headerBytes, 16)
                val version = headerBytes[20].toInt() and 0xFF
                val align = headerBytes[21].toInt() and 0xFF

                tempInput.delete()
                return CsoHeader(magic, headerSize, isoSize, blockSize, version, align)
            }
        } catch (e: Exception) {
            return null
        }
    }

    private fun writeHeader(raf: RandomAccessFile, magic: Int, headerSize: Long, isoSize: Long, blockSize: Long, version: Int, align: Byte) {
        writeIntLE(raf, magic)
        writeIntLE(raf, headerSize.toInt())
        writeLongLE(raf, isoSize)
        writeIntLE(raf, blockSize.toInt())
        raf.writeByte(version)
        raf.writeByte(align.toInt())
        raf.writeShort(0)
    }

    private fun writeIntLE(raf: RandomAccessFile, v: Int) {
        raf.write(v and 0xFF)
        raf.write((v ushr 8) and 0xFF)
        raf.write((v ushr 16) and 0xFF)
        raf.write((v ushr 24) and 0xFF)
    }

    private fun writeLongLE(raf: RandomAccessFile, v: Long) {
        raf.write((v and 0xFF).toInt())
        raf.write(((v ushr 8) and 0xFF).toInt())
        raf.write(((v ushr 16) and 0xFF).toInt())
        raf.write(((v ushr 24) and 0xFF).toInt())
        raf.write(((v ushr 32) and 0xFF).toInt())
        raf.write(((v ushr 40) and 0xFF).toInt())
        raf.write(((v ushr 48) and 0xFF).toInt())
        raf.write(((v ushr 56) and 0xFF).toInt())
    }

    private fun readIntLE(raf: RandomAccessFile): Int {
        val b1 = raf.read()
        val b2 = raf.read()
        val b3 = raf.read()
        val b4 = raf.read()
        if (b1 == -1 || b2 == -1 || b3 == -1 || b4 == -1) throw java.io.EOFException()
        return b1 or (b2 shl 8) or (b3 shl 16) or (b4 shl 24)
    }

    private fun readUInt32LE(arr: ByteArray, offset: Int): Long {
        val b1 = arr[offset].toLong() and 0xFF
        val b2 = arr[offset + 1].toLong() and 0xFF
        val b3 = arr[offset + 2].toLong() and 0xFF
        val b4 = arr[offset + 3].toLong() and 0xFF
        return b1 or (b2 shl 8) or (b3 shl 16) or (b4 shl 24)
    }

    private fun readUInt64LE(arr: ByteArray, offset: Int): Long {
        var value = 0L
        for (i in 0..7) {
            value = value or ((arr[offset + i].toLong() and 0xFF) shl (i * 8))
        }
        return value
    }
}
