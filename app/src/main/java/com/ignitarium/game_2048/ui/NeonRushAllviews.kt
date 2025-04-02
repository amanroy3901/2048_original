package com.ignitarium.game_2048.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ignitarium.game_2048.ui.theme.HighLighter


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
            strokeWidth = 12f  // Slightly reduced stroke width for optimization
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
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonColor: Color = HighLighter,
    glowColor: Color = HighLighter.copy(alpha = 0.8f),
    cornerRadius: Dp = 12.dp,
    glowRadius: Dp = 10.dp,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .drawWithContent {
                drawContent()
                drawNeonGlow(glowColor, glowRadius)
            },
        shape = RoundedCornerShape(cornerRadius),
        border = BorderStroke(1.dp, Color.Transparent),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
    ) {
        Text(
            text = text, color = Color.White, style = TextStyle(
                fontSize = MaterialTheme.typography.bodyLarge.fontSize, shadow = Shadow(
                    color = glowColor, blurRadius = 10f
                )
            )
        )
    }
}

//@Preview(showBackground = true)
//@Composable
//fun NeonButtonPreview() {
//    NeonRoundedButton(
//        text = "Click Me", onClick = { }, buttonColor = Color.Cyan,  // Neon cyan
//        glowColor = Color.Cyan.copy(alpha = 0.8f), width = 220.dp, height = 55.dp
//    )
//}


