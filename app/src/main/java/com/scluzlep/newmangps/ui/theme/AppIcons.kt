package com.scluzlep.newmangps.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Inlined Material icons originally sourced from material-icons-extended (Apache 2.0).
 * Inlining avoids packaging all 11k+ unused vector classes when R8 minification is off.
 */
object AppIcons {
    private inline fun materialIcon(
        name: String,
        autoMirror: Boolean = false,
        block: ImageVector.Builder.() -> ImageVector.Builder
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = autoMirror
    ).block().build()

    private inline fun ImageVector.Builder.materialPath(
        fillAlpha: Float = 1f,
        pathFillType: PathFillType = PathFillType.NonZero,
        pathBuilder: PathBuilder.() -> Unit
    ): ImageVector.Builder = path(
        fill = SolidColor(Color.Black),
        fillAlpha = fillAlpha,
        stroke = null,
        strokeAlpha = 1f,
        strokeLineWidth = 1f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Bevel,
        strokeLineMiter = 1f,
        pathFillType = pathFillType,
        pathBuilder = pathBuilder
    )

    val AdminPanelSettings: ImageVector by lazy {
        materialIcon("AdminPanelSettings") {
            materialPath {
                moveTo(17.0f, 11.0f)
                curveToRelative(0.34f, 0.0f, 0.67f, 0.04f, 1.0f, 0.09f)
                verticalLineTo(6.27f)
                lineTo(10.5f, 3.0f)
                lineTo(3.0f, 6.27f)
                verticalLineToRelative(4.91f)
                curveToRelative(0.0f, 4.54f, 3.2f, 8.79f, 7.5f, 9.82f)
                curveToRelative(0.55f, -0.13f, 1.08f, -0.32f, 1.6f, -0.55f)
                curveTo(11.41f, 19.47f, 11.0f, 18.28f, 11.0f, 17.0f)
                curveTo(11.0f, 13.69f, 13.69f, 11.0f, 17.0f, 11.0f)
                close()
            }
            materialPath {
                moveTo(17.0f, 13.0f)
                curveToRelative(-2.21f, 0.0f, -4.0f, 1.79f, -4.0f, 4.0f)
                curveToRelative(0.0f, 2.21f, 1.79f, 4.0f, 4.0f, 4.0f)
                reflectiveCurveToRelative(4.0f, -1.79f, 4.0f, -4.0f)
                curveTo(21.0f, 14.79f, 19.21f, 13.0f, 17.0f, 13.0f)
                close()
                moveTo(17.0f, 14.38f)
                curveToRelative(0.62f, 0.0f, 1.12f, 0.51f, 1.12f, 1.12f)
                reflectiveCurveToRelative(-0.51f, 1.12f, -1.12f, 1.12f)
                reflectiveCurveToRelative(-1.12f, -0.51f, -1.12f, -1.12f)
                reflectiveCurveTo(16.38f, 14.38f, 17.0f, 14.38f)
                close()
                moveTo(17.0f, 19.75f)
                curveToRelative(-0.93f, 0.0f, -1.74f, -0.46f, -2.24f, -1.17f)
                curveToRelative(0.05f, -0.72f, 1.51f, -1.08f, 2.24f, -1.08f)
                reflectiveCurveToRelative(2.19f, 0.36f, 2.24f, 1.08f)
                curveTo(18.74f, 19.29f, 17.93f, 19.75f, 17.0f, 19.75f)
                close()
            }
        }
    }

    val BarChart: ImageVector by lazy {
        materialIcon("BarChart") {
            materialPath {
                moveTo(4.0f, 9.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(11.0f)
                horizontalLineToRelative(-4.0f)
                close()
            }
            materialPath {
                moveTo(16.0f, 13.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(7.0f)
                horizontalLineToRelative(-4.0f)
                close()
            }
            materialPath {
                moveTo(10.0f, 4.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(16.0f)
                horizontalLineToRelative(-4.0f)
                close()
            }
        }
    }

    val BatteryAlert: ImageVector by lazy {
        materialIcon("BatteryAlert") {
            materialPath {
                moveTo(15.67f, 4.0f)
                lineTo(14.0f, 4.0f)
                lineTo(14.0f, 2.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(2.0f)
                lineTo(8.33f, 4.0f)
                curveTo(7.6f, 4.0f, 7.0f, 4.6f, 7.0f, 5.33f)
                verticalLineToRelative(15.33f)
                curveTo(7.0f, 21.4f, 7.6f, 22.0f, 8.33f, 22.0f)
                horizontalLineToRelative(7.33f)
                curveToRelative(0.74f, 0.0f, 1.34f, -0.6f, 1.34f, -1.33f)
                lineTo(17.0f, 5.33f)
                curveTo(17.0f, 4.6f, 16.4f, 4.0f, 15.67f, 4.0f)
                close()
                moveTo(13.0f, 18.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(13.0f, 14.0f)
                horizontalLineToRelative(-2.0f)
                lineTo(11.0f, 9.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(5.0f)
                close()
            }
        }
    }

    val BatteryChargingFull: ImageVector by lazy {
        materialIcon("BatteryChargingFull") {
            materialPath {
                moveTo(15.67f, 4.0f)
                horizontalLineTo(14.0f)
                verticalLineTo(2.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(2.0f)
                horizontalLineTo(8.33f)
                curveTo(7.6f, 4.0f, 7.0f, 4.6f, 7.0f, 5.33f)
                verticalLineToRelative(15.33f)
                curveTo(7.0f, 21.4f, 7.6f, 22.0f, 8.33f, 22.0f)
                horizontalLineToRelative(7.33f)
                curveToRelative(0.74f, 0.0f, 1.34f, -0.6f, 1.34f, -1.33f)
                verticalLineTo(5.33f)
                curveTo(17.0f, 4.6f, 16.4f, 4.0f, 15.67f, 4.0f)
                close()
                moveTo(11.0f, 20.0f)
                verticalLineToRelative(-5.5f)
                horizontalLineTo(9.0f)
                lineTo(13.0f, 7.0f)
                verticalLineToRelative(5.5f)
                horizontalLineToRelative(2.0f)
                lineTo(11.0f, 20.0f)
                close()
            }
        }
    }

    val BatteryFull: ImageVector by lazy {
        materialIcon("BatteryFull") {
            materialPath {
                moveTo(15.67f, 4.0f)
                horizontalLineTo(14.0f)
                verticalLineTo(2.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(2.0f)
                horizontalLineTo(8.33f)
                curveTo(7.6f, 4.0f, 7.0f, 4.6f, 7.0f, 5.33f)
                verticalLineToRelative(15.33f)
                curveTo(7.0f, 21.4f, 7.6f, 22.0f, 8.33f, 22.0f)
                horizontalLineToRelative(7.33f)
                curveToRelative(0.74f, 0.0f, 1.34f, -0.6f, 1.34f, -1.33f)
                verticalLineTo(5.33f)
                curveTo(17.0f, 4.6f, 16.4f, 4.0f, 15.67f, 4.0f)
                close()
            }
        }
    }

    val BookmarkAdd: ImageVector by lazy {
        materialIcon("BookmarkAdd") {
            materialPath {
                moveTo(21.0f, 7.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineTo(7.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineTo(5.0f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(3.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(7.0f)
                close()
                moveTo(19.0f, 21.0f)
                lineToRelative(-7.0f, -3.0f)
                lineToRelative(-7.0f, 3.0f)
                verticalLineTo(5.0f)
                curveToRelative(0.0f, -1.1f, 0.9f, -2.0f, 2.0f, -2.0f)
                lineToRelative(7.0f, 0.0f)
                curveToRelative(-0.63f, 0.84f, -1.0f, 1.87f, -1.0f, 3.0f)
                curveToRelative(0.0f, 2.76f, 2.24f, 5.0f, 5.0f, 5.0f)
                curveToRelative(0.34f, 0.0f, 0.68f, -0.03f, 1.0f, -0.1f)
                verticalLineTo(21.0f)
                close()
            }
        }
    }

    val ContactPage: ImageVector by lazy {
        materialIcon("ContactPage") {
            materialPath {
                moveTo(14.0f, 2.0f)
                horizontalLineTo(6.0f)
                curveTo(4.9f, 2.0f, 4.0f, 2.9f, 4.0f, 4.0f)
                verticalLineToRelative(16.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(12.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineTo(8.0f)
                lineTo(14.0f, 2.0f)
                close()
                moveTo(12.0f, 10.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, 0.9f, 2.0f, 2.0f)
                curveToRelative(0.0f, 1.1f, -0.9f, 2.0f, -2.0f, 2.0f)
                reflectiveCurveToRelative(-2.0f, -0.9f, -2.0f, -2.0f)
                curveTo(10.0f, 10.9f, 10.9f, 10.0f, 12.0f, 10.0f)
                close()
                moveTo(16.0f, 18.0f)
                horizontalLineTo(8.0f)
                verticalLineToRelative(-0.57f)
                curveToRelative(0.0f, -0.81f, 0.48f, -1.53f, 1.22f, -1.85f)
                curveTo(10.07f, 15.21f, 11.01f, 15.0f, 12.0f, 15.0f)
                curveToRelative(0.99f, 0.0f, 1.93f, 0.21f, 2.78f, 0.58f)
                curveTo(15.52f, 15.9f, 16.0f, 16.62f, 16.0f, 17.43f)
                verticalLineTo(18.0f)
                close()
            }
        }
    }

    val Devices: ImageVector by lazy {
        materialIcon("Devices") {
            materialPath {
                moveTo(4.0f, 6.0f)
                horizontalLineToRelative(18.0f)
                lineTo(22.0f, 4.0f)
                lineTo(4.0f, 4.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(11.0f)
                lineTo(0.0f, 17.0f)
                verticalLineToRelative(3.0f)
                horizontalLineToRelative(14.0f)
                verticalLineToRelative(-3.0f)
                lineTo(4.0f, 17.0f)
                lineTo(4.0f, 6.0f)
                close()
                moveTo(23.0f, 8.0f)
                horizontalLineToRelative(-6.0f)
                curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                verticalLineToRelative(10.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                horizontalLineToRelative(6.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                lineTo(24.0f, 9.0f)
                curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                close()
                moveTo(22.0f, 17.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(-7.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(7.0f)
                close()
            }
        }
    }

    val DirectionsCar: ImageVector by lazy {
        materialIcon("DirectionsCar") {
            materialPath {
                moveTo(18.92f, 6.01f)
                curveTo(18.72f, 5.42f, 18.16f, 5.0f, 17.5f, 5.0f)
                horizontalLineToRelative(-11.0f)
                curveToRelative(-0.66f, 0.0f, -1.21f, 0.42f, -1.42f, 1.01f)
                lineTo(3.0f, 12.0f)
                verticalLineToRelative(8.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                horizontalLineToRelative(1.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                verticalLineToRelative(-1.0f)
                horizontalLineToRelative(12.0f)
                verticalLineToRelative(1.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                horizontalLineToRelative(1.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                verticalLineToRelative(-8.0f)
                lineToRelative(-2.08f, -5.99f)
                close()
                moveTo(6.5f, 16.0f)
                curveToRelative(-0.83f, 0.0f, -1.5f, -0.67f, -1.5f, -1.5f)
                reflectiveCurveTo(5.67f, 13.0f, 6.5f, 13.0f)
                reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                reflectiveCurveTo(7.33f, 16.0f, 6.5f, 16.0f)
                close()
                moveTo(17.5f, 16.0f)
                curveToRelative(-0.83f, 0.0f, -1.5f, -0.67f, -1.5f, -1.5f)
                reflectiveCurveToRelative(0.67f, -1.5f, 1.5f, -1.5f)
                reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                reflectiveCurveToRelative(-0.67f, 1.5f, -1.5f, 1.5f)
                close()
                moveTo(5.0f, 11.0f)
                lineToRelative(1.5f, -4.5f)
                horizontalLineToRelative(11.0f)
                lineTo(19.0f, 11.0f)
                lineTo(5.0f, 11.0f)
                close()
            }
        }
    }

    val ExpandLess: ImageVector by lazy {
        materialIcon("ExpandLess") {
            materialPath {
                moveTo(12.0f, 8.0f)
                lineToRelative(-6.0f, 6.0f)
                lineToRelative(1.41f, 1.41f)
                lineTo(12.0f, 10.83f)
                lineToRelative(4.59f, 4.58f)
                lineTo(18.0f, 14.0f)
                close()
            }
        }
    }

    val ExpandMore: ImageVector by lazy {
        materialIcon("ExpandMore") {
            materialPath {
                moveTo(16.59f, 8.59f)
                lineTo(12.0f, 13.17f)
                lineTo(7.41f, 8.59f)
                lineTo(6.0f, 10.0f)
                lineToRelative(6.0f, 6.0f)
                lineToRelative(6.0f, -6.0f)
                close()
            }
        }
    }

    val FileUpload: ImageVector by lazy {
        materialIcon("FileUpload") {
            materialPath {
                moveTo(9.0f, 16.0f)
                horizontalLineToRelative(6.0f)
                verticalLineToRelative(-6.0f)
                horizontalLineToRelative(4.0f)
                lineToRelative(-7.0f, -7.0f)
                lineToRelative(-7.0f, 7.0f)
                horizontalLineToRelative(4.0f)
                close()
                moveTo(5.0f, 18.0f)
                horizontalLineToRelative(14.0f)
                verticalLineToRelative(2.0f)
                lineTo(5.0f, 20.0f)
                close()
            }
        }
    }

    val GridView: ImageVector by lazy {
        materialIcon("GridView") {
            materialPath(pathFillType = PathFillType.EvenOdd) {
                moveTo(3.0f, 3.0f)
                verticalLineToRelative(8.0f)
                horizontalLineToRelative(8.0f)
                lineTo(11.0f, 3.0f)
                lineTo(3.0f, 3.0f)
                close()
                moveTo(9.0f, 9.0f)
                lineTo(5.0f, 9.0f)
                lineTo(5.0f, 5.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(4.0f)
                close()
                moveTo(3.0f, 13.0f)
                verticalLineToRelative(8.0f)
                horizontalLineToRelative(8.0f)
                verticalLineToRelative(-8.0f)
                lineTo(3.0f, 13.0f)
                close()
                moveTo(9.0f, 19.0f)
                lineTo(5.0f, 19.0f)
                verticalLineToRelative(-4.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(4.0f)
                close()
                moveTo(13.0f, 3.0f)
                verticalLineToRelative(8.0f)
                horizontalLineToRelative(8.0f)
                lineTo(21.0f, 3.0f)
                horizontalLineToRelative(-8.0f)
                close()
                moveTo(19.0f, 9.0f)
                horizontalLineToRelative(-4.0f)
                lineTo(15.0f, 5.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(4.0f)
                close()
                moveTo(13.0f, 13.0f)
                verticalLineToRelative(8.0f)
                horizontalLineToRelative(8.0f)
                verticalLineToRelative(-8.0f)
                horizontalLineToRelative(-8.0f)
                close()
                moveTo(19.0f, 19.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(-4.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(4.0f)
                close()
            }
        }
    }

    val Groups: ImageVector by lazy {
        materialIcon("Groups") {
            materialPath {
                moveTo(12.0f, 12.75f)
                curveToRelative(1.63f, 0.0f, 3.07f, 0.39f, 4.24f, 0.9f)
                curveToRelative(1.08f, 0.48f, 1.76f, 1.56f, 1.76f, 2.73f)
                lineTo(18.0f, 18.0f)
                horizontalLineTo(6.0f)
                lineToRelative(0.0f, -1.61f)
                curveToRelative(0.0f, -1.18f, 0.68f, -2.26f, 1.76f, -2.73f)
                curveTo(8.93f, 13.14f, 10.37f, 12.75f, 12.0f, 12.75f)
                close()
                moveTo(4.0f, 13.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                reflectiveCurveToRelative(-2.0f, 0.9f, -2.0f, 2.0f)
                curveTo(2.0f, 12.1f, 2.9f, 13.0f, 4.0f, 13.0f)
                close()
                moveTo(5.13f, 14.1f)
                curveTo(4.76f, 14.04f, 4.39f, 14.0f, 4.0f, 14.0f)
                curveToRelative(-0.99f, 0.0f, -1.93f, 0.21f, -2.78f, 0.58f)
                curveTo(0.48f, 14.9f, 0.0f, 15.62f, 0.0f, 16.43f)
                verticalLineTo(18.0f)
                lineToRelative(4.5f, 0.0f)
                verticalLineToRelative(-1.61f)
                curveTo(4.5f, 15.56f, 4.73f, 14.78f, 5.13f, 14.1f)
                close()
                moveTo(20.0f, 13.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                reflectiveCurveToRelative(-2.0f, 0.9f, -2.0f, 2.0f)
                curveTo(18.0f, 12.1f, 18.9f, 13.0f, 20.0f, 13.0f)
                close()
                moveTo(24.0f, 16.43f)
                curveToRelative(0.0f, -0.81f, -0.48f, -1.53f, -1.22f, -1.85f)
                curveTo(21.93f, 14.21f, 20.99f, 14.0f, 20.0f, 14.0f)
                curveToRelative(-0.39f, 0.0f, -0.76f, 0.04f, -1.13f, 0.1f)
                curveToRelative(0.4f, 0.68f, 0.63f, 1.46f, 0.63f, 2.29f)
                verticalLineTo(18.0f)
                lineToRelative(4.5f, 0.0f)
                verticalLineTo(16.43f)
                close()
                moveTo(12.0f, 6.0f)
                curveToRelative(1.66f, 0.0f, 3.0f, 1.34f, 3.0f, 3.0f)
                curveToRelative(0.0f, 1.66f, -1.34f, 3.0f, -3.0f, 3.0f)
                reflectiveCurveToRelative(-3.0f, -1.34f, -3.0f, -3.0f)
                curveTo(9.0f, 7.34f, 10.34f, 6.0f, 12.0f, 6.0f)
                close()
            }
        }
    }

    val History: ImageVector by lazy {
        materialIcon("History") {
            materialPath {
                moveTo(13.0f, 3.0f)
                curveToRelative(-4.97f, 0.0f, -9.0f, 4.03f, -9.0f, 9.0f)
                lineTo(1.0f, 12.0f)
                lineToRelative(3.89f, 3.89f)
                lineToRelative(0.07f, 0.14f)
                lineTo(9.0f, 12.0f)
                lineTo(6.0f, 12.0f)
                curveToRelative(0.0f, -3.87f, 3.13f, -7.0f, 7.0f, -7.0f)
                reflectiveCurveToRelative(7.0f, 3.13f, 7.0f, 7.0f)
                reflectiveCurveToRelative(-3.13f, 7.0f, -7.0f, 7.0f)
                curveToRelative(-1.93f, 0.0f, -3.68f, -0.79f, -4.94f, -2.06f)
                lineToRelative(-1.42f, 1.42f)
                curveTo(8.27f, 19.99f, 10.51f, 21.0f, 13.0f, 21.0f)
                curveToRelative(4.97f, 0.0f, 9.0f, -4.03f, 9.0f, -9.0f)
                reflectiveCurveToRelative(-4.03f, -9.0f, -9.0f, -9.0f)
                close()
                moveTo(12.0f, 8.0f)
                verticalLineToRelative(5.0f)
                lineToRelative(4.28f, 2.54f)
                lineToRelative(0.72f, -1.21f)
                lineToRelative(-3.5f, -2.08f)
                lineTo(13.5f, 8.0f)
                lineTo(12.0f, 8.0f)
                close()
            }
        }
    }

    val Image: ImageVector by lazy {
        materialIcon("Image") {
            materialPath {
                moveTo(21.0f, 19.0f)
                verticalLineTo(5.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                horizontalLineTo(5.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(14.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(14.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                close()
                moveTo(8.5f, 13.5f)
                lineToRelative(2.5f, 3.01f)
                lineTo(14.5f, 12.0f)
                lineToRelative(4.5f, 6.0f)
                horizontalLineTo(5.0f)
                lineToRelative(3.5f, -4.5f)
                close()
            }
        }
    }

    val Layers: ImageVector by lazy {
        materialIcon("Layers") {
            materialPath {
                moveTo(11.99f, 18.54f)
                lineToRelative(-7.37f, -5.73f)
                lineTo(3.0f, 14.07f)
                lineToRelative(9.0f, 7.0f)
                lineToRelative(9.0f, -7.0f)
                lineToRelative(-1.63f, -1.27f)
                lineToRelative(-7.38f, 5.74f)
                close()
                moveTo(12.0f, 16.0f)
                lineToRelative(7.36f, -5.73f)
                lineTo(21.0f, 9.0f)
                lineToRelative(-9.0f, -7.0f)
                lineToRelative(-9.0f, 7.0f)
                lineToRelative(1.63f, 1.27f)
                lineTo(12.0f, 16.0f)
                close()
            }
        }
    }

    val LocationSearching: ImageVector by lazy {
        materialIcon("LocationSearching") {
            materialPath {
                moveTo(20.94f, 11.0f)
                curveToRelative(-0.46f, -4.17f, -3.77f, -7.48f, -7.94f, -7.94f)
                verticalLineTo(1.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(2.06f)
                curveTo(6.83f, 3.52f, 3.52f, 6.83f, 3.06f, 11.0f)
                horizontalLineTo(1.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(2.06f)
                curveToRelative(0.46f, 4.17f, 3.77f, 7.48f, 7.94f, 7.94f)
                verticalLineTo(23.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(-2.06f)
                curveToRelative(4.17f, -0.46f, 7.48f, -3.77f, 7.94f, -7.94f)
                horizontalLineTo(23.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(-2.06f)
                close()
                moveTo(12.0f, 19.0f)
                curveToRelative(-3.87f, 0.0f, -7.0f, -3.13f, -7.0f, -7.0f)
                reflectiveCurveToRelative(3.13f, -7.0f, 7.0f, -7.0f)
                reflectiveCurveToRelative(7.0f, 3.13f, 7.0f, 7.0f)
                reflectiveCurveToRelative(-3.13f, 7.0f, -7.0f, 7.0f)
                close()
            }
        }
    }

    val LockReset: ImageVector by lazy {
        materialIcon("LockReset") {
            materialPath {
                moveTo(13.0f, 3.0f)
                curveToRelative(-4.97f, 0.0f, -9.0f, 4.03f, -9.0f, 9.0f)
                horizontalLineTo(1.0f)
                lineToRelative(4.0f, 4.0f)
                lineToRelative(4.0f, -4.0f)
                horizontalLineTo(6.0f)
                curveToRelative(0.0f, -3.86f, 3.14f, -7.0f, 7.0f, -7.0f)
                reflectiveCurveToRelative(7.0f, 3.14f, 7.0f, 7.0f)
                reflectiveCurveToRelative(-3.14f, 7.0f, -7.0f, 7.0f)
                curveToRelative(-1.9f, 0.0f, -3.62f, -0.76f, -4.88f, -1.99f)
                lineTo(6.7f, 18.42f)
                curveTo(8.32f, 20.01f, 10.55f, 21.0f, 13.0f, 21.0f)
                curveToRelative(4.97f, 0.0f, 9.0f, -4.03f, 9.0f, -9.0f)
                reflectiveCurveTo(17.97f, 3.0f, 13.0f, 3.0f)
                close()
                moveTo(15.0f, 11.0f)
                verticalLineToRelative(-1.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                reflectiveCurveToRelative(-2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(1.0f)
                curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                verticalLineToRelative(3.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                horizontalLineToRelative(4.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                verticalLineToRelative(-3.0f)
                curveTo(16.0f, 11.45f, 15.55f, 11.0f, 15.0f, 11.0f)
                close()
                moveTo(14.0f, 11.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(-1.0f)
                curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                reflectiveCurveToRelative(1.0f, 0.45f, 1.0f, 1.0f)
                verticalLineTo(11.0f)
                close()
            }
        }
    }

    val Map: ImageVector by lazy {
        materialIcon("Map") {
            materialPath {
                moveTo(20.5f, 3.0f)
                lineToRelative(-0.16f, 0.03f)
                lineTo(15.0f, 5.1f)
                lineTo(9.0f, 3.0f)
                lineTo(3.36f, 4.9f)
                curveToRelative(-0.21f, 0.07f, -0.36f, 0.25f, -0.36f, 0.48f)
                verticalLineTo(20.5f)
                curveToRelative(0.0f, 0.28f, 0.22f, 0.5f, 0.5f, 0.5f)
                lineToRelative(0.16f, -0.03f)
                lineTo(9.0f, 18.9f)
                lineToRelative(6.0f, 2.1f)
                lineToRelative(5.64f, -1.9f)
                curveToRelative(0.21f, -0.07f, 0.36f, -0.25f, 0.36f, -0.48f)
                verticalLineTo(3.5f)
                curveToRelative(0.0f, -0.28f, -0.22f, -0.5f, -0.5f, -0.5f)
                close()
                moveTo(15.0f, 19.0f)
                lineToRelative(-6.0f, -2.11f)
                verticalLineTo(5.0f)
                lineToRelative(6.0f, 2.11f)
                verticalLineTo(19.0f)
                close()
            }
        }
    }

    val MonitorHeart: ImageVector by lazy {
        materialIcon("MonitorHeart") {
            materialPath {
                moveTo(15.11f, 12.45f)
                lineTo(14.0f, 10.24f)
                lineToRelative(-3.11f, 6.21f)
                curveTo(10.73f, 16.79f, 10.38f, 17.0f, 10.0f, 17.0f)
                reflectiveCurveToRelative(-0.73f, -0.21f, -0.89f, -0.55f)
                lineTo(7.38f, 13.0f)
                horizontalLineTo(2.0f)
                verticalLineToRelative(5.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(16.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineToRelative(-5.0f)
                horizontalLineToRelative(-6.0f)
                curveTo(15.62f, 13.0f, 15.27f, 12.79f, 15.11f, 12.45f)
                close()
            }
            materialPath {
                moveTo(20.0f, 4.0f)
                horizontalLineTo(4.0f)
                curveTo(2.9f, 4.0f, 2.0f, 4.9f, 2.0f, 6.0f)
                verticalLineToRelative(5.0f)
                horizontalLineToRelative(6.0f)
                curveToRelative(0.38f, 0.0f, 0.73f, 0.21f, 0.89f, 0.55f)
                lineTo(10.0f, 13.76f)
                lineToRelative(3.11f, -6.21f)
                curveToRelative(0.34f, -0.68f, 1.45f, -0.68f, 1.79f, 0.0f)
                lineTo(16.62f, 11.0f)
                horizontalLineTo(22.0f)
                verticalLineTo(6.0f)
                curveTo(22.0f, 4.9f, 21.1f, 4.0f, 20.0f, 4.0f)
                close()
            }
        }
    }

    val MyLocation: ImageVector by lazy {
        materialIcon("MyLocation") {
            materialPath {
                moveTo(12.0f, 8.0f)
                curveToRelative(-2.21f, 0.0f, -4.0f, 1.79f, -4.0f, 4.0f)
                reflectiveCurveToRelative(1.79f, 4.0f, 4.0f, 4.0f)
                reflectiveCurveToRelative(4.0f, -1.79f, 4.0f, -4.0f)
                reflectiveCurveToRelative(-1.79f, -4.0f, -4.0f, -4.0f)
                close()
                moveTo(20.94f, 11.0f)
                curveToRelative(-0.46f, -4.17f, -3.77f, -7.48f, -7.94f, -7.94f)
                lineTo(13.0f, 1.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(2.06f)
                curveTo(6.83f, 3.52f, 3.52f, 6.83f, 3.06f, 11.0f)
                lineTo(1.0f, 11.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(2.06f)
                curveToRelative(0.46f, 4.17f, 3.77f, 7.48f, 7.94f, 7.94f)
                lineTo(11.0f, 23.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(-2.06f)
                curveToRelative(4.17f, -0.46f, 7.48f, -3.77f, 7.94f, -7.94f)
                lineTo(23.0f, 13.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(-2.06f)
                close()
                moveTo(12.0f, 19.0f)
                curveToRelative(-3.87f, 0.0f, -7.0f, -3.13f, -7.0f, -7.0f)
                reflectiveCurveToRelative(3.13f, -7.0f, 7.0f, -7.0f)
                reflectiveCurveToRelative(7.0f, 3.13f, 7.0f, 7.0f)
                reflectiveCurveToRelative(-3.13f, 7.0f, -7.0f, 7.0f)
                close()
            }
        }
    }

    val Navigation: ImageVector by lazy {
        materialIcon("Navigation") {
            materialPath {
                moveTo(12.0f, 2.0f)
                lineTo(4.5f, 20.29f)
                lineToRelative(0.71f, 0.71f)
                lineTo(12.0f, 18.0f)
                lineToRelative(6.79f, 3.0f)
                lineToRelative(0.71f, -0.71f)
                close()
            }
        }
    }

    val NotificationsActive: ImageVector by lazy {
        materialIcon("NotificationsActive") {
            materialPath {
                moveTo(7.58f, 4.08f)
                lineTo(6.15f, 2.65f)
                curveTo(3.75f, 4.48f, 2.17f, 7.3f, 2.03f, 10.5f)
                horizontalLineToRelative(2.0f)
                curveToRelative(0.15f, -2.65f, 1.51f, -4.97f, 3.55f, -6.42f)
                close()
                moveTo(19.97f, 10.5f)
                horizontalLineToRelative(2.0f)
                curveToRelative(-0.15f, -3.2f, -1.73f, -6.02f, -4.12f, -7.85f)
                lineToRelative(-1.42f, 1.43f)
                curveToRelative(2.02f, 1.45f, 3.39f, 3.77f, 3.54f, 6.42f)
                close()
                moveTo(18.0f, 11.0f)
                curveToRelative(0.0f, -3.07f, -1.64f, -5.64f, -4.5f, -6.32f)
                lineTo(13.5f, 4.0f)
                curveToRelative(0.0f, -0.83f, -0.67f, -1.5f, -1.5f, -1.5f)
                reflectiveCurveToRelative(-1.5f, 0.67f, -1.5f, 1.5f)
                verticalLineToRelative(0.68f)
                curveTo(7.63f, 5.36f, 6.0f, 7.92f, 6.0f, 11.0f)
                verticalLineToRelative(5.0f)
                lineToRelative(-2.0f, 2.0f)
                verticalLineToRelative(1.0f)
                horizontalLineToRelative(16.0f)
                verticalLineToRelative(-1.0f)
                lineToRelative(-2.0f, -2.0f)
                verticalLineToRelative(-5.0f)
                close()
                moveTo(12.0f, 22.0f)
                curveToRelative(0.14f, 0.0f, 0.27f, -0.01f, 0.4f, -0.04f)
                curveToRelative(0.65f, -0.14f, 1.18f, -0.58f, 1.44f, -1.18f)
                curveToRelative(0.1f, -0.24f, 0.15f, -0.5f, 0.15f, -0.78f)
                horizontalLineToRelative(-4.0f)
                curveToRelative(0.01f, 1.1f, 0.9f, 2.0f, 2.01f, 2.0f)
                close()
            }
        }
    }

    val Numbers: ImageVector by lazy {
        materialIcon("Numbers") {
            materialPath {
                moveTo(20.5f, 10.0f)
                lineTo(21.0f, 8.0f)
                horizontalLineToRelative(-4.0f)
                lineToRelative(1.0f, -4.0f)
                horizontalLineToRelative(-2.0f)
                lineToRelative(-1.0f, 4.0f)
                horizontalLineToRelative(-4.0f)
                lineToRelative(1.0f, -4.0f)
                horizontalLineToRelative(-2.0f)
                lineTo(9.0f, 8.0f)
                horizontalLineTo(5.0f)
                lineToRelative(-0.5f, 2.0f)
                horizontalLineToRelative(4.0f)
                lineToRelative(-1.0f, 4.0f)
                horizontalLineToRelative(-4.0f)
                lineTo(3.0f, 16.0f)
                horizontalLineToRelative(4.0f)
                lineToRelative(-1.0f, 4.0f)
                horizontalLineToRelative(2.0f)
                lineToRelative(1.0f, -4.0f)
                horizontalLineToRelative(4.0f)
                lineToRelative(-1.0f, 4.0f)
                horizontalLineToRelative(2.0f)
                lineToRelative(1.0f, -4.0f)
                horizontalLineToRelative(4.0f)
                lineToRelative(0.5f, -2.0f)
                horizontalLineToRelative(-4.0f)
                lineToRelative(1.0f, -4.0f)
                horizontalLineTo(20.5f)
                close()
                moveTo(13.5f, 14.0f)
                horizontalLineToRelative(-4.0f)
                lineToRelative(1.0f, -4.0f)
                horizontalLineToRelative(4.0f)
                lineTo(13.5f, 14.0f)
                close()
            }
        }
    }

    val OpenInNew: ImageVector by lazy {
        materialIcon("OpenInNew") {
            materialPath {
                moveTo(19.0f, 19.0f)
                horizontalLineTo(5.0f)
                verticalLineTo(5.0f)
                horizontalLineToRelative(7.0f)
                verticalLineTo(3.0f)
                horizontalLineTo(5.0f)
                curveToRelative(-1.11f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(14.0f)
                curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(14.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineToRelative(-7.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(7.0f)
                close()
                moveTo(14.0f, 3.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(3.59f)
                lineToRelative(-9.83f, 9.83f)
                lineToRelative(1.41f, 1.41f)
                lineTo(19.0f, 6.41f)
                verticalLineTo(10.0f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(3.0f)
                horizontalLineToRelative(-7.0f)
                close()
            }
        }
    }

    val Pause: ImageVector by lazy {
        materialIcon("Pause") {
            materialPath {
                moveTo(6.0f, 19.0f)
                horizontalLineToRelative(4.0f)
                lineTo(10.0f, 5.0f)
                lineTo(6.0f, 5.0f)
                verticalLineToRelative(14.0f)
                close()
                moveTo(14.0f, 5.0f)
                verticalLineToRelative(14.0f)
                horizontalLineToRelative(4.0f)
                lineTo(18.0f, 5.0f)
                horizontalLineToRelative(-4.0f)
                close()
            }
        }
    }

    val PersonAdd: ImageVector by lazy {
        materialIcon("PersonAdd") {
            materialPath {
                moveTo(15.0f, 12.0f)
                curveToRelative(2.21f, 0.0f, 4.0f, -1.79f, 4.0f, -4.0f)
                reflectiveCurveToRelative(-1.79f, -4.0f, -4.0f, -4.0f)
                reflectiveCurveToRelative(-4.0f, 1.79f, -4.0f, 4.0f)
                reflectiveCurveToRelative(1.79f, 4.0f, 4.0f, 4.0f)
                close()
                moveTo(6.0f, 10.0f)
                lineTo(6.0f, 7.0f)
                lineTo(4.0f, 7.0f)
                verticalLineToRelative(3.0f)
                lineTo(1.0f, 10.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(3.0f)
                verticalLineToRelative(3.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(-3.0f)
                horizontalLineToRelative(3.0f)
                verticalLineToRelative(-2.0f)
                lineTo(6.0f, 10.0f)
                close()
                moveTo(15.0f, 14.0f)
                curveToRelative(-2.67f, 0.0f, -8.0f, 1.34f, -8.0f, 4.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(16.0f)
                verticalLineToRelative(-2.0f)
                curveToRelative(0.0f, -2.66f, -5.33f, -4.0f, -8.0f, -4.0f)
                close()
            }
        }
    }

    val PhotoLibrary: ImageVector by lazy {
        materialIcon("PhotoLibrary") {
            materialPath {
                moveTo(22.0f, 16.0f)
                lineTo(22.0f, 4.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                lineTo(8.0f, 2.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(12.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(12.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                close()
                moveTo(11.0f, 12.0f)
                lineToRelative(2.03f, 2.71f)
                lineTo(16.0f, 11.0f)
                lineToRelative(4.0f, 5.0f)
                lineTo(8.0f, 16.0f)
                lineToRelative(3.0f, -4.0f)
                close()
                moveTo(2.0f, 6.0f)
                verticalLineToRelative(14.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(14.0f)
                verticalLineToRelative(-2.0f)
                lineTo(4.0f, 20.0f)
                lineTo(4.0f, 6.0f)
                lineTo(2.0f, 6.0f)
                close()
            }
        }
    }

    val Radar: ImageVector by lazy {
        materialIcon("Radar") {
            materialPath {
                moveTo(19.74f, 18.33f)
                curveTo(21.15f, 16.6f, 22.0f, 14.4f, 22.0f, 12.0f)
                curveToRelative(0.0f, -5.52f, -4.48f, -10.0f, -10.0f, -10.0f)
                reflectiveCurveTo(2.0f, 6.48f, 2.0f, 12.0f)
                reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
                curveToRelative(2.4f, 0.0f, 4.6f, -0.85f, 6.33f, -2.26f)
                curveToRelative(0.27f, -0.22f, 0.53f, -0.46f, 0.78f, -0.71f)
                curveToRelative(0.03f, -0.03f, 0.05f, -0.06f, 0.07f, -0.08f)
                curveTo(19.38f, 18.75f, 19.57f, 18.54f, 19.74f, 18.33f)
                close()
                moveTo(12.0f, 20.0f)
                curveToRelative(-4.41f, 0.0f, -8.0f, -3.59f, -8.0f, -8.0f)
                reflectiveCurveToRelative(3.59f, -8.0f, 8.0f, -8.0f)
                reflectiveCurveToRelative(8.0f, 3.59f, 8.0f, 8.0f)
                curveToRelative(0.0f, 1.85f, -0.63f, 3.54f, -1.69f, 4.9f)
                lineToRelative(-1.43f, -1.43f)
                curveToRelative(0.69f, -0.98f, 1.1f, -2.17f, 1.1f, -3.46f)
                curveToRelative(0.0f, -3.31f, -2.69f, -6.0f, -6.0f, -6.0f)
                reflectiveCurveToRelative(-6.0f, 2.69f, -6.0f, 6.0f)
                reflectiveCurveToRelative(2.69f, 6.0f, 6.0f, 6.0f)
                curveToRelative(1.3f, 0.0f, 2.51f, -0.42f, 3.49f, -1.13f)
                lineToRelative(1.42f, 1.42f)
                curveTo(15.54f, 19.37f, 13.85f, 20.0f, 12.0f, 20.0f)
                close()
                moveTo(13.92f, 12.51f)
                curveToRelative(0.17f, -0.66f, 0.02f, -1.38f, -0.49f, -1.9f)
                lineToRelative(-0.02f, -0.02f)
                curveToRelative(-0.77f, -0.77f, -2.0f, -0.78f, -2.78f, -0.04f)
                curveToRelative(-0.01f, 0.01f, -0.03f, 0.02f, -0.05f, 0.04f)
                curveToRelative(-0.78f, 0.78f, -0.78f, 2.05f, 0.0f, 2.83f)
                lineToRelative(0.02f, 0.02f)
                curveToRelative(0.52f, 0.51f, 1.25f, 0.67f, 1.91f, 0.49f)
                lineToRelative(1.51f, 1.51f)
                curveToRelative(-0.6f, 0.36f, -1.29f, 0.58f, -2.04f, 0.58f)
                curveToRelative(-2.21f, 0.0f, -4.0f, -1.79f, -4.0f, -4.0f)
                reflectiveCurveToRelative(1.79f, -4.0f, 4.0f, -4.0f)
                reflectiveCurveToRelative(4.0f, 1.79f, 4.0f, 4.0f)
                curveToRelative(0.0f, 0.73f, -0.21f, 1.41f, -0.56f, 2.0f)
                lineTo(13.92f, 12.51f)
                close()
            }
        }
    }

    val SatelliteAlt: ImageVector by lazy {
        materialIcon("SatelliteAlt") {
            materialPath {
                moveTo(15.44f, 0.59f)
                lineToRelative(-3.18f, 3.18f)
                curveToRelative(-0.78f, 0.78f, -0.78f, 2.05f, 0.0f, 2.83f)
                lineToRelative(1.24f, 1.24f)
                lineToRelative(-0.71f, 0.71f)
                lineTo(11.55f, 7.3f)
                curveToRelative(-0.78f, -0.78f, -2.05f, -0.78f, -2.83f, 0.0f)
                lineTo(7.3f, 8.72f)
                curveToRelative(-0.78f, 0.78f, -0.78f, 2.05f, 0.0f, 2.83f)
                lineToRelative(1.24f, 1.24f)
                lineToRelative(-0.71f, 0.71f)
                lineTo(6.6f, 12.25f)
                curveToRelative(-0.78f, -0.78f, -2.05f, -0.78f, -2.83f, 0.0f)
                lineToRelative(-3.18f, 3.18f)
                curveToRelative(-0.78f, 0.78f, -0.78f, 2.05f, 0.0f, 2.83f)
                lineToRelative(3.54f, 3.54f)
                curveToRelative(0.78f, 0.78f, 2.05f, 0.78f, 2.83f, 0.0f)
                lineToRelative(3.18f, -3.18f)
                curveToRelative(0.78f, -0.78f, 0.78f, -2.05f, 0.0f, -2.83f)
                lineToRelative(-1.24f, -1.24f)
                lineToRelative(0.71f, -0.71f)
                lineToRelative(1.24f, 1.24f)
                curveToRelative(0.78f, 0.78f, 2.05f, 0.78f, 2.83f, 0.0f)
                lineToRelative(1.41f, -1.41f)
                curveToRelative(0.78f, -0.78f, 0.78f, -2.05f, 0.0f, -2.83f)
                lineTo(13.84f, 9.6f)
                lineToRelative(0.71f, -0.71f)
                lineToRelative(1.24f, 1.24f)
                curveToRelative(0.78f, 0.78f, 2.05f, 0.78f, 2.83f, 0.0f)
                lineToRelative(3.18f, -3.18f)
                curveToRelative(0.78f, -0.78f, 0.78f, -2.05f, 0.0f, -2.83f)
                lineToRelative(-3.54f, -3.54f)
                curveTo(17.48f, -0.2f, 16.22f, -0.2f, 15.44f, 0.59f)
                close()
                moveTo(6.6f, 19.32f)
                lineToRelative(-1.06f, 1.06f)
                lineTo(2.0f, 16.85f)
                lineToRelative(1.06f, -1.06f)
                lineTo(6.6f, 19.32f)
                close()
                moveTo(8.72f, 17.2f)
                lineToRelative(-1.06f, 1.06f)
                lineToRelative(-3.54f, -3.54f)
                lineToRelative(1.06f, -1.06f)
                lineTo(8.72f, 17.2f)
                close()
                moveTo(18.26f, 7.66f)
                lineTo(17.2f, 8.72f)
                lineToRelative(-3.54f, -3.54f)
                lineToRelative(1.06f, -1.06f)
                lineTo(18.26f, 7.66f)
                close()
                moveTo(20.38f, 5.54f)
                lineTo(19.32f, 6.6f)
                lineToRelative(-3.54f, -3.54f)
                lineTo(16.85f, 2.0f)
                lineTo(20.38f, 5.54f)
                close()
                moveTo(14.0f, 21.0f)
                lineToRelative(0.0f, 2.0f)
                curveToRelative(4.97f, 0.0f, 9.0f, -4.03f, 9.0f, -9.0f)
                lineToRelative(-2.0f, 0.0f)
                curveTo(21.0f, 17.87f, 17.87f, 21.0f, 14.0f, 21.0f)
                close()
                moveTo(14.0f, 17.0f)
                lineToRelative(0.0f, 2.0f)
                curveToRelative(2.76f, 0.0f, 5.0f, -2.24f, 5.0f, -5.0f)
                lineToRelative(-2.0f, 0.0f)
                curveTo(17.0f, 15.66f, 15.66f, 17.0f, 14.0f, 17.0f)
                close()
            }
        }
    }

    val SettingsRemote: ImageVector by lazy {
        materialIcon("SettingsRemote") {
            materialPath {
                moveTo(15.0f, 9.0f)
                lineTo(9.0f, 9.0f)
                curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                verticalLineToRelative(12.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                horizontalLineToRelative(6.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                lineTo(16.0f, 10.0f)
                curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                close()
                moveTo(12.0f, 15.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, -0.9f, -2.0f, -2.0f)
                reflectiveCurveToRelative(0.9f, -2.0f, 2.0f, -2.0f)
                reflectiveCurveToRelative(2.0f, 0.9f, 2.0f, 2.0f)
                reflectiveCurveToRelative(-0.9f, 2.0f, -2.0f, 2.0f)
                close()
                moveTo(7.05f, 6.05f)
                lineToRelative(1.41f, 1.41f)
                curveTo(9.37f, 6.56f, 10.62f, 6.0f, 12.0f, 6.0f)
                reflectiveCurveToRelative(2.63f, 0.56f, 3.54f, 1.46f)
                lineToRelative(1.41f, -1.41f)
                curveTo(15.68f, 4.78f, 13.93f, 4.0f, 12.0f, 4.0f)
                reflectiveCurveToRelative(-3.68f, 0.78f, -4.95f, 2.05f)
                close()
                moveTo(12.0f, 0.0f)
                curveTo(8.96f, 0.0f, 6.21f, 1.23f, 4.22f, 3.22f)
                lineToRelative(1.41f, 1.41f)
                curveTo(7.26f, 3.01f, 9.51f, 2.0f, 12.0f, 2.0f)
                reflectiveCurveToRelative(4.74f, 1.01f, 6.36f, 2.64f)
                lineToRelative(1.41f, -1.41f)
                curveTo(17.79f, 1.23f, 15.04f, 0.0f, 12.0f, 0.0f)
                close()
            }
        }
    }

    val Shield: ImageVector by lazy {
        materialIcon("Shield") {
            materialPath {
                moveTo(12.0f, 1.0f)
                lineTo(3.0f, 5.0f)
                verticalLineToRelative(6.0f)
                curveToRelative(0.0f, 5.55f, 3.84f, 10.74f, 9.0f, 12.0f)
                curveToRelative(5.16f, -1.26f, 9.0f, -6.45f, 9.0f, -12.0f)
                verticalLineTo(5.0f)
                lineToRelative(-9.0f, -4.0f)
                close()
            }
        }
    }

    val SimCard: ImageVector by lazy {
        materialIcon("SimCard") {
            materialPath {
                moveTo(19.99f, 4.0f)
                curveToRelative(0.0f, -1.1f, -0.89f, -2.0f, -1.99f, -2.0f)
                horizontalLineToRelative(-8.0f)
                lineTo(4.0f, 8.0f)
                verticalLineToRelative(12.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(12.01f)
                curveToRelative(1.1f, 0.0f, 1.99f, -0.9f, 1.99f, -2.0f)
                lineToRelative(-0.01f, -16.0f)
                close()
                moveTo(9.0f, 19.0f)
                lineTo(7.0f, 19.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(17.0f, 19.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(9.0f, 15.0f)
                lineTo(7.0f, 15.0f)
                verticalLineToRelative(-4.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(4.0f)
                close()
                moveTo(13.0f, 19.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(-4.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(4.0f)
                close()
                moveTo(13.0f, 13.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(17.0f, 15.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(-4.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(4.0f)
                close()
            }
        }
    }

    val SquareFoot: ImageVector by lazy {
        materialIcon("SquareFoot") {
            materialPath {
                moveTo(17.66f, 17.66f)
                lineToRelative(-1.06f, 1.06f)
                lineToRelative(-0.71f, -0.71f)
                lineToRelative(1.06f, -1.06f)
                lineToRelative(-1.94f, -1.94f)
                lineToRelative(-1.06f, 1.06f)
                lineToRelative(-0.71f, -0.71f)
                lineToRelative(1.06f, -1.06f)
                lineToRelative(-1.94f, -1.94f)
                lineToRelative(-1.06f, 1.06f)
                lineToRelative(-0.71f, -0.71f)
                lineToRelative(1.06f, -1.06f)
                lineTo(9.7f, 9.7f)
                lineToRelative(-1.06f, 1.06f)
                lineToRelative(-0.71f, -0.71f)
                lineToRelative(1.06f, -1.06f)
                lineTo(7.05f, 7.05f)
                lineTo(5.99f, 8.11f)
                lineTo(5.28f, 7.4f)
                lineToRelative(1.06f, -1.06f)
                lineTo(4.0f, 4.0f)
                verticalLineToRelative(14.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(14.0f)
                lineTo(17.66f, 17.66f)
                close()
                moveTo(7.0f, 17.0f)
                verticalLineToRelative(-5.76f)
                lineTo(12.76f, 17.0f)
                horizontalLineTo(7.0f)
                close()
            }
        }
    }

    val Traffic: ImageVector by lazy {
        materialIcon("Traffic") {
            materialPath {
                moveTo(20.0f, 10.0f)
                horizontalLineToRelative(-3.0f)
                lineTo(17.0f, 8.86f)
                curveToRelative(1.72f, -0.45f, 3.0f, -2.0f, 3.0f, -3.86f)
                horizontalLineToRelative(-3.0f)
                lineTo(17.0f, 4.0f)
                curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                lineTo(8.0f, 3.0f)
                curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                verticalLineToRelative(1.0f)
                lineTo(4.0f, 5.0f)
                curveToRelative(0.0f, 1.86f, 1.28f, 3.41f, 3.0f, 3.86f)
                lineTo(7.0f, 10.0f)
                lineTo(4.0f, 10.0f)
                curveToRelative(0.0f, 1.86f, 1.28f, 3.41f, 3.0f, 3.86f)
                lineTo(7.0f, 15.0f)
                lineTo(4.0f, 15.0f)
                curveToRelative(0.0f, 1.86f, 1.28f, 3.41f, 3.0f, 3.86f)
                lineTo(7.0f, 20.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                horizontalLineToRelative(8.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                verticalLineToRelative(-1.14f)
                curveToRelative(1.72f, -0.45f, 3.0f, -2.0f, 3.0f, -3.86f)
                horizontalLineToRelative(-3.0f)
                verticalLineToRelative(-1.14f)
                curveToRelative(1.72f, -0.45f, 3.0f, -2.0f, 3.0f, -3.86f)
                close()
                moveTo(12.0f, 19.0f)
                curveToRelative(-1.11f, 0.0f, -2.0f, -0.9f, -2.0f, -2.0f)
                reflectiveCurveToRelative(0.89f, -2.0f, 2.0f, -2.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, 0.9f, 2.0f, 2.0f)
                reflectiveCurveToRelative(-0.89f, 2.0f, -2.0f, 2.0f)
                close()
                moveTo(12.0f, 14.0f)
                curveToRelative(-1.11f, 0.0f, -2.0f, -0.9f, -2.0f, -2.0f)
                reflectiveCurveToRelative(0.89f, -2.0f, 2.0f, -2.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, 0.9f, 2.0f, 2.0f)
                reflectiveCurveToRelative(-0.89f, 2.0f, -2.0f, 2.0f)
                close()
                moveTo(12.0f, 9.0f)
                curveToRelative(-1.11f, 0.0f, -2.0f, -0.9f, -2.0f, -2.0f)
                curveToRelative(0.0f, -1.11f, 0.89f, -2.0f, 2.0f, -2.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, 0.89f, 2.0f, 2.0f)
                curveToRelative(0.0f, 1.1f, -0.89f, 2.0f, -2.0f, 2.0f)
                close()
            }
        }
    }

    val Tune: ImageVector by lazy {
        materialIcon("Tune") {
            materialPath {
                moveTo(3.0f, 17.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(6.0f)
                verticalLineToRelative(-2.0f)
                lineTo(3.0f, 17.0f)
                close()
                moveTo(3.0f, 5.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(10.0f)
                lineTo(13.0f, 5.0f)
                lineTo(3.0f, 5.0f)
                close()
                moveTo(13.0f, 21.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(8.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(-8.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(6.0f)
                horizontalLineToRelative(2.0f)
                close()
                moveTo(7.0f, 9.0f)
                verticalLineToRelative(2.0f)
                lineTo(3.0f, 11.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(2.0f)
                lineTo(9.0f, 9.0f)
                lineTo(7.0f, 9.0f)
                close()
                moveTo(21.0f, 13.0f)
                verticalLineToRelative(-2.0f)
                lineTo(11.0f, 11.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(10.0f)
                close()
                moveTo(15.0f, 9.0f)
                horizontalLineToRelative(2.0f)
                lineTo(17.0f, 7.0f)
                horizontalLineToRelative(4.0f)
                lineTo(21.0f, 5.0f)
                horizontalLineToRelative(-4.0f)
                lineTo(17.0f, 3.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(6.0f)
                close()
            }
        }
    }

    val Visibility: ImageVector by lazy {
        materialIcon("Visibility") {
            materialPath {
                moveTo(12.0f, 4.5f)
                curveTo(7.0f, 4.5f, 2.73f, 7.61f, 1.0f, 12.0f)
                curveToRelative(1.73f, 4.39f, 6.0f, 7.5f, 11.0f, 7.5f)
                reflectiveCurveToRelative(9.27f, -3.11f, 11.0f, -7.5f)
                curveToRelative(-1.73f, -4.39f, -6.0f, -7.5f, -11.0f, -7.5f)
                close()
                moveTo(12.0f, 17.0f)
                curveToRelative(-2.76f, 0.0f, -5.0f, -2.24f, -5.0f, -5.0f)
                reflectiveCurveToRelative(2.24f, -5.0f, 5.0f, -5.0f)
                reflectiveCurveToRelative(5.0f, 2.24f, 5.0f, 5.0f)
                reflectiveCurveToRelative(-2.24f, 5.0f, -5.0f, 5.0f)
                close()
                moveTo(12.0f, 9.0f)
                curveToRelative(-1.66f, 0.0f, -3.0f, 1.34f, -3.0f, 3.0f)
                reflectiveCurveToRelative(1.34f, 3.0f, 3.0f, 3.0f)
                reflectiveCurveToRelative(3.0f, -1.34f, 3.0f, -3.0f)
                reflectiveCurveToRelative(-1.34f, -3.0f, -3.0f, -3.0f)
                close()
            }
        }
    }

    val VisibilityOff: ImageVector by lazy {
        materialIcon("VisibilityOff") {
            materialPath {
                moveTo(12.0f, 7.0f)
                curveToRelative(2.76f, 0.0f, 5.0f, 2.24f, 5.0f, 5.0f)
                curveToRelative(0.0f, 0.65f, -0.13f, 1.26f, -0.36f, 1.83f)
                lineToRelative(2.92f, 2.92f)
                curveToRelative(1.51f, -1.26f, 2.7f, -2.89f, 3.43f, -4.75f)
                curveToRelative(-1.73f, -4.39f, -6.0f, -7.5f, -11.0f, -7.5f)
                curveToRelative(-1.4f, 0.0f, -2.74f, 0.25f, -3.98f, 0.7f)
                lineToRelative(2.16f, 2.16f)
                curveTo(10.74f, 7.13f, 11.35f, 7.0f, 12.0f, 7.0f)
                close()
                moveTo(2.0f, 4.27f)
                lineToRelative(2.28f, 2.28f)
                lineToRelative(0.46f, 0.46f)
                curveTo(3.08f, 8.3f, 1.78f, 10.02f, 1.0f, 12.0f)
                curveToRelative(1.73f, 4.39f, 6.0f, 7.5f, 11.0f, 7.5f)
                curveToRelative(1.55f, 0.0f, 3.03f, -0.3f, 4.38f, -0.84f)
                lineToRelative(0.42f, 0.42f)
                lineTo(19.73f, 22.0f)
                lineTo(21.0f, 20.73f)
                lineTo(3.27f, 3.0f)
                lineTo(2.0f, 4.27f)
                close()
                moveTo(7.53f, 9.8f)
                lineToRelative(1.55f, 1.55f)
                curveToRelative(-0.05f, 0.21f, -0.08f, 0.43f, -0.08f, 0.65f)
                curveToRelative(0.0f, 1.66f, 1.34f, 3.0f, 3.0f, 3.0f)
                curveToRelative(0.22f, 0.0f, 0.44f, -0.03f, 0.65f, -0.08f)
                lineToRelative(1.55f, 1.55f)
                curveToRelative(-0.67f, 0.33f, -1.41f, 0.53f, -2.2f, 0.53f)
                curveToRelative(-2.76f, 0.0f, -5.0f, -2.24f, -5.0f, -5.0f)
                curveToRelative(0.0f, -0.79f, 0.2f, -1.53f, 0.53f, -2.2f)
                close()
                moveTo(11.84f, 9.02f)
                lineToRelative(3.15f, 3.15f)
                lineToRelative(0.02f, -0.16f)
                curveToRelative(0.0f, -1.66f, -1.34f, -3.0f, -3.0f, -3.0f)
                lineToRelative(-0.17f, 0.01f)
                close()
            }
        }
    }

    val Wifi: ImageVector by lazy {
        materialIcon("Wifi") {
            materialPath {
                moveTo(1.0f, 9.0f)
                lineToRelative(2.0f, 2.0f)
                curveToRelative(4.97f, -4.97f, 13.03f, -4.97f, 18.0f, 0.0f)
                lineToRelative(2.0f, -2.0f)
                curveTo(16.93f, 2.93f, 7.08f, 2.93f, 1.0f, 9.0f)
                close()
                moveTo(9.0f, 17.0f)
                lineToRelative(3.0f, 3.0f)
                lineToRelative(3.0f, -3.0f)
                curveToRelative(-1.65f, -1.66f, -4.34f, -1.66f, -6.0f, 0.0f)
                close()
                moveTo(5.0f, 13.0f)
                lineToRelative(2.0f, 2.0f)
                curveToRelative(2.76f, -2.76f, 7.24f, -2.76f, 10.0f, 0.0f)
                lineToRelative(2.0f, -2.0f)
                curveTo(15.14f, 9.14f, 8.87f, 9.14f, 5.0f, 13.0f)
                close()
            }
        }
    }

    val Logout: ImageVector by lazy {
        materialIcon("Logout", autoMirror = true) {
            materialPath {
                moveTo(17.0f, 7.0f)
                lineToRelative(-1.41f, 1.41f)
                lineTo(18.17f, 11.0f)
                horizontalLineTo(8.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(10.17f)
                lineToRelative(-2.58f, 2.58f)
                lineTo(17.0f, 17.0f)
                lineToRelative(5.0f, -5.0f)
                close()
                moveTo(4.0f, 5.0f)
                horizontalLineToRelative(8.0f)
                verticalLineTo(3.0f)
                horizontalLineTo(4.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(14.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(8.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineTo(4.0f)
                verticalLineTo(5.0f)
                close()
            }
        }
    }

    val MapOutlined: ImageVector by lazy {
        materialIcon("MapOutlined") {
            materialPath {
                moveTo(20.5f, 3.0f)
                lineToRelative(-0.16f, 0.03f)
                lineTo(15.0f, 5.1f)
                lineTo(9.0f, 3.0f)
                lineTo(3.36f, 4.9f)
                curveToRelative(-0.21f, 0.07f, -0.36f, 0.25f, -0.36f, 0.48f)
                lineTo(3.0f, 20.5f)
                curveToRelative(0.0f, 0.28f, 0.22f, 0.5f, 0.5f, 0.5f)
                lineToRelative(0.16f, -0.03f)
                lineTo(9.0f, 18.9f)
                lineToRelative(6.0f, 2.1f)
                lineToRelative(5.64f, -1.9f)
                curveToRelative(0.21f, -0.07f, 0.36f, -0.25f, 0.36f, -0.48f)
                lineTo(21.0f, 3.5f)
                curveToRelative(0.0f, -0.28f, -0.22f, -0.5f, -0.5f, -0.5f)
                close()
                moveTo(10.0f, 5.47f)
                lineToRelative(4.0f, 1.4f)
                verticalLineToRelative(11.66f)
                lineToRelative(-4.0f, -1.4f)
                lineTo(10.0f, 5.47f)
                close()
                moveTo(5.0f, 6.46f)
                lineToRelative(3.0f, -1.01f)
                verticalLineToRelative(11.7f)
                lineToRelative(-3.0f, 1.16f)
                lineTo(5.0f, 6.46f)
                close()
                moveTo(19.0f, 17.54f)
                lineToRelative(-3.0f, 1.01f)
                lineTo(16.0f, 6.86f)
                lineToRelative(3.0f, -1.16f)
                verticalLineToRelative(11.84f)
                close()
            }
        }
    }

    val GridViewOutlined: ImageVector by lazy {
        materialIcon("GridViewOutlined") {
            materialPath {
                moveTo(3.0f, 3.0f)
                verticalLineToRelative(8.0f)
                horizontalLineToRelative(8.0f)
                verticalLineTo(3.0f)
                horizontalLineTo(3.0f)
                close()
                moveTo(9.0f, 9.0f)
                horizontalLineTo(5.0f)
                verticalLineTo(5.0f)
                horizontalLineToRelative(4.0f)
                verticalLineTo(9.0f)
                close()
                moveTo(3.0f, 13.0f)
                verticalLineToRelative(8.0f)
                horizontalLineToRelative(8.0f)
                verticalLineToRelative(-8.0f)
                horizontalLineTo(3.0f)
                close()
                moveTo(9.0f, 19.0f)
                horizontalLineTo(5.0f)
                verticalLineToRelative(-4.0f)
                horizontalLineToRelative(4.0f)
                verticalLineTo(19.0f)
                close()
                moveTo(13.0f, 3.0f)
                verticalLineToRelative(8.0f)
                horizontalLineToRelative(8.0f)
                verticalLineTo(3.0f)
                horizontalLineTo(13.0f)
                close()
                moveTo(19.0f, 9.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineTo(5.0f)
                horizontalLineToRelative(4.0f)
                verticalLineTo(9.0f)
                close()
                moveTo(13.0f, 13.0f)
                verticalLineToRelative(8.0f)
                horizontalLineToRelative(8.0f)
                verticalLineToRelative(-8.0f)
                horizontalLineTo(13.0f)
                close()
                moveTo(19.0f, 19.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(-4.0f)
                horizontalLineToRelative(4.0f)
                verticalLineTo(19.0f)
                close()
            }
        }
    }

}
