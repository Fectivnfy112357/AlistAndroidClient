package com.textvision.alistclient.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object Corner {
    val ExtraSmall = 10.dp   // tag, mini elements
    val Small      = 14.dp   // input, small card
    val Medium     = 18.dp   // button
    val Large      = 22.dp   // card
    val ExtraLarge = 28.dp   // modal, album cover
    val Pill       = 999.dp  // chip
    val Circle     = CircleShape
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Corner.ExtraSmall),
    small      = RoundedCornerShape(Corner.Small),
    medium     = RoundedCornerShape(Corner.Medium),
    large      = RoundedCornerShape(Corner.Large),
    extraLarge = RoundedCornerShape(Corner.ExtraLarge),
)
