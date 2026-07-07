package com.textvision.alistclient.admin.form

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class DynamicFormFieldTest {
    @get:Rule val rule = createComposeRule()

    private fun render(item: FormItem, value: Any? = null, onChange: (Any?) -> Unit = {}) {
        rule.setContent { DynamicFormField(item, value, onChange) }
    }

    @Test fun textFieldRendersLabelAndAcceptsInput() {
        var captured: Any? = null
        render(FormItem.Text("name", "用户名", required = true), value = "", onChange = { captured = it })
        rule.onNodeWithText("用户名").assertExists()
        rule.onNodeWithText("用户名").performTextInput("alice")
        rule.mainClock.autoAdvance = false
    }

    @Test fun textAreaRendersLabel() {
        render(FormItem.TextArea("remark", "备注"))
        rule.onNodeWithText("备注").assertExists()
    }

    @Test fun urlFieldRendersLabel() {
        render(FormItem.Url("logo", "Logo URL"))
        rule.onNodeWithText("Logo URL").assertExists()
    }

    @Test fun boolFieldRendersLabel() {
        render(FormItem.Bool("enabled", "启用"))
        rule.onNodeWithText("启用").assertExists()
    }

    @Test fun numberFieldRendersLabel() {
        render(FormItem.Number("port", "端口"))
        rule.onNodeWithText("端口").assertExists()
    }

    @Test fun selectFieldRendersLabel() {
        render(FormItem.Select("driver", "Driver", listOf("Local" to "Local", "S3" to "S3")))
        rule.onNodeWithText("Driver").assertExists()
    }

    @Test fun multiSelectFieldRendersLabel() {
        render(FormItem.MultiSelect("tags", "标签", listOf("a" to "A", "b" to "B")))
        rule.onNodeWithText("标签").assertExists()
    }
}