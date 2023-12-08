package com.example.canvasexample

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.min

@Composable
fun pixelToDp(px: Double): Dp {
    val density = LocalDensity.current.density.toDouble()
    return (px / density).dp
}

//@Deprecated
//fun nodesScaling() {
//
//    val screenWidth = Resources.getSystem().displayMetrics.widthPixels
//    val screenHeight = Resources.getSystem().displayMetrics.heightPixels
//
//    val borderSize = 180
//
//    var minX = Double.MAX_VALUE
//    var maxX = Double.MIN_VALUE
//    var minY = Double.MAX_VALUE
//    var maxY = Double.MIN_VALUE
//    _nodes.forEach { node ->
//        if (minX > node.x) minX = node.x
//        if (maxX < node.x) maxX = node.x
//        if (minY > node.y) minY = node.y
//        if (maxY < node.y) maxY = node.y
//    }
//
//    var lengthX = abs(maxX - minX)
//    var lengthY = abs(maxY - minY)
//
//    var scalingX = (screenWidth - borderSize * 2) / lengthX
//    var scalingY = (screenHeight - borderSize * 2) / lengthY
//
//    val scaling = min(scalingX, scalingY)
//
//    _nodes.forEachIndexed { index, node ->
//        var nodeTemp = node.copy()
//        nodeTemp.x = (-1 * minX + node.x) * scaling + 180
//        nodeTemp.y = (-1 * minY + node.y) * scaling + 180
//        _nodesTemper[index] = nodeTemp
//    }
//}