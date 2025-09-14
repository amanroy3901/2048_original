package com.avfusionapps.game_2048.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.avfusionapps.game_2048.ui.theme.HighLighter

@Composable
fun NeonCutCornerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonColor: Color = HighLighter,
    cornerRadius: Int = 8
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = CutCornerShape(cornerRadius.dp),
        colors = ButtonDefaults.buttonColors(buttonColor),
        contentPadding = PaddingValues(8.dp)
    ) {
        Text(text = text, color = Color.White)
    }
}

fun ContentDrawScope.drawNeonGlow(
    color: Color, radius: Dp
) {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            style = PaintingStyle.Stroke
            strokeWidth = 12f
        }

        val frameworkPaint = paint.asFrameworkPaint().apply {
            this.color = color.copy(alpha = 0f).toArgb()
            setShadowLayer(radius.toPx(), 0f, 0f, color.copy(alpha = 0.6f).toArgb())
        }

        canvas.drawRoundRect(
            left = 0f,
            right = size.width,
            bottom = size.height,
            top = 0f,
            radiusX = radius.toPx(),
            radiusY = radius.toPx(),
            paint = paint
        )
    }
}

@Composable
fun NeonRoundedButton(
    text: String? = null,
    icon: Int? = null,
    onClick: () -> Unit,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    buttonColor: Color = HighLighter,
    glowColor: Color = HighLighter.copy(alpha = 0.8f),
    cornerRadius: Dp = 12.dp,
    glowRadius: Dp = 10.dp,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .drawWithContent {
                drawContent()
                if (enabled) drawNeonGlow(glowColor, glowRadius)
            },
        shape = RoundedCornerShape(cornerRadius),
        border = BorderStroke(1.dp, Color.Transparent),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            disabledContainerColor = buttonColor.copy(alpha = 0.5f)
        ),
        enabled = enabled
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = contentDescription,
                    tint = if (enabled) Color.White else Color.White.copy(alpha = 0.5f)
                )
                if (!text.isNullOrBlank()) Spacer(modifier = Modifier.width(8.dp))
            }
            if (!text.isNullOrBlank()) {
                Text(
                    text = text,
                    color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
                    style = TextStyle(
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        shadow = if (enabled) Shadow(color = glowColor, blurRadius = 10f) else null
                    )
                )
            }
        }
    }
}
