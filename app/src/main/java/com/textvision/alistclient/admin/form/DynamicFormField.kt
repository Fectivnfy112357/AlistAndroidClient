package com.textvision.alistclient.admin.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicFormField(
    item: FormItem,
    value: Any?,
    onValueChange: (Any?) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (item) {
        is FormItem.Text -> TextFieldRow(
            label = item.label,
            value = (value as? String).orEmpty(),
            keyboardType = KeyboardType.Text,
            onChange = onValueChange,
            modifier = modifier,
        )
        is FormItem.Url -> TextFieldRow(
            label = item.label,
            value = (value as? String).orEmpty(),
            keyboardType = KeyboardType.Uri,
            onChange = onValueChange,
            modifier = modifier,
        )
        is FormItem.TextArea -> Column(modifier.padding(vertical = 4.dp)) {
            Text(item.label, style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = (value as? String).orEmpty(),
                onValueChange = { onValueChange(it) },
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                minLines = 3,
            )
        }
        is FormItem.Bool -> BoolRow(
            label = item.label,
            checked = (value as? Boolean) ?: false,
            onChange = onValueChange,
            modifier = modifier,
        )
        is FormItem.Number -> TextFieldRow(
            label = item.label,
            value = value?.toString().orEmpty(),
            keyboardType = KeyboardType.Decimal,
            onChange = { onValueChange(it) },
            modifier = modifier,
        )
        is FormItem.Select -> SelectRow(
            label = item.label,
            options = item.options,
            value = (value as? String).orEmpty(),
            onChange = onValueChange,
            modifier = modifier,
        )
        is FormItem.MultiSelect -> MultiSelectRow(
            label = item.label,
            options = item.options,
            values = (value as? List<String>).orEmpty(),
            onChange = onValueChange,
            modifier = modifier,
        )
    }
}

@Composable
private fun TextFieldRow(
    label: String,
    value: String,
    keyboardType: KeyboardType,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            singleLine = keyboardType != KeyboardType.Text || true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        )
    }
}

@Composable
private fun BoolRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectRow(
    label: String,
    options: List<Pair<String, String>>,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = options.firstOrNull { it.first == value }?.second ?: value
    Column(modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        ) {
            OutlinedTextField(
                value = displayText,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { (key, labelText) ->
                    DropdownMenuItem(
                        text = { Text(labelText) },
                        onClick = {
                            onChange(key)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MultiSelectRow(
    label: String,
    options: List<Pair<String, String>>,
    values: List<String>,
    onChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { (key, labelText) ->
                val selected = key in values
                FilterChip(
                    selected = selected,
                    onClick = {
                        val next = if (selected) values - key else values + key
                        onChange(next)
                    },
                    label = { Text(labelText) },
                )
            }
        }
    }
}
