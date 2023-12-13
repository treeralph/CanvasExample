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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.canvasexample.ui.theme.CanvasExampleTheme
import kotlin.math.max
import kotlin.math.min
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.canvasexample.composable.MotionScaffold
import com.example.canvasexample.composable.drawEdge
import com.example.canvasexample.composable.drawNode
import com.example.canvasexample.composable.drawNotificationNode
import com.example.canvasexample.db.Node
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var graphViewModel: GraphViewModel

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        graphViewModel = GraphViewModel(application)
        super.onCreate(savedInstanceState)
        setContent {
            CanvasExampleTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Graph(viewModel = graphViewModel)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        graphViewModel.saveData()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Graph(
    viewModel: GraphViewModel,
) {
    var openAddDialog by remember { mutableStateOf(false) }
    var linkText by remember { mutableStateOf("") }

    MotionScaffold(
        containerColor = MaterialTheme.colorScheme.primary,
        snackbarHost = { SnackbarHost(hostState = viewModel.snackBarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { openAddDialog = true }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            }
        }
    ) { scale ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        x = Resources.getSystem().displayMetrics.widthPixels / 2,
                        y = Resources.getSystem().displayMetrics.heightPixels / 2
                    )
                }
        ) {
            viewModel.nodes.forEachIndexed { index, node ->
                drawNode(
                    node = node,
                    scale = scale,
                    scaleAlpha = scale * 16f,
                    scaleBeta = scale * 512f,
                    nodeClickListener = { expanded ->
                        if (expanded) viewModel.operateByZoomIn(index)
                        else viewModel.operateByZoomOut(index)
                    },
                    onDragEnd = { x, y ->
//                        viewModel.updateNode(id, x, y)
//                        viewModel.addEdge(id)
                    },
                    onNodeMoved = { x, y ->
                        viewModel.findCollisionNode(
                            selfId = index,
                            selfX = x,
                            selfY = y
                        )
                    }
                )
            }
            viewModel.edges.forEach { edge ->
                drawEdge(
                    start = viewModel.nodes[viewModel.nodeId2Index[edge.node1]!!],
                    end = viewModel.nodes[viewModel.nodeId2Index[edge.node2]!!],
                    scale = scale
                )
            }
            viewModel.notificationNodes.forEach { node ->
                drawNotificationNode(
                    node = node,
                    scale = scale
                )
            }
        }
        if (openAddDialog) {
            AddNodeDialog(
                text = linkText,
                onTextChange = { linkText = it },
                onDismissClicked = { openAddDialog = false },
                onConfirmClicked = { link ->
                    viewModel.addNode(link)
                    openAddDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNodeDialog(
    text: String = "",
    onTextChange: (String) -> Unit = {},
    onConfirmClicked: (String) -> Unit = {},
    onDismissClicked: () -> Unit = {},
) {
    AlertDialog(
        title = {
            Text(
                text = "Add Node",
                fontSize = 16.sp
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text(text = "LINK") }
            )
        },
        onDismissRequest = onDismissClicked,
        confirmButton = {
            Button(onClick = { onConfirmClicked(text) }) {
                Text(text = "CONFIRM")
            }
        },
        dismissButton = {
            Button(onClick = onDismissClicked) {
                Text(text = "DISMISS")
            }
        }
    )
}
