package com.textvision.alistclient.admin.form

import com.textvision.alistclient.network.dto.ConfigItem
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed class FormItem {
    abstract val name: String
    abstract val label: String
    abstract val required: Boolean

    data class Text(
        override val name: String,
        override val label: String,
        override val required: Boolean = false,
    ) : FormItem()

    data class Url(
        override val name: String,
        override val label: String,
        override val required: Boolean = false,
    ) : FormItem()

    data class TextArea(
        override val name: String,
        override val label: String,
        override val required: Boolean = false,
    ) : FormItem()

    data class Bool(
        override val name: String,
        override val label: String,
        override val required: Boolean = false,
    ) : FormItem()

    data class Number(
        override val name: String,
        override val label: String,
        override val required: Boolean = false,
    ) : FormItem()

    data class Select(
        override val name: String,
        override val label: String,
        val options: List<Pair<String, String>>,
        override val required: Boolean = false,
    ) : FormItem()

    data class MultiSelect(
        override val name: String,
        override val label: String,
        val options: List<Pair<String, String>>,
        override val required: Boolean = false,
    ) : FormItem()

    companion object {
        fun fromConfigItem(item: ConfigItem): FormItem {
            val name = item.name
            val label = item.label ?: name
            val required = item.required
            return when (item.type?.lowercase()) {
                "string" -> Text(name, label, required)
                "url" -> Url(name, label, required)
                "text" -> TextArea(name, label, required)
                "bool", "boolean" -> Bool(name, label, required)
                "number", "int", "integer", "float", "double" -> Number(name, label, required)
                "select" -> Select(name, label, parseOptions(item.options), required)
                "multi-select", "multi_select", "list", "strings" ->
                    MultiSelect(name, label, parseOptions(item.options), required)
                else -> Text(name, label, required) // 降级为 Text
            }
        }

        private fun parseOptions(raw: kotlinx.serialization.json.JsonElement?): List<Pair<String, String>> {
            if (raw == null) return emptyList()
            return try {
                val arr = (raw as? JsonArray) ?: return emptyList()
                arr.map { element ->
                    val obj = element as? JsonObject
                    val value = obj?.get("value")?.jsonPrimitive?.content ?: element.jsonPrimitive.content
                    val label = obj?.get("label")?.jsonPrimitive?.content ?: value
                    value to label
                }
            } catch (t: Throwable) {
                emptyList()
            }
        }
    }
}