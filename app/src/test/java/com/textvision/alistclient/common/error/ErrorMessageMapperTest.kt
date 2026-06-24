package com.textvision.alistclient.common.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ErrorMessageMapperTest {
    @Test fun mapsUserMessages() {
        assertEquals("网络不可用，请检查 WiFi 或移动数据", ErrorMessageMapper.toUserMessage(AppError.NetworkUnavailable))
        assertEquals("服务器证书不受信任（过期/自签/主机名不匹配）", ErrorMessageMapper.toUserMessage(AppError.CertificateUntrusted))
        assertEquals("文件不存在", ErrorMessageMapper.toUserMessage(AppError.NotFound))
        assertEquals("server said no", ErrorMessageMapper.toUserMessage(AppError.OperationFailed("server said no")))
    }

    @Test fun mapsActionLabels() {
        assertEquals("重试", ErrorMessageMapper.toActionLabel(AppError.Timeout))
        assertEquals("重新登录", ErrorMessageMapper.toActionLabel(AppError.Unauthorized))
        assertNull(ErrorMessageMapper.toActionLabel(AppError.Cancelled))
    }
}
