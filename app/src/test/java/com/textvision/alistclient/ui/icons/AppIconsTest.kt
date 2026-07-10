package com.textvision.alistclient.ui.icons

import org.junit.Assert.assertNotNull
import org.junit.Test

class AppIconsTest {
    @Test fun allIconsNonNull() {
        val fields = AppIcons::class.java.declaredFields
        fields.forEach { field ->
            field.isAccessible = true
            val value = field.get(AppIcons)
            assertNotNull("AppIcons.${field.name} should not be null", value)
        }
    }
}
