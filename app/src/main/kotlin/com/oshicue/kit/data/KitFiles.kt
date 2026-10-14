package com.oshicue.kit.data

import android.content.Context
import java.io.File
import java.util.UUID

internal object KitFiles {
    fun directory(context: Context): File {
        val directory = File(context.filesDir, "OshicueKit")
        if (!directory.exists()) directory.mkdirs()
        return directory
    }

    fun writeAtomic(file: File, bytes: ByteArray) {
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, file.name + ".tmp")
        temporary.writeBytes(bytes)
        if (!temporary.renameTo(file)) {
            file.writeBytes(bytes)
            temporary.delete()
        }
    }

    /** Preserves unreadable data before a user starts a replacement document. */
    fun backUpUnreadable(file: File) {
        if (!file.exists()) return
        val recovery = File(file.parentFile, file.nameWithoutExtension + ".recovery-" + UUID.randomUUID() + ".json")
        file.copyTo(recovery, overwrite = false)
    }
}
