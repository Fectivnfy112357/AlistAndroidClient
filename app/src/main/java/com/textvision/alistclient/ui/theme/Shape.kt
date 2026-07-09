package com.textvision.alistclient.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object Corner {
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val ExtraLarge = 28.dp
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Corner.ExtraSmall),
    small = RoundedCornerShape(Corner.Small),
    medium = RoundedCornerShape(Corner.Medium),
    large = RoundedCornerShape(Corner.Large),
    extraLarge = RoundedCornerShape(Corner.ExtraLarge),
)
