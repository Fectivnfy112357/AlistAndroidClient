package com.textvision.alistclient.file

object FileNameValidator {
    fun isValid(name: String): Boolean = errorMessage(name) == null

    fun errorMessage(name: String): String? {
        val trimmed = name.trim()
        if (name.isBlank()) return "名称不能为空"
        if (trimmed == "." || trimmed == "..") return "名称不合法"
        if (name.contains('/') || name.contains('\\')) return "名称不能包含 / 或 \\"
        if (name.any { it.isISOControl() }) return "名称包含非法字符"
        if (trimmed.length > 255) return "名称过长（最多 255 字符）"
        return null
    }
}
