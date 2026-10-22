/*
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lineageos.settings.utils

import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

object FileUtils {
    private const val TAG = "FileUtils"

    /**
     * Reads the first line of text from the given file.
     */
    @JvmStatic
    fun readOneLine(fileName: String): String? {
        var line: String? = null
        var reader: BufferedReader? = null
        try {
            reader = BufferedReader(FileReader(fileName), 512)
            line = reader.readLine()
        } catch (e: FileNotFoundException) {
            Log.w(TAG, "No such file $fileName for reading", e)
        } catch (e: IOException) {
            Log.e(TAG, "Could not read from file $fileName", e)
        } finally {
            try {
                reader?.close()
            } catch (e: IOException) {
                // Ignored
            }
        }
        return line
    }

    /**
     * Writes the given value into the given file
     */
    @JvmStatic
    fun writeLine(fileName: String, value: String): Boolean {
        var writer: BufferedWriter? = null
        try {
            writer = BufferedWriter(FileWriter(fileName))
            writer.write(value)
        } catch (e: FileNotFoundException) {
            Log.w(TAG, "No such file $fileName for writing", e)
            return false
        } catch (e: IOException) {
            Log.e(TAG, "Could not write to file $fileName", e)
            return false
        } finally {
            try {
                writer?.close()
            } catch (e: IOException) {
                // Ignored
            }
        }
        return true
    }

    /**
     * Checks whether the given file exists
     */
    @JvmStatic
    fun fileExists(fileName: String): Boolean {
        val file = File(fileName)
        return file.exists()
    }

    /**
     * Checks whether the given file is readable
     */
    @JvmStatic
    fun isFileReadable(fileName: String): Boolean {
        val file = File(fileName)
        return file.exists() && file.canRead()
    }

    /**
     * Checks whether the given file is writable
     */
    @JvmStatic
    fun isFileWritable(fileName: String): Boolean {
        val file = File(fileName)
        return file.exists() && file.canWrite()
    }

    /**
     * Deletes an existing file
     */
    @JvmStatic
    fun delete(fileName: String): Boolean {
        val file = File(fileName)
        return try {
            file.delete()
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException trying to delete $fileName", e)
            false
        }
    }

    /**
     * Renames an existing file
     */
    @JvmStatic
    fun rename(srcPath: String, dstPath: String): Boolean {
        val srcFile = File(srcPath)
        val dstFile = File(dstPath)
        return try {
            srcFile.renameTo(dstFile)
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException trying to rename $srcPath to $dstPath", e)
            false
        } catch (e: NullPointerException) {
            Log.e(TAG, "NullPointerException trying to rename $srcPath to $dstPath", e)
            false
        }
    }
}
