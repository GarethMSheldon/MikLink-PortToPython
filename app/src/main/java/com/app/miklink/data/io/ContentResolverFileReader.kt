package com.app.miklink.data.io

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ContentResolverFileReader @Inject constructor(
    private val context: Context
) : FileReader {
    override suspend fun read(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
        } catch (e: Exception) {
            null
        }
    }
}
