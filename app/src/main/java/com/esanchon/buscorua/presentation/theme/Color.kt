package com.esanchon.buscorua.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors

// Colores de la Compañía de Tranvías de La Coruña: rojo de fondo y plata del logotipo "TC"
val TranviasRed = Color(0xFFE63B2C)
val TranviasRedDark = Color(0xFF8E2219)
val TranviasSilver = Color(0xFFACADAD)
val TranviasSilverDark = Color(0xFF7D7E7E)

// Gris oscuro con un punto cálido para tarjetas y botones, a juego con el rojo
val SurfaceWarm = Color(0xFF2B2322)

// Avisos en ámbar: en rojo se confundirían con el color de la marca
val WarningAmber = Color(0xFFFFB74D)

internal val wearColorPalette: Colors = Colors(
    primary = TranviasRed,
    primaryVariant = TranviasRedDark,
    secondary = TranviasSilver,
    secondaryVariant = TranviasSilverDark,
    background = Color.Black,
    surface = SurfaceWarm,
    error = WarningAmber,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = TranviasSilver,
    onError = Color.Black
)
