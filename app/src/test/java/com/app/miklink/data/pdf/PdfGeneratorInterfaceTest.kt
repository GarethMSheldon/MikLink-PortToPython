package com.app.miklink.data.pdf

import android.content.Context
import com.app.miklink.data.network.NeighborDetailListAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PdfGeneratorInterfaceTest {

    private lateinit var pdfGenerator: PdfGeneratorIText

    @Before
    fun setup() {
        val moshi = Moshi.Builder()
            .add(NeighborDetailListAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()

        val mockContext = mockk<Context>(relaxed = true)
        pdfGenerator = PdfGeneratorIText(mockContext, moshi)
    }

    @Test
    fun `PdfGeneratorIText implements PdfGenerator interface`() {
        assertTrue("PdfGeneratorIText should implement PdfGenerator interface", pdfGenerator is PdfGenerator)
    }
}
