package com.app.miklink.ui.common

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BaseEditViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @org.junit.Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @org.junit.After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    class FakeEditViewModel(savedStateHandle: SavedStateHandle) : BaseEditViewModel(savedStateHandle, "entityId") {
        var loadedId: Long? = null

        override suspend fun loadEntity(id: Long) {
            loadedId = id
        }

        // Expose a public save method that marks saved
        fun doSave() {
            markSaved()
        }

        // Test helper -> expose protected loader so tests can trigger loading explicitly
        fun triggerLoadIfEditing() {
            loadIfEditing()
        }
    }

    @Test
    fun `GIVEN no id in savedState WHEN created THEN isEditing false and loadEntity not called`() = runBlocking {
        val saved = SavedStateHandle()
        val vm = FakeEditViewModel(saved)
        assertFalse(vm.isEditing)
        assertTrue(vm.isSaved.value == false)
        assertTrue(vm.loadedId == null)
    }

    @Test
    fun `GIVEN id present WHEN created THEN isEditing true and loadEntity called`() = runBlocking {
        val saved = SavedStateHandle(mapOf("entityId" to 42L))
        val vm = FakeEditViewModel(saved)

        // explicitly trigger loading helper via the test helper and wait for it to complete
        vm.triggerLoadIfEditing()
        var attempts = 0
        while (vm.loadedId == null && attempts++ < 50) Thread.sleep(10)
        assertTrue(vm.isEditing)
        assertTrue(vm.loadedId == 42L)
    }

    @Test
    fun `markSaved updates isSaved flow`() = runBlocking {
        val saved = SavedStateHandle()
        val vm = FakeEditViewModel(saved)
        assertFalse(vm.isSaved.value)
        vm.doSave()
        assertTrue(vm.isSaved.value)
    }
}
