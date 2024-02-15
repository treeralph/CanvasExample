package com.example.canvasexample

import android.app.Application
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.canvasexample.db.AppDatabase
import com.example.canvasexample.db.Edge
import com.example.canvasexample.db.EdgeDao
import com.example.canvasexample.db.Folder
import com.example.canvasexample.db.FolderDao
import com.example.canvasexample.db.HistoryDao
import com.example.canvasexample.db.Node
import com.example.canvasexample.db.NodeDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jsoup.Jsoup
import kotlin.math.abs
import kotlin.random.Random
import kotlin.system.measureTimeMillis

@Stable
class GraphViewModelNDK(application: Application): ViewModel() {

    companion object {
        private const val TAG: String = "GraphViewModelNDK"
        private const val THRESHOLD = 45
    }

    private val _db: AppDatabase
    private val _nodeDao: NodeDao
    private val _edgeDao: EdgeDao
    private val _folderDao: FolderDao
    private val _historyDao: HistoryDao

    private val _nodesNDK = ArrayList<Node>()
    private val _edgesNDK = ArrayList<Edge>()
    private val _nodeId2Index = HashMap<Long, Int>()

    private val _nodeStates = mutableListOf<MutableState<Node>>()
    private val _edgeStates = mutableListOf<MutableState<Edge>>()

    val nodeStates: List<MutableState<Node>> = _nodeStates
    val edgeStates: List<MutableState<Edge>> = _edgeStates
    val nodeId2Index: Map<Long, Int> = _nodeId2Index

    private val _folders = mutableStateListOf<Folder>()
    private val _notificationNodes = mutableListOf<MutableState<Node>>()

    val folders: List<Folder> = _folders
    val notificationNodes: List<MutableState<Node>> = _notificationNodes

    private var _currentFolder: Folder? = null
    private val _currentFolderId = MutableLiveData<Long>(-1)
    val currentFolderId: LiveData<Long> = _currentFolderId

    private val _mutex = Mutex()

    private val _able = MutableLiveData(true)
    val able: LiveData<Boolean> = _able

    var sharedSheet = ""

    init {
        Log.e(TAG, "init...")
        _db = AppDatabase.getInstance(application)!!
        _nodeDao = _db.nodeDao()
        _edgeDao = _db.edgeDao()
        _folderDao = _db.folderDao()
        _historyDao = _db.historyDao()

        viewModelScope.launch(Dispatchers.IO) {
            // updateLatestFolder()
            if (_folderDao.getAllFolders().isEmpty()) {
                val defaultId = _folderDao.insertFolder(folderName = "default")
                _historyDao.deleteAllHistories()
                _historyDao.insertHistory(latestFolder = defaultId)
            }

            if (_historyDao.getAllHistories().size != 1) {
                _historyDao.deleteAllHistories()
                _historyDao.insertHistory(
                    _folderDao.getAllFolders()[0].id
                )
            }

            _folders.addAll(_folderDao.getAllFolders())
            val latestId = _historyDao.getAllHistories()[0].id
            _currentFolder = _folderDao.getFolderById(latestId)
            _currentFolderId.postValue(latestId)
            updateElements(_currentFolder!!, true)
        }
    }

    fun opAble() {
        if (!_able.value!!) _able.value = true
    }
    fun opDisable() {
        if (_able.value!!) _able.value = false
    }

    fun mutexAcquire() = viewModelScope.launch(Dispatchers.IO) { _mutex.lock() }
    fun mutexRelease() = viewModelScope.launch(Dispatchers.IO) { _mutex.unlock() }

    fun draw() {
        viewModelScope.launch(Dispatchers.IO) {
            while(_able.value!!) {
                val opTime = if(_able.value!!) measureTimeMillis {
                    _mutex.withLock {
                        operateMainNDK(
                            nodes = _nodesNDK,
                            edges = _edgesNDK,
                            nodeId2Index = _nodeId2Index,
                        )
                        viewModelScope.launch(Dispatchers.Main) {
                            _nodesNDK.forEachIndexed { index, node ->
                                if (_onMovedNode.isEmpty() || _nodeId2Index[_onMovedNode[0].id] != index) {
                                    _nodeStates[index].value = node.copy()
                                }
                            }
                        }
                    }
                } else 0
                Log.e(TAG, "draw: opTime: $opTime")
                if(opTime < 20) Thread.sleep(20 - opTime)
            }
        }
    }

    /**
     * History 통해서 폴더를 제어 하려고 하지 말고 참고 정도로 쓰자.
     * History check 후에 Folder 를 가져 온 후에 있으면 만들고 없으면 제일 위에 꺼.
     * 그리고 어플리케이션 종료 시점에 정상적으로 작동하지 못했다고 하더라도 다음에 정보가 없어지는 것을 방지
     * */
    private fun updateLatestFolder() {
        Log.e(TAG, "updateLatestFolder: ")
        viewModelScope.launch(Dispatchers.IO) {
            _mutex.withLock {
                Log.e(TAG, "updateLatestFolder: 0")
                Log.e(TAG, "updateLatestFolder: 1")
                val tempHistory = _historyDao.getAllHistories()
                if (tempHistory.size == 1) {
                    Log.e(TAG, "updateLatestFolder: 2")
                    val tempFolder = _folderDao.getFolderById(tempHistory[0].latestFolder)
                    _currentFolderId.postValue(tempFolder.id)
                    _currentFolder = tempFolder
                } else {
                    Log.e(TAG, "updateLatestFolder: 3")
                    _historyDao.deleteAllHistories()
                    val tempFolders = _folderDao.getAllFolders()
                    if (tempFolders.isEmpty()) {
                        Log.e(TAG, "updateLatestFolder: 4")
                        val newFolderId = _folderDao.insertFolder(
                            folderName = "Hello World!",
                            folderInfo = "Default Folder"
                        )
                        val tempFolder = _folderDao.getFolderById(newFolderId)
                        _currentFolderId.postValue(tempFolder.id)
                        _currentFolder = tempFolder
                    } else {
                        Log.e(TAG, "updateLatestFolder: 5")
                        _currentFolderId.postValue(tempFolders[0].id)
                        _currentFolder = tempFolders[0]
                    }
                }
            }
        }
    }

    fun getNode(nodeId: Long): Node = _nodeStates[_nodeId2Index[nodeId]!!].value

    /**
     * Get Nodes & Edges From folder
     * */
    fun updateElements(folder: Folder, init: Boolean = false) {
        if(init || _currentFolderId.value!! != folder.id) {
            viewModelScope.launch(Dispatchers.IO) {
                _mutex.withLock {
                    _db.runInTransaction {

                        _nodesNDK.clear()
                        _edgesNDK.clear()
                        _nodeId2Index.clear()
                        _nodeStates.clear()
                        _edgeStates.clear()

                        _nodeDao.getNodesByFolder(folder.id).forEachIndexed { index, node ->
                            _nodesNDK.add(node)
                            _nodeStates.add(mutableStateOf(node.copy()))
                            _nodeId2Index[node.id] = index
                        }

                        _edgeDao.getEdgesByFolder(folder.id).forEach { edge ->
                            _edgesNDK.add(edge)
                            _edgeStates.add(mutableStateOf(edge.copy()))
                        }

                        _currentFolder = folder
                        _currentFolderId.postValue(folder.id)
                    }
                }
                Log.i(TAG, "updateElements: folder: $folder")
            }
        }
    }

    fun addFolder(folderName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newFolderId = _folderDao.insertFolder(folderName = folderName)
            val newFolder = _folderDao.getFolderById(newFolderId)
            _folders.add(newFolder)
        }
    }

    fun editNode(node: Node) {
        viewModelScope.launch(Dispatchers.IO) {
            _mutex.withLock {
                _db.nodeDao().updateNodes(listOf(node))
                val tempIndex = _nodeId2Index[node.id]!!
                _nodesNDK[tempIndex] = node.copy()
                _nodeStates[tempIndex].value = node.copy()
            }
        }
    }

    fun editNodeForTest(node: Node) {
        viewModelScope.launch(Dispatchers.IO) {
            _mutex.withLock {
                _db.nodeDao().updateNodes(listOf(node))
                val targetIndex = _nodesNDK.size - 1
                swapNode(_nodeId2Index[node.id]!!, targetIndex)
                _nodeStates.removeAt(targetIndex)
                _nodesNDK.removeAt(targetIndex)
                Thread.sleep(1000)
                _nodesNDK.add(node.copy())
                _nodeStates.add(mutableStateOf(node.copy()))
            }


//            deleteNode(node)
//            _mutex.withLock {
//                val id = _nodeDao.insertNode(
//                    x = node.x,
//                    y = node.y,
//                    imgUri = node.imgUri,
//                    linkUrl = node.linkUrl,
//                    content = node.content,
//                    description = node.description,
//                    folder = _currentFolderId.value ?: -1
//                )
//                val node = _nodeDao.getNodeById(id)
//            }
        }
    }

    fun deleteNode(node: Node) {
        viewModelScope.launch(Dispatchers.IO) {
            _mutex.withLock {
                _db.runInTransaction {
                    /** delete edge & node ( including _edges, _nodes, _nodeId2Index ) */
                    val targetIndex = _nodesNDK.size - 1
                    _db.nodeDao().deleteNodes(listOf(node))
                    _db.edgeDao().deleteEdgesByNodeId(node.id)
                    swapNode(_nodeId2Index[node.id]!!, targetIndex) // send target node to list tail

                    _edgesNDK.removeIf { it.node1 == node.id || it.node2 == node.id }
                    _edgeStates.removeIf { it.value.node1 == node.id || it.value.node2 == node.id }
                    _nodesNDK.removeAt(targetIndex)
                    _nodeStates.removeAt(targetIndex)

                    _nodeId2Index.remove(node.id)
                }
            }
        }
    }

    private fun swapNode(index1: Int, index2: Int) {
        val temp1 = _nodesNDK[index1].copy()
        val temp2 = _nodesNDK[index2].copy()
        val temp3 = _nodeStates[index1].value.copy()
        val temp4 = _nodeStates[index2].value.copy()

        _nodesNDK[index1] = temp2
        _nodesNDK[index2] = temp1
        _nodeStates[index1].value = temp4
        _nodeStates[index2].value = temp3
        _nodeId2Index[temp1.id] = index2
        _nodeId2Index[temp2.id] = index1
    }

    private fun subAddNode(link: String, capsule: LinkCapsule) {
        _db.runInTransaction {
            val id = _nodeDao.insertNode(
                x = Random.nextDouble(-30.0, 30.0),
                y = Random.nextDouble(-30.0, 30.0),
                imgUri = capsule.imageUrl,
                linkUrl = link,
                content = capsule.title,
                description = capsule.description,
                folder = _currentFolderId.value ?: -1
            )
            val node = _nodeDao.getNodeById(id)
            Log.e(TAG, "addNode: $node")
            _nodesNDK.add(node.copy())
            _nodeStates.add(mutableStateOf(node.copy()))
            _nodeId2Index[id] = _nodesNDK.size - 1
        }
    }
    fun addNode(link: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _mutex.withLock {
                val capsule = try {
                    linkParserV2(link)
                } catch(e: IllegalArgumentException) {
                    LinkCapsule(title = link)
                }
                subAddNode(link, capsule)
            }
        }
    }

    private fun linkParserV2(link: String): LinkCapsule {
        val result = LinkCapsule()
        if (link.isNotEmpty()) {
            val doc = Jsoup.connect(link).get()
            val metaTags = doc.select("meta[property^=og:]")
            var title: String? = null
            var description: String? = null
            var imageUrl: String? = null
            for (metaTag in metaTags) {
                val property = metaTag.attr("property")
                val content = metaTag.attr("content")
                when (property) {
                    "og:title" -> {
                        title = content
                    }

                    "og:description" -> {
                        description = content
                    }

                    "og:image" -> {
                        imageUrl = content
                    }
                }
            }
            result.title = title ?: ""
            result.description = description ?: ""
            result.imageUrl = imageUrl ?: ""
        }
        return result
    }

    private fun isCollision(
        x1: Double,
        y1: Double,
        x2: Double,
        y2: Double,
    ): Boolean = (abs(x1 - x2) < THRESHOLD && abs(y1 - y2) < THRESHOLD)

    fun saveData() {
        viewModelScope.launch(Dispatchers.IO) {
            _nodeDao.updateNodes(_nodesNDK)
            _edgeDao.updateEdges(_edgesNDK)
            _historyDao.updateHistory(
                _historyDao.getAllHistories()[0].copy(latestFolder = _currentFolderId.value ?: -1)
            )

            Log.i(TAG, "saveData: Node, Edge save: success")
        }
    }

    private val _onMovedNode = mutableStateListOf<Node>()

    fun onNodeDragStart(node: Node) {
        viewModelScope.launch(Dispatchers.IO) {
            _mutex.lock()
            if (_onMovedNode.size != 0) {
                _onMovedNode.clear()
            }
            _onMovedNode.add(node.copy())
        }
    }

    fun onNodeDragEnd(index: Int, node: Node) {
        viewModelScope.launch(Dispatchers.IO) {
            run a@ {
                if (_notificationNodes.size != 1) {
                    _nodesNDK[index] = _nodeStates[index].value.copy()
                    _onMovedNode.clear()
                    _notificationNodes.clear()
                    return@a
                }

                addEdge(node.id, _notificationNodes[0].value.id)?.let { currentEdge ->
                    _edgesNDK.add(currentEdge)
                    _edgeStates.add(mutableStateOf(currentEdge.copy()))
                    _onMovedNode.clear()
                    _notificationNodes.clear()
                }
            }
            _mutex.unlock()
        }
    }

    fun onNodeMoved(index: Int, offset: Offset) {
        val target = _nodeStates[index].value
        val x = target.x + offset.x
        val y = target.y + offset.y
        _nodeStates[index].value = target.copy(x = x, y = y)
        // _nodes[index] = _nodesTemper[index].copy() // anti collision
        findCollisionNode(index, x, y)
    }

    private fun findCollisionNode(
        selfId: Int,
        selfX: Double,
        selfY: Double,
    ) {
        var targetId: Int = -1
        _nodesNDK.forEachIndexed { index, node ->
            if (isCollision(node.x, node.y, selfX, selfY)) {
                if (index != selfId) {
                    targetId = index
                }
            }
        }
        if (targetId == -1) {
            _notificationNodes.clear()
        } else {
            _notificationNodes.clear()
            _notificationNodes.add(mutableStateOf(_nodesNDK[targetId].copy()))
        }
        Log.e(TAG, "findCollisionNode: ${_notificationNodes.size}")
    }

    private fun addEdge(node1: Long, node2: Long): Edge? {
        if(_edgeDao.isEdge(node1, node2).isEmpty()) {
            val edgeId = _edgeDao.insertEdge(node1, node2, _currentFolderId.value ?: -1)
            _nodesNDK[_nodeId2Index[node1]!!].mass += 1
            _nodesNDK[_nodeId2Index[node2]!!].mass += 1
            return _edgeDao.getEdgeById(edgeId)
        }
        return null
    }

    data class LinkCapsule(
        var title: String = "",
        var description: String = "",
        var imageUrl: String = "",
    )

    private external suspend fun operateMainNDK(
        nodes: ArrayList<Node>,
        edges: ArrayList<Edge>,
        nodeId2Index: HashMap<Long, Int>,
    )
}
