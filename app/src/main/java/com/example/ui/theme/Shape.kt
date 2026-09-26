package com.example.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Material 3 Expressive Shapes
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// Specialized Expressive tokens
val ExpressivePillShape = CircleShape
val ExpressiveCardShape = RoundedCornerShape(20.dp)
val ExpressiveBannerShape = RoundedCornerShape(24.dp)
val ExpressiveChipShape = RoundedCornerShape(100.dp)
val ExpressiveDialogShape = RoundedCornerShape(28.dp)
val ExpressiveButtonShape = RoundedCornerShape(14.dp)
val ExpressiveSquircleShape = RoundedCornerShape(16.dp)
