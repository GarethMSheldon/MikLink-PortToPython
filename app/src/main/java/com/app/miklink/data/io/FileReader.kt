package com.app.miklink.data.io

import android.net.Uri

interface FileReader {
    suspend fun read(uri: Uri): String?
}
