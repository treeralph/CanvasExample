package com.example.canvasexample.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.canvasexample.NODE_RADIUS
import com.example.canvasexample.R
import com.example.canvasexample.STROKE_ALPHA
import com.example.canvasexample.STROKE_BOX_SIZE_THRESHOLD
import com.example.canvasexample.STROKE_WIDTH
import com.example.canvasexample.db.Node
import com.example.canvasexample.pixelToDp
import com.example.canvasexample.ui.theme.Pink40
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotionScaffoldInV2(
    modifier: Modifier = Modifier,
    onScale: (Float) -> Unit,
    onReadMode: Boolean,
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
        scale *= zoomChange
        offset += offsetChange
        onScale(scale)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    Row {
                        FilledIconButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Adjust,
                                contentDescription = ""
                            )
                        }
                        FilledIconButton(
                            onClick = {

                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)

                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Folder,
                                contentDescription = ""
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { },
                        enabled = !onReadMode,
                        colors = IconButtonDefaults.outlinedIconButtonColors(
                            contentColor = Color.Black,
                            disabledContentColor = Color(0x54757575)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MenuBook,
                            contentDescription = "",
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
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
fun drawNodeSelectedInV2(
    node: Node,
    scale: Float = 1f,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onNodeMoved: (Offset) -> Unit = { _ -> },
    onClickListener: (Node) -> Unit,
) {
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (node.x - NODE_RADIUS).toInt(),
                    y = (node.y - NODE_RADIUS).toInt()
                )
            }
            .size(pixelToDp(px = NODE_RADIUS * 2))
            .clickable { onClickListener(node) }
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
                .offset { IntOffset((NODE_RADIUS / 2).toInt(), (NODE_RADIUS / 2).toInt()) }
                .size(pixelToDp(px = NODE_RADIUS))
                .clip(CircleShape)
                .background(color = MaterialTheme.colorScheme.onSecondary),
        )

        if (node.imgUri.isNotEmpty()) {
            AsyncImage(
                modifier = Modifier
                    .fillMaxSize()
                    .border(BorderStroke(1.dp, Color.White)),
                model = node.imgUri,
                contentScale = ContentScale.Crop,
                contentDescription = "",
                placeholder = painterResource(id = R.drawable.ic_launcher_background)
            )

            Text(
                modifier = Modifier
                    .offset { IntOffset(0, (NODE_RADIUS * 2).toInt() + 8) }
                    .size(width = pixelToDp(px = NODE_RADIUS * 2), height = 8.dp),
                text = node.content,
                color = Color.White,
                maxLines = 1,
                fontSize = 6.sp,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.primary)
                    .border(BorderStroke(1.dp, Color.White)),
            ) {
                Text(
                    modifier = Modifier.fillMaxSize(),
                    text = node.content,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun drawEdgeSelectedInV2(
    start: Node,
    end: Node,
) {
    val minX = min(start.x, end.x)
    val maxX = max(start.x, end.x)
    val minY = min(start.y, end.y)
    val maxY = max(start.y, end.y)

    val distanceX = maxX - minX
    val distanceY = maxY - minY
    Box(
        modifier = Modifier
            .offset(
                x = pixelToDp(minX),
                y = pixelToDp(minY)
            )
            .size(
                width = pixelToDp(
                    if (distanceX > STROKE_BOX_SIZE_THRESHOLD) distanceX
                    else STROKE_BOX_SIZE_THRESHOLD
                ),
                height = pixelToDp(
                    if (distanceY > STROKE_BOX_SIZE_THRESHOLD) distanceY
                    else STROKE_BOX_SIZE_THRESHOLD
                )
            )
    ) {
        Spacer(
            modifier = Modifier
                .drawBehind {
                    drawLine(
                        color = Color.White,
                        start = Offset(
                            (start.x - minX).toFloat(),
                            (start.y - minY).toFloat()
                        ),
                        end = Offset(
                            (end.x - minX).toFloat(),
                            (end.y - minY).toFloat()
                        ),
                        alpha = STROKE_ALPHA,
                        strokeWidth = STROKE_WIDTH
                    )
                }
                .fillMaxSize()
        )
    }
}

@Composable
fun drawNotificationNodeInV2(
    node: Node,
) {
    var x2dp = pixelToDp(node.x)
    var y2dp = pixelToDp(node.y)

    var scale2dp = pixelToDp(NODE_RADIUS * 2) + 128.dp

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

