package com.example.canvasexample

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.canvasexample.ui.theme.CanvasExampleTheme
import kotlin.math.max
import kotlin.math.min
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.canvasexample.db.Node

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val graphViewModel = GraphViewModel(application)
        super.onCreate(savedInstanceState)
        setContent {
            CanvasExampleTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = {
                                    graphViewModel.addNode()
                                }
                            ) {
                                Icon(imageVector = Icons.Filled.AddCircle, contentDescription = null)
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(it)
                                .fillMaxSize()
                                .background(color = MaterialTheme.colorScheme.secondary)
                        ) {
                            Graph(viewModel = graphViewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Graph(
    viewModel: GraphViewModel = viewModel(),
) {

    val screenWidthCenter = Resources.getSystem().displayMetrics.widthPixels / 2
    val screenHeightCenter = Resources.getSystem().displayMetrics.heightPixels / 2

    var index by remember { mutableStateOf(0) }
    var test by remember { mutableStateOf(32f) }
    var test2 by remember { mutableStateOf(5f) }
    var test3 by remember { mutableStateOf(1f) }
    var scale by remember { mutableStateOf(1f) }
    var scaleAlpha by remember { mutableStateOf(16f) }
    var scaleBeta by remember { mutableStateOf(512f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        Log.e(
            "TAG",
            "Graph: scale - $scale"
        )
        if (scale < 0.3f) {
            if (zoomChange > 1f) {
                scale *= zoomChange
                test /= zoomChange
                test2 /= zoomChange
                scaleAlpha /= zoomChange
                scaleBeta /= zoomChange
            }
        } else if (scale > 3f) {
            if (zoomChange < 1f) {
                scale *= zoomChange
                test /= zoomChange
                test2 /= zoomChange
                scaleAlpha /= zoomChange
                scaleBeta /= zoomChange
            }
        } else {
            scale *= zoomChange
            test /= zoomChange
            test2 /= zoomChange
            scaleAlpha /= zoomChange
            scaleBeta /= zoomChange
        }

        offset += offsetChange * scale
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .transformable(state = state)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
//                .graphicsLayer(
//                    scaleX = scale,
//                    scaleY = scale,
//                    translationX = offset.x,
//                    translationY = offset.y
//                )
//                .transformable(state = state)
                .fillMaxSize()
//                .offset {
//                    IntOffset(viewModel.dx.observeAsState().value!!, screenHeightCenter)
//                }
                .offset(
                    x = pixelToDp(viewModel.dx.observeAsState(0).value.toDouble()),
                    y = pixelToDp(viewModel.dy.observeAsState(0).value.toDouble())
                )
        ) {
            viewModel.edges.forEach { edge ->
                drawEdge(
                    start = viewModel.nodes[edge.node1],
                    end = viewModel.nodes[edge.node2],
                    scale = test2,
                )
            }

            viewModel.nodes.forEachIndexed { index, node ->
                drawNodeV2(
                    index = index,
                    node = node,
                    scale = scale,
                    scaleAlpha = scaleAlpha,
                    scaleBeta = scaleBeta,
                    viewModel = viewModel
                )
            }

        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun drawNodeV2(
    index: Int,
    node: Node,
    scale: Float,
    scaleAlpha: Float,
    scaleBeta: Float,
    viewModel: GraphViewModel = viewModel(),
) {
    val screenWidthCenter = Resources.getSystem().displayMetrics.widthPixels / 2
    val screenHeightCenter = Resources.getSystem().displayMetrics.heightPixels / 2

    var x2dp = pixelToDp(node.x)
    var y2dp = pixelToDp(node.y)
    var alpha2dp = pixelToDp(scaleAlpha.toDouble())
    var beta2dp = pixelToDp(scaleBeta.toDouble())

    var expanded by remember { mutableStateOf(false) }
    var readable by remember { mutableFloatStateOf(scale) }

    val outBoundX by animateDpAsState(
        targetValue = if (!expanded) x2dp - alpha2dp else x2dp - beta2dp,
        label = ""
    )
    val outBoundY by animateDpAsState(
        targetValue = if (!expanded) y2dp - alpha2dp else y2dp - beta2dp,
        label = ""
    )
    val inBound by animateDpAsState(
        targetValue = if (!expanded) alpha2dp * 0.5f else beta2dp * 0.5f,
        label = ""
    )

    var coordinates by remember { mutableStateOf(Offset(0f, 0f)) }

    var editMode by remember { mutableStateOf(false) }
    val editModeBackgroundColor by animateColorAsState(
        targetValue = if(editMode) MaterialTheme.colorScheme.tertiary else Color.Transparent,
        label = ""
    )

    var offset by remember { mutableStateOf(Offset.Zero) }
    var transformableState = rememberTransformableState { _, offsetChange, _ ->
        offset += offsetChange * scale
    }

    Box(
        modifier = Modifier
            .offset(x = outBoundX, y = outBoundY)
            .size(if (!expanded) alpha2dp * 2 else beta2dp * 2)
            .combinedClickable(
                onClick = {
                    Log.e("TAG", "drawNodeV2: node clicked!")
                    expanded = !expanded
                    if (expanded) {
                        viewModel.operateMainV2(index)
                        viewModel.move(coordinates)
                    } else {
                        viewModel.operateMainV3(index)
                        viewModel.move(Offset(screenWidthCenter * 1f, screenHeightCenter * 1f))
                    }
                },
                onLongClick = {
                    // todo: enter editMode
                    editMode = !editMode
                }
            )
            .animateContentSize()
            .clip(CircleShape)
            .onGloballyPositioned { layoutCoordinates ->
                coordinates = layoutCoordinates.positionInRoot()
                if (expanded) {
                    Log.e("TAG", "drawNodeV2:coordinates $coordinates")
                }
            }
            .transformable(
                enabled = editMode,
                state = transformableState
            )
            .graphicsLayer(
                translationX = offset.x,
                translationY = offset.y
            )
            .drawBehind {
                drawCircle(
                    color = editModeBackgroundColor,
                    alpha = 0.5f
                )
            }
    ) {
        Spacer(
            modifier = Modifier
                .offset(x = inBound, y = inBound)
                .size(if (!expanded) alpha2dp else beta2dp)
                .clip(CircleShape)
                .animateContentSize()
                .background(color = MaterialTheme.colorScheme.onSecondary)

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