package com.pinfold.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun vector(name: String, build: androidx.compose.ui.graphics.vector.ImageVector.Builder.() -> Unit) =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(build).build()

private fun ImageVector.Builder.line(pathData: androidx.compose.ui.graphics.Path.() -> Unit) {
    path(
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        strokeLineMiter = 4f,
        pathFillType = PathFillType.NonZero,
        build = pathData,
    )
}

object NavIcons {
    val Search = vector("Search") {
        line {
            moveTo(11f, 11f)
            moveTo(10.5f, 6.5f)
            lineTo(6.5f, 6.5f)
            lineTo(6.5f, 16.5f)
            lineTo(16.5f, 16.5f)
            lineTo(16.5f, 11f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            strokeLineMiter = 4f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(14.8f, 14.8f)
            lineTo(18.5f, 18.5f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            strokeLineMiter = 4f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(11f, 6.5f)
            lineTo(16.5f, 6.5f)
            lineTo(16.5f, 11f)
        }
    }

    val Plus = vector("Plus") {
        line {
            moveTo(12f, 5f)
            lineTo(12f, 19f)
            moveTo(5f, 12f)
            lineTo(19f, 12f)
        }
    }

    val Settings = vector("Settings") {
        line {
            moveTo(4f, 7f)
            lineTo(20f, 7f)
            moveTo(4f, 12f)
            lineTo(20f, 12f)
            moveTo(4f, 17f)
            lineTo(20f, 17f)
        }
        path(
            fill = SolidColor(Color.White),
            stroke = null,
            strokeLineWidth = 0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(8f, 5.5f)
            lineTo(9.6f, 7f)
            lineTo(8f, 8.5f)
            close()
            moveTo(15f, 10.5f)
            lineTo(16.6f, 12f)
            lineTo(15f, 13.5f)
            close()
            moveTo(10f, 15.5f)
            lineTo(11.6f, 17f)
            lineTo(10f, 18.5f)
            close()
        }
    }
}
