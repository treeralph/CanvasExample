package com.example.canvasexample.composable

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.canvasexample.db.Node
import com.example.canvasexample.pixelToDp
import com.example.canvasexample.ui.theme.Pink40
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotionScaffold(
    modifier: Modifier = Modifier,
    maxZoom: Float = 1.3f,
    minZoom: Float = 0.5f,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable BoxScope.(Float) -> Unit,
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        if (scale < minZoom) {
            if (zoomChange > 1f) {
                scale *= zoomChange
            }
        } else if (scale > maxZoom) {
            if (zoomChange < 1f) {
                scale *= zoomChange
            }
        } else {
            scale *= zoomChange
        }
        offset += offsetChange
        Log.e("TAG", "MotionScaffold: $scale")
    }

    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = contentWindowInsets,
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .transformable(state = state)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                content(scale)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun drawNodeFixVersion2(
    node: Node,
    scaleAlpha: Float,
    scaleBeta: Float,
    nodeClickListener: (Boolean) -> Unit = {} /** node implicit editing */,
    nodeLongClickListener: () -> Unit = {} /** node explicit editing */,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onNodeMoved: (Offset) -> Unit = { _ -> },
) {
    var expanded by remember { mutableStateOf(false) }

    var alpha2dp = pixelToDp(scaleAlpha.toDouble())
    var beta2dp = pixelToDp(scaleBeta.toDouble())

    val nodeOutBoundOffset by animateIntOffsetAsState(
        targetValue = if(!expanded) {
            IntOffset(
                x = node.x.toInt() - scaleAlpha.toInt(),
                y = node.y.toInt() - scaleAlpha.toInt()
            )
        } else {
            IntOffset(
                x = node.x.toInt() - scaleBeta.toInt(),
                y = node.y.toInt() - scaleBeta.toInt()
            )
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "",
    )

    val nodeInBoundOffset by animateIntOffsetAsState(
        targetValue = if(!expanded) {
            IntOffset(
                x = (scaleAlpha * 0.5).toInt(),
                y = (scaleAlpha * 0.5).toInt()
            )
        } else {
            IntOffset(
                x = (scaleBeta * 0.5).toInt(),
                y = (scaleBeta * 0.5).toInt()
            )
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "",
    )

    Box(
        modifier = Modifier
            .offset { nodeOutBoundOffset }
            .size(if (!expanded) alpha2dp * 2 else beta2dp * 2)
            .combinedClickable(
                onClick = {
                    expanded = !expanded
                    nodeClickListener(expanded)
                },
                onLongClick = { nodeLongClickListener() }
            )
            .animateContentSize()
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                ) { change, dragAmount ->
                    change.consume()
                    onNodeMoved(dragAmount)
                }
            }
    ) {
        Spacer(
            modifier = Modifier
                .offset { nodeInBoundOffset }
                .size(if (!expanded) alpha2dp else beta2dp)
                .clip(CircleShape)
                .animateContentSize()
                .background(color = MaterialTheme.colorScheme.onSecondary),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun drawNodeFixVersion(
    node: Node,
    scaleAlpha: Float,
    scaleBeta: Float,
    nodeClickListener: (Boolean) -> Unit = {} /** node implicit editing */,
    nodeLongClickListener: () -> Unit = {} /** node explicit editing */,
    onDragStart: () -> Unit = {},
    onDragEnd: (Double, Double) -> Unit = { _, _ -> },
    onNodeMoved: (Double, Double) -> Unit = { _, _ -> },
) {
    var x by remember { mutableDoubleStateOf(node.x) }
    var y by remember { mutableDoubleStateOf(node.y) }

    var expanded by remember { mutableStateOf(false) }

    var alpha2dp = pixelToDp(scaleAlpha.toDouble())
    var beta2dp = pixelToDp(scaleBeta.toDouble())

    /**
     *
     * todo: IntOffset의 value를 node.x가 아닌 x로 설정하면 recomposition이 원활이 진행되지 않는 문제가 있다.
     *      이걸 해결해야 노드를 움직이는 animation을 원활하게 진행할 수 있다.
     *
     * */

    val nodeOutBoundOffset by animateIntOffsetAsState(
        targetValue = if(!expanded) {
            IntOffset(
                x = node.x.toInt() - scaleAlpha.toInt(),
                y = node.y.toInt() - scaleAlpha.toInt()
            )
        } else {
            IntOffset(
                x = node.x.toInt() - scaleBeta.toInt(),
                y = node.y.toInt() - scaleBeta.toInt()
            )
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "",
    )

    val nodeInBoundOffset by animateIntOffsetAsState(
        targetValue = if(!expanded) {
            IntOffset(
                x = (scaleAlpha * 0.5).toInt(),
                y = (scaleAlpha * 0.5).toInt()
            )
        } else {
            IntOffset(
                x = (scaleBeta * 0.5).toInt(),
                y = (scaleBeta * 0.5).toInt()
            )
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "",
    )

    Box(
        modifier = Modifier
            .offset { nodeOutBoundOffset }
            .size(if (!expanded) alpha2dp * 2 else beta2dp * 2)
            .combinedClickable(
                onClick = {
                    expanded = !expanded
                    nodeClickListener(expanded)
                },
                onLongClick = { nodeLongClickListener() }
            )
            .animateContentSize()
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        onDragStart()
                    },
                    onDragEnd = {
                        /**
                         * onDragEnd
                         *      if ( node connecting ) nodeConnect()
                         *      else status back
                         * */
                        onDragEnd(x, y)
                        x = node.x
                        y = node.y
                    },
                    onDragCancel = {
                        x = node.x
                        y = node.y
                    }
                ) { change, dragAmount ->
                    change.consume()
                    x += dragAmount.x
                    y += dragAmount.y
                    onNodeMoved(x, y)
                }
            }
    ) {
        Spacer(
            modifier = Modifier
                .offset { nodeInBoundOffset }
                .size(if (!expanded) alpha2dp else beta2dp)
                .clip(CircleShape)
                .animateContentSize()
                .background(color = MaterialTheme.colorScheme.onSecondary),
        )
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun drawNode(
    node: Node,
    scale: Float,
    scaleAlpha: Float,
    scaleBeta: Float,
    nodeClickListener: (Boolean) -> Unit = {} /** node implicit editing */,
    nodeLongClickListener: () -> Unit = {} /** node explicit editing */,
    onDragStart: () -> Unit = {},
    onDragEnd: (Double, Double) -> Unit = { _, _ -> },
    onNodeMoved: (Double, Double) -> Unit = { _, _ -> },
) {
    var x by remember { mutableDoubleStateOf(node.x) }
    var y by remember { mutableDoubleStateOf(node.y) }

    var x2dp = pixelToDp(x)
    var y2dp = pixelToDp(y)

    var alpha2dp = pixelToDp(scaleAlpha.toDouble())
    var beta2dp = pixelToDp(scaleBeta.toDouble())

    var expanded by remember { mutableStateOf(false) }

    val outBoundX by animateDpAsState(
        targetValue = if (!expanded) x2dp - alpha2dp else x2dp - beta2dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = ""
    )
    val outBoundY by animateDpAsState(
        targetValue = if (!expanded) y2dp - alpha2dp else y2dp - beta2dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = ""
    )
    val inBound by animateDpAsState(
        targetValue = if (!expanded) alpha2dp * 0.5f else beta2dp * 0.5f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = ""
    )

    Box(
        modifier = Modifier
            .offset(x = outBoundX, y = outBoundY)
            .size(if (!expanded) alpha2dp * 2 else beta2dp * 2)
            .combinedClickable(
                onClick = {
                    expanded = !expanded
                    nodeClickListener(expanded)
                },
                onLongClick = { nodeLongClickListener() }
            )
            .animateContentSize()
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        onDragStart()
                    },
                    onDragEnd = {
                        /**
                         * onDragEnd
                         *      if ( node connecting ) nodeConnect()
                         *      else status back
                         * */
                        onDragEnd(x, y)
                        x = node.x
                        y = node.y
                    },
                    onDragCancel = {
                        x = node.x
                        y = node.y
                    }
                ) { change, dragAmount ->
                    change.consume()
                    x += dragAmount.x
                    y += dragAmount.y
                    onNodeMoved(x, y)
                }
            }
    ) {
        Spacer(
            modifier = Modifier
                .offset(x = inBound, y = inBound)
                .size(if (!expanded) alpha2dp else beta2dp)
                .clip(CircleShape)
                .animateContentSize()
                .background(color = MaterialTheme.colorScheme.onSecondary),
            )
    }
}

@Composable
fun drawEdge(
    start: Node,
    end: Node,
    scale: Float,
) {
    val thresholdSize = 6.0

    var minX = min(start.x, end.x)
    var maxX = max(start.x, end.x)
    var minY = min(start.y, end.y)
    var maxY = max(start.y, end.y)

    var distanceX = maxX - minX
    var distanceY = maxY - minY

    val targetX by animateDpAsState(targetValue = pixelToDp(minX), label = "")
    val targetY by animateDpAsState(targetValue = pixelToDp(minY), label = "")

    val targetWidth by animateDpAsState(
        targetValue = pixelToDp(if (distanceX > thresholdSize) distanceX else thresholdSize),
        label = ""
    )
    val targetHeight by animateDpAsState(
        targetValue = pixelToDp(if (distanceY > thresholdSize) distanceY else thresholdSize),
        label = ""
    )
    val targetStart by animateOffsetAsState(
        targetValue = Offset(
            (start.x - minX).toFloat(),
            (start.y - minY).toFloat()
        ), label = ""
    )
    val targetEnd by animateOffsetAsState(
        targetValue = Offset(
            (end.x - minX).toFloat(),
            (end.y - minY).toFloat()
        ), label = ""
    )

    Box(
        modifier = Modifier
            .offset(
                x = targetX,
                y = targetY
            )
            .size(
                width = targetWidth,
                height = targetHeight
            )
    ) {
        Spacer(
            modifier = Modifier
                .drawBehind {
                    drawLine(
                        color = Color.White,
                        start = targetStart,
                        end = targetEnd,
                        alpha = 0.3f,
                        strokeWidth = scale
                    )
                }
                .fillMaxSize()
        )
    }
}

@Composable
fun drawNotificationNode(
    node: Node,
    scale: Float,
) {
    var x2dp = pixelToDp(node.x)
    var y2dp = pixelToDp(node.y)

    var scale2dp = pixelToDp(scale.toDouble()) + 12.dp

    Box(
        modifier = Modifier
            .offset(x = x2dp - scale2dp, y = y2dp - scale2dp)
            .size(scale2dp * 2)
            .alpha(0.3f)
            .drawBehind {
                drawCircle(color = Pink40)
            }
    )
}