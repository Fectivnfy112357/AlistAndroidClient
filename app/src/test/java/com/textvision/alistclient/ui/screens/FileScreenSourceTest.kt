package com.textvision.alistclient.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FileScreenSourceTest {
    @Test
    fun uploadPickerUsesOpenDocumentSoRetryCanReadTheUriLater() {
        val source = File("src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt").readText()

        assertTrue(source.contains("ActivityResultContracts.OpenDocument()"))
        assertFalse(source.contains("ActivityResultContracts.GetContent()"))
    }
}
