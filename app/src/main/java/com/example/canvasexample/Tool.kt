package com.example.canvasexample

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
@Composable
fun pixelToDp(px: Double): Dp {
    val density = LocalDensity.current.density.toDouble()
    return (px / density).dp
}

fun isYoutubeUrl(link: String): Boolean {
    val pattern = "^(http(s)?:\\/\\/)?((w){3}.)?youtu(be|.be)?(\\.com)?\\/.+"
    return !link.isEmpty() && link.matches(pattern.toRegex())
}
fun openYoutubeIntent(youtubeURI: String): Intent {
    return Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(youtubeURI)
        setPackage("com.google.android.youtube")
    }
}
fun openLinkIntent(link: String): Intent {
    Log.e("TAG", "")
    return Intent(Intent.ACTION_VIEW, Uri.parse(link))
}

fun isNaver(link: String): Boolean = link.startsWith("https://naver.me/")


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