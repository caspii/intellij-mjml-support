package de.timo_reymann.mjml_support.util

import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import com.jetbrains.rd.util.warn
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Files

object FileLockUtil {
    private val logger = getLogger<FileLockUtil>()

    fun runWithLock(file: File, callback: () -> Unit) {
        logger.info { "Create lock file $file" }
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        val randomAccessFile = RandomAccessFile(file.absoluteFile, "rw")

        for (retries in 0 until 10) {
            try {
                val lock = randomAccessFile.channel.lock()
                callback()
                lock.release()
                return
            } catch (e: OverlappingFileLockException) {
                val wait = (retries * 1.5).toLong()
                logger.warn { "Could not get log file, retrying in ${wait}s" }
                Thread.sleep(wait)
            } catch (e: Exception) {
                logger.error("Failed to write lock file", e)
                throw FileLockFailedException(e)
            }
        }

        try {
            Files.delete(file.toPath())
        } catch (e: Exception) {
            logger.error("Failed to delete lock file", e)
        }
    }
}

class FileLockFailedException(cause: Exception) : Exception(cause)
