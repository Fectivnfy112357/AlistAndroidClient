package com.textvision.alistclient.file

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNameValidatorTest {
    @Test fun validatesLegalName() {
        assertTrue(FileNameValidator.isValid("photo.jpg"))
        assertNull(FileNameValidator.errorMessage("photo.jpg"))
    }

    @Test fun rejectsSpecificInvalidNames() {
        assertEquals("名称不能为空", FileNameValidator.errorMessage("   "))
        assertEquals("名称不合法", FileNameValidator.errorMessage(".."))
        assertEquals("名称不能包含 / 或 \\", FileNameValidator.errorMessage("a/b"))
        assertEquals("名称包含非法字符", FileNameValidator.errorMessage("a\u0000b"))
        assertEquals("名称过长（最多 255 字符）", FileNameValidator.errorMessage("a".repeat(256)))
        assertFalse(FileNameValidator.isValid("a\\b"))
    }
}
