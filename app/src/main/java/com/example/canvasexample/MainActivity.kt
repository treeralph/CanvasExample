package com.example.canvasexample

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.canvasexample.ui.theme.CanvasExampleTheme
import com.example.canvasexample.composable.MotionScaffoldInV2
import com.example.canvasexample.composable.DrawEdgeSelectedInV2
import com.example.canvasexample.composable.DrawEdgeSelectedInV3
import com.example.canvasexample.composable.DrawNodeSelectedInV2
import com.example.canvasexample.composable.DrawNodeSelectedInV3
import com.example.canvasexample.composable.drawNotificationNode
import com.example.canvasexample.db.Node
import com.example.canvasexample.ui.theme.Typography

class MainActivity : ComponentActivity() {
    private lateinit var graphViewModel: GraphViewModelV2
    private var currentUri: MutableState<String> = mutableStateOf("")
    private val imageLoadLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uriNullable ->
            Log.e("TAG", "uriNullable: $uriNullable")
            uriNullable?.let { uri ->
                grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, flag)
                currentUri.value = uri.toString()
            }
        }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        graphViewModel = GraphViewModelV2(application)
        super.onCreate(savedInstanceState)
        setContent {
            CanvasExampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GraphLab(
                        viewModel = graphViewModel,
                        move2Link = { link ->
                            startActivity(
                                if (isYoutubeUrl(link)) openYoutubeIntent(link)
                                else openLinkIntent(link)
                            )
                        },
                        currentUri,
                        onEditImageClickListener = {
                            imageLoadLauncher.launch("image/*")
                        }
                    )
//                    GraphLabV3(
//                        viewModel = graphViewModel,
//                        move2Link = { link ->
//                            startActivity(
//                                if (isYoutubeUrl(link)) openYoutubeIntent(link)
//                                else openLinkIntent(link)
//                            )
//                        },
//                        currentUri,
//                        onEditImageClickListener = {
//                            imageLoadLauncher.launch("image/*")
//                        }
//                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        graphViewModel.draw()
    }
    override fun onStop() {
        super.onStop()
        graphViewModel.saveData()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphLab(
    viewModel: GraphViewModelV2,
    move2Link: (String) -> Unit,
    currentUri: MutableState<String>,
    onEditImageClickListener: () -> Unit,
) {
    var openAddDialog by remember { mutableStateOf(false) }
    var openNodeDetailDialog by remember { mutableStateOf(false) }
    var openAlertDialog by remember { mutableStateOf(false) }
    var openImageEditDialog by remember { mutableStateOf(false) }
    var openTitleEditDialog by remember { mutableStateOf(false) }
    var openDescriptionEditDialog by remember { mutableStateOf(false) }
    var openFolderSelectDialog by remember { mutableStateOf(false) }
    var openAddFolderDialog by remember { mutableStateOf(false) }
    var currentNode by remember { mutableStateOf(Node()) }
    var linkText by remember { mutableStateOf("") }

    MotionScaffoldInV2(
        containerColor = MaterialTheme.colorScheme.primary,
        // snackbarHost = { SnackbarHost(hostState = viewModel.snackBarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { openAddDialog = true }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            }
        },
        onScale = { scale ->
            if (scale > MODE_SWITCHING_SCALE_THRESHOLD) viewModel.opDisable()
            else viewModel.opAble()

        },
        onReadMode = viewModel.able.observeAsState(initial = true).value,
        onFolderButtonClickListener = { openFolderSelectDialog = true }
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
            viewModel.edges.forEach { edge ->
                key(edge.id * -1) {
                    DrawEdgeSelectedInV2(
                        start = viewModel.nodes[viewModel.nodeId2Index[edge.node1]!!],
                        end = viewModel.nodes[viewModel.nodeId2Index[edge.node2]!!],
                    )
                }
            }

            viewModel.nodes.forEachIndexed { index, node ->
                key(node.id) {
                    DrawNodeSelectedInV2(
                        node = node,
                        scale = scale,
                        dragAble = viewModel.able.observeAsState(initial = true).value,
                        onDragStart = {
                            viewModel.onNodeDragStart(node)
                        },
                        onDragEnd = {
                            viewModel.onNodeDragEnd(index, node)
                        },
                        onNodeMoved = { offset ->
                            viewModel.onNodeMoved(index, offset)
                        }
                    ) { node ->
                        Log.e("TAG", "GraphLab: onClick: $node")
                        currentNode = node
                        openNodeDetailDialog = true
                    }
                }
            }

            viewModel.notificationNodes.forEach { node ->
                drawNotificationNode(
                    node = node,
                    scale = scale
                )
            }

        }

        if (openAlertDialog) {
            /** Delete current Node */
            AlertDialog(
                onDismissRequest = { openAlertDialog = false },
                icon = { Icon(imageVector = Icons.Outlined.CheckCircle, contentDescription = "") },
                title = {
                    Text(
                        text = "Delete \"${currentNode.content}\"?",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                dismissButton = {
                    TextButton(onClick = { openAlertDialog = false }) {
                        Text(text = "DISMISS")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteNode(currentNode)
                            openAlertDialog = false
                            openNodeDetailDialog = false
                        }
                    ) {
                        Text(text = "CONFIRM")
                    }
                },
            )
        }

        if (openImageEditDialog) {

            var target by remember { currentUri }

            AlertDialog(
                onDismissRequest = { openImageEditDialog = false },
                icon = { Icon(imageVector = Icons.Outlined.Settings, contentDescription = "") },
                title = {
                    Text(
                        text = "Edit Image",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    AsyncImage(
                        model = target,
                        contentDescription = "",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditImageClickListener() }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { openImageEditDialog = false }) {
                        Text(text = "DISMISS")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            currentNode.imgUri = target
                            viewModel.editNode(currentNode)
                            openImageEditDialog = false
                        }
                    ) {
                        Text(text = "CONFIRM")
                    }
                },
            )
        }

        if (openTitleEditDialog) {

            var target by remember { mutableStateOf(currentNode.content) }

            AlertDialog(
                onDismissRequest = { openTitleEditDialog = false },
                icon = { Icon(imageVector = Icons.Outlined.Settings, contentDescription = "") },
                title = {
                    Text(
                        text = "Edit Title",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { openTitleEditDialog = false }) {
                        Text(text = "DISMISS")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            currentNode.content = target
                            viewModel.editNode(currentNode)
                            openTitleEditDialog = false
                        }
                    ) {
                        Text(text = "CONFIRM")
                    }

                },
            )
        }

        if (openDescriptionEditDialog) {

            var target by remember { mutableStateOf(currentNode.description) }

            AlertDialog(
                onDismissRequest = { openDescriptionEditDialog = false },
                icon = { Icon(imageVector = Icons.Outlined.Settings, contentDescription = "") },
                title = {
                    Text(
                        text = "Edit Description",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { openDescriptionEditDialog = false }) {
                        Text(text = "DISMISS")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            currentNode.description = target
                            viewModel.editNode(currentNode)
                            openDescriptionEditDialog = false
                        }
                    ) {
                        Text(text = "CONFIRM")
                    }
                },
            )
        }

        if (openAddDialog) {
            AddNodeDialog(
                text = linkText,
                onTextChange = { linkText = it },
                onDismissClicked = {
                    openAddDialog = false
                    linkText = ""
                },
                onConfirmClicked = { link ->
                    viewModel.addNode(link)
                    openAddDialog = false
                    linkText = ""
                }
            )
        }

        if (openNodeDetailDialog) {

            var isEditMode by remember { mutableStateOf(false) }

            Dialog(onDismissRequest = { openNodeDetailDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(),
                    shape = RoundedCornerShape(36.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    /** Head */
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        /** Delete Button */
                        IconButton(
                            onClick = { openAlertDialog = true }
                        ) {
                            Icon(
                                modifier = Modifier.padding(8.dp),
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = ""
                            )
                        }

                        /** Edit Button */
                        FilledIconButton(
                            onClick = { isEditMode = !isEditMode },
                            shape = CircleShape,
                            colors = if (isEditMode) {
                                IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            } else {
                                IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                )
                            }
                        ) {
                            Icon(
                                modifier = Modifier.padding(8.dp),
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "",
                            )
                        }


                        /** Move2Link Button */
                        IconButton(
                            onClick = { move2Link(currentNode.linkUrl) }
                        ) {
                            Icon(
                                modifier = Modifier.padding(8.dp),
                                imageVector = Icons.Outlined.Link,
                                contentDescription = ""
                            )
                        }
                        Spacer(modifier = Modifier.size(12.dp))
                    }

                    /** Body */
                    Column(modifier = Modifier.fillMaxWidth()) {
                        /** Image */
                        Box(modifier = Modifier.fillMaxWidth()) {
                            AsyncImage(
                                modifier = Modifier.fillMaxWidth(),
                                model = currentNode.imgUri,
                                contentDescription = "",
                            )
                            if (isEditMode) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            color = MaterialTheme.colorScheme.secondary.copy(
                                                alpha = 0.7f
                                            )
                                        )
                                ) {
                                    IconButton(
                                        modifier = Modifier.align(Alignment.Center),
                                        onClick = { openImageEditDialog = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Settings,
                                            contentDescription = ""
                                        )
                                    }
                                }
                            }
                        }

                        /** Content */
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                modifier = Modifier.padding(8.dp),
                                text = currentNode.content,
                                style = Typography.titleSmall
                            )
                            if (isEditMode) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            color = MaterialTheme.colorScheme.secondary.copy(
                                                alpha = 0.7f
                                            )
                                        )
                                ) {
                                    IconButton(
                                        modifier = Modifier.align(Alignment.Center),
                                        onClick = { openTitleEditDialog = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Settings,
                                            contentDescription = ""
                                        )
                                    }
                                }
                            }
                        }

                        /** Description */
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                modifier = Modifier.padding(
                                    start = 8.dp,
                                    end = 8.dp,
                                    bottom = 32.dp
                                ),
                                text = currentNode.description,
                                style = Typography.bodySmall
                            )
                            if (isEditMode) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            color = MaterialTheme.colorScheme.secondary.copy(
                                                alpha = 0.7f
                                            )
                                        )
                                ) {
                                    IconButton(
                                        modifier = Modifier.align(Alignment.Center),
                                        onClick = { openDescriptionEditDialog = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Settings,
                                            contentDescription = ""
                                        )
                                    }
                                }
                            }
                        }
                    }

                    /** Tail */
                    Row {

                    }
                }
            }
        }

        if (openAddFolderDialog) {

            var target by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { openAddFolderDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.AddCircleOutline,
                        contentDescription = ""
                    )
                },
                title = {
                    Text(
                        text = "Add Folder",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text(text = "Folder Name") }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { openAddFolderDialog = false }) {
                        Text(text = "DISMISS")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.addFolder(target)
                            openAddFolderDialog = false
                        }
                    ) {
                        Text(text = "CONFIRM")
                    }

                },
            )
        }

        if (openFolderSelectDialog) {

            /* todo: change add button, implement delete folder button & edit folder button */

            var isFolderEditMode by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { openFolderSelectDialog = false },
                confirmButton = { },
                icon = { Icon(imageVector = Icons.Outlined.Folder, contentDescription = "") },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f)) { 
                            if(isFolderEditMode) {
                                IconButton(onClick = { openAddFolderDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Outlined.AddCircleOutline,
                                        contentDescription = ""
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Text(text = "Choose Folder")
                        Row(modifier = Modifier.weight(1f)) {
                            Spacer(modifier = Modifier.weight(1f))
                            FilledIconButton(
                                onClick = { isFolderEditMode = !isFolderEditMode },
                                shape = CircleShape,
                                colors = if(!isFolderEditMode) {
                                    IconButtonDefaults.filledIconButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = Color.Black.copy(alpha = 0.5f)
                                    )
                                }else {
                                    IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = ""
                                )
                            }
                        }
                    }
                },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        userScrollEnabled = true
                    ) {
                        items(viewModel.folders) { folder ->
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    viewModel.updateElements(folder)
                                    openFolderSelectDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (viewModel.currentFolderId
                                            .observeAsState(-1).value == folder.id
                                    ) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                            ) {
                                Text(
                                    modifier = Modifier.align(Alignment.CenterVertically),
                                    text = folder.folderName
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphLabV3(
    viewModel: GraphViewModelV3,
    move2Link: (String) -> Unit,
    currentUri: MutableState<String>,
    onEditImageClickListener: () -> Unit,
) {
    var openAddDialog by remember { mutableStateOf(false) }
    var openNodeDetailDialog by remember { mutableStateOf(false) }
    var openAlertDialog by remember { mutableStateOf(false) }
    var openImageEditDialog by remember { mutableStateOf(false) }
    var openTitleEditDialog by remember { mutableStateOf(false) }
    var openDescriptionEditDialog by remember { mutableStateOf(false) }
    var openFolderSelectDialog by remember { mutableStateOf(false) }
    var openAddFolderDialog by remember { mutableStateOf(false) }
    var currentNode by remember { mutableStateOf(Node()) }
    var linkText by remember { mutableStateOf("") }

    MotionScaffoldInV2(
        containerColor = MaterialTheme.colorScheme.primary,
        // snackbarHost = { SnackbarHost(hostState = viewModel.snackBarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { openAddDialog = true }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            }
        },
        onScale = { scale ->
            if (scale > MODE_SWITCHING_SCALE_THRESHOLD) viewModel.opDisable()
            else viewModel.opAble()

        },
        onReadMode = viewModel.able.observeAsState(initial = true).value,
        onFolderButtonClickListener = { openFolderSelectDialog = true }
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
            viewModel.edges.forEach { edgeState ->
                key(edgeState.value.id * -1) {
                    DrawEdgeSelectedInV3(
                        start = { viewModel.nodes[viewModel.nodeId2Index[edgeState.value.node1]!!].value },
                        end = { viewModel.nodes[viewModel.nodeId2Index[edgeState.value.node2]!!].value },
                    )
                }
            }

            viewModel.nodes.forEachIndexed { index, nodeState ->
                key(nodeState.value.id) {
                    DrawNodeSelectedInV3(
                        node = { nodeState.value },
                        dragAble = viewModel.able.observeAsState(initial = true).value,
                        onDragStart = {
                            viewModel.onNodeDragStart(nodeState.value)
                        },
                        onDragEnd = {
                            viewModel.onNodeDragEnd(index, nodeState.value)
                        },
                        onNodeMoved = { offset ->
                            viewModel.onNodeMoved(index, offset)
                        }
                    ) { node ->
                        Log.e("TAG", "GraphLab: onClick: $node")
                        currentNode = node
                        openNodeDetailDialog = true
                    }
                }
            }

            viewModel.notificationNodes.forEach { nodeState ->
                drawNotificationNode(
                    node = nodeState.value,
                    scale = scale
                )
            }

        }

        if (openAlertDialog) {
            /** Delete current Node */
            AlertDialog(
                onDismissRequest = { openAlertDialog = false },
                icon = { Icon(imageVector = Icons.Outlined.CheckCircle, contentDescription = "") },
                title = {
                    Text(
                        text = "Delete \"${currentNode.content}\"?",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                dismissButton = {
                    TextButton(onClick = { openAlertDialog = false }) {
                        Text(text = "DISMISS")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteNode(currentNode)
                            openAlertDialog = false
                            openNodeDetailDialog = false
                        }
                    ) {
                        Text(text = "CONFIRM")
                    }
                },
            )
        }

        if (openImageEditDialog) {

            var target by remember { currentUri }

            AlertDialog(
                onDismissRequest = { openImageEditDialog = false },
                icon = { Icon(imageVector = Icons.Outlined.Settings, contentDescription = "") },
                title = {
                    Text(
                        text = "Edit Image",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    AsyncImage(
                        model = target,
                        contentDescription = "",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditImageClickListener() }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { openImageEditDialog = false }) {
                        Text(text = "DISMISS")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            currentNode.imgUri = target
                            viewModel.editNode(currentNode)
                            openImageEditDialog = false
                        }
                    ) {
                        Text(text = "CONFIRM")
                    }
                },
            )
        }

        if (openTitleEditDialog) {

            var target by remember { mutableStateOf(currentNode.content) }

            AlertDialog(
                onDismissRequest = { openTitleEditDialog = false },
                icon = { Icon(imageVector = Icons.Outlined.Settings, contentDescription = "") },
                title = {
                    Text(
                        text = "Edit Title",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { openTitleEditDialog = false }) {
                        Text(text = "DISMISS")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            currentNode.content = target
                            viewModel.editNode(currentNode)
                            openTitleEditDialog = false
                        }
                    ) {
                        Text(text = "CONFIRM")
                    }

                },
            )
        }

        if (openDescriptionEditDialog) {

            var target by remember { mutableStateOf(currentNode.description) }

            AlertDialog(
                onDismissRequest = { openDescriptionEditDialog = false },
                icon = { Icon(imageVector = Icons.Outlined.Settings, contentDescription = "") },
                title = {
                    Text(
                        text = "Edit Description",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { openDescriptionEditDialog = false }) {
                        Text(text = "DISMISS")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            currentNode.description = target
                            viewModel.editNode(currentNode)
                            openDescriptionEditDialog = false
                        }
                    ) {
                        Text(text = "CONFIRM")
                    }
                },
            )
        }

        if (openAddDialog) {
            AddNodeDialog(
                text = linkText,
                onTextChange = { linkText = it },
                onDismissClicked = {
                    openAddDialog = false
                    linkText = ""
                },
                onConfirmClicked = { link ->
                    viewModel.addNode(link)
                    openAddDialog = false
                    linkText = ""
                }
            )
        }

        if (openNodeDetailDialog) {

            var isEditMode by remember { mutableStateOf(false) }

            Dialog(onDismissRequest = { openNodeDetailDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(),
                    shape = RoundedCornerShape(36.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    /** Head */
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        /** Delete Button */
                        IconButton(
                            onClick = { openAlertDialog = true }
                        ) {
                            Icon(
                                modifier = Modifier.padding(8.dp),
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = ""
                            )
                        }

                        /** Edit Button */
                        FilledIconButton(
                            onClick = { isEditMode = !isEditMode },
                            shape = CircleShape,
                            colors = if (isEditMode) {
                                IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            } else {
                                IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                )
                            }
                        ) {
                            Icon(
                                modifier = Modifier.padding(8.dp),
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "",
                            )
                        }


                        /** Move2Link Button */
                        IconButton(
                            onClick = { move2Link(currentNode.linkUrl) }
                        ) {
                            Icon(
                                modifier = Modifier.padding(8.dp),
                                imageVector = Icons.Outlined.Link,
                                contentDescription = ""
                            )
                        }
                        Spacer(modifier = Modifier.size(12.dp))
                    }

                    /** Body */
                    Column(modifier = Modifier.fillMaxWidth()) {
                        /** Image */
                        Box(modifier = Modifier.fillMaxWidth()) {
                            AsyncImage(
                                modifier = Modifier.fillMaxWidth(),
                                model = currentNode.imgUri,
                                contentDescription = "",
                            )
                            if (isEditMode) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            color = MaterialTheme.colorScheme.secondary.copy(
                                                alpha = 0.7f
                                            )
                                        )
                                ) {
                                    IconButton(
                                        modifier = Modifier.align(Alignment.Center),
                                        onClick = { openImageEditDialog = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Settings,
                                            contentDescription = ""
                                        )
                                    }
                                }
                            }
                        }

                        /** Content */
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                modifier = Modifier.padding(8.dp),
                                text = currentNode.content,
                                style = Typography.titleSmall
                            )
                            if (isEditMode) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            color = MaterialTheme.colorScheme.secondary.copy(
                                                alpha = 0.7f
                                            )
                                        )
                                ) {
                                    IconButton(
                                        modifier = Modifier.align(Alignment.Center),
                                        onClick = { openTitleEditDialog = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Settings,
                                            contentDescription = ""
                                        )
                                    }
                                }
                            }
                        }

                        /** Description */
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                modifier = Modifier.padding(
                                    start = 8.dp,
                                    end = 8.dp,
                                    bottom = 32.dp
                                ),
                                text = currentNode.description,
                                style = Typography.bodySmall
                            )
                            if (isEditMode) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            color = MaterialTheme.colorScheme.secondary.copy(
                                                alpha = 0.7f
                                            )
                                        )
                                ) {
                                    IconButton(
                                        modifier = Modifier.align(Alignment.Center),
                                        onClick = { openDescriptionEditDialog = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Settings,
                                            contentDescription = ""
                                        )
                                    }
                                }
                            }
                        }
                    }

                    /** Tail */
                    Row {

                    }
                }
            }
        }

        if (openAddFolderDialog) {

            var target by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { openAddFolderDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.AddCircleOutline,
                        contentDescription = ""
                    )
                },
                title = {
                    Text(
                        text = "Add Folder",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text(text = "Folder Name") }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { openAddFolderDialog = false }) {
                        Text(text = "DISMISS")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.addFolder(target)
                            openAddFolderDialog = false
                        }
                    ) {
                        Text(text = "CONFIRM")
                    }

                },
            )
        }

        if (openFolderSelectDialog) {

            /* todo: change add button, implement delete folder button & edit folder button */

            var isFolderEditMode by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { openFolderSelectDialog = false },
                confirmButton = { },
                icon = { Icon(imageVector = Icons.Outlined.Folder, contentDescription = "") },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f)) {
                            if(isFolderEditMode) {
                                IconButton(onClick = { openAddFolderDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Outlined.AddCircleOutline,
                                        contentDescription = ""
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Text(text = "Choose Folder")
                        Row(modifier = Modifier.weight(1f)) {
                            Spacer(modifier = Modifier.weight(1f))
                            FilledIconButton(
                                onClick = { isFolderEditMode = !isFolderEditMode },
                                shape = CircleShape,
                                colors = if(!isFolderEditMode) {
                                    IconButtonDefaults.filledIconButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = Color.Black.copy(alpha = 0.5f)
                                    )
                                }else {
                                    IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = ""
                                )
                            }
                        }
                    }
                },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        userScrollEnabled = true
                    ) {
                        items(viewModel.folders) { folder ->
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    viewModel.updateElements(folder)
                                    openFolderSelectDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (viewModel.currentFolderId
                                            .observeAsState(-1).value == folder.id
                                    ) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                            ) {
                                Text(
                                    modifier = Modifier.align(Alignment.CenterVertically),
                                    text = folder.folderName
                                )
                            }
                        }
                    }
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
    onConfirmClicked: (String) -> Unit = { _ -> },
    onDismissClicked: () -> Unit = {},
) {
    AlertDialog(
        icon = {
            Icon(imageVector = Icons.Outlined.AddBox, contentDescription = "")
        },
        title = {
            Text(
                text = "Add Node",
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    label = { Text(text = "Link or Title")}
                )
            }
        },
        onDismissRequest = onDismissClicked,
        confirmButton = {
            TextButton(onClick = { onConfirmClicked(text) }) {
                Text(text = "CONFIRM")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissClicked) {
                Text(text = "DISMISS")
            }
        }
    )
}

@Composable
fun NodeDetailDialog(
    node: Node = testNode,
    onDismissClicked: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissClicked,
        confirmButton = { },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    modifier = Modifier.fillMaxWidth(),
                    model = node.imgUri,
                    contentDescription = ""
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = node.content
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = node.description
                )
            }
        }
    )
}

@Composable
fun NodeDetailDialog2(
    node: Node = testNode,
    onDismissClicked: () -> Unit,
) {
    Dialog(onDismissRequest = onDismissClicked) {

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(36.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            /** Head */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                /** Delete Button */
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(
                        modifier = Modifier.padding(8.dp),
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = ""
                    )
                }

                /** Edit Button */
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(
                        modifier = Modifier.padding(8.dp),
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = ""
                    )
                }

                /** Move2Link Button */
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(
                        modifier = Modifier.padding(8.dp),
                        imageVector = Icons.Outlined.Link,
                        contentDescription = ""
                    )
                }
            }

            /** Body */
            Column(modifier = Modifier.fillMaxWidth()) {
                /** Image */
                AsyncImage(
                    modifier = Modifier.fillMaxWidth(),
                    model = "",
                    contentDescription = "",
                )
                /** Content */
                Text(
                    text = testNode.content,
                    style = Typography.headlineSmall
                )


                /** Description */
                Text(
                    text = testNode.description,
                    style = Typography.bodySmall
                )
            }

            /** Tail */
            Row {

            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NodeDetailPreview() {
    CanvasExampleTheme {
        AddNodeDialog()
    }
}

@Preview(showBackground = true)
@Composable
fun NodeDetailPreview2() {
    CanvasExampleTheme {
        NodeDetailDialog2 {

        }
    }
}