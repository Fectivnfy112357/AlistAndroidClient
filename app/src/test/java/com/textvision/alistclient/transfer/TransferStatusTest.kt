package com.textvision.alistclient.transfer

import com.textvision.alistclient.transfer.model.TransferStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferStatusTest {
    @Test fun retryRulesMatchSpec() {
        assertTrue(TransferStatus.Failed.canRetry)
        assertTrue(TransferStatus.Interrupted.canRetry)
        assertFalse(TransferStatus.Cancelled.canRetry)
        assertFalse(TransferStatus.Success.canRetry)
    }

    @Test fun interruptedAndFailedUseDifferentLabels() {
        assertEquals("重试", TransferStatus.Failed.retryLabel)
        assertEquals("重新传输", TransferStatus.Interrupted.retryLabel)
    }
}
