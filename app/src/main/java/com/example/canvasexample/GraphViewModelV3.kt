package com.example.canvasexample

import android.app.Application
import android.util.Log
import androidx.compose.runtime.MutableState
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
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jsoup.Jsoup
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

class GraphViewModelV3(
    application: Application,
) : ViewModel() {

    companion object {
        private const val TAG: String = "GraphViewModel"
        private const val THRESHOLD = 45

        /** operating variable */
        private const val COULOMB = 4800.0
        private const val DISTANCE = 3200
        private const val GRAVITY = 0.04
        private const val BOUNCE = 0.06
        private const val ATTENUATION = 0.4
    }

    private val _db: AppDatabase
    private val _nodeDao: NodeDao
    private val _edgeDao: EdgeDao
    private val _folderDao: FolderDao
    private val _historyDao: HistoryDao

    private lateinit var _currentFolder: Folder
    private val _currentFolderId = MutableLiveData<Long>(-1)
    val currentFolderId: MutableLiveData<Long> = _currentFolderId

    private val _nodes = mutableListOf<MutableState<Node>>()
    private val _edges = mutableListOf<MutableState<Edge>>()
    private val _notificationNodes = mutableListOf<MutableState<Node>>()

    val nodes: List<MutableState<Node>> = _nodes
    val edges: List<MutableState<Edge>> = _edges
    val notificationNodes: List<MutableState<Node>> = _notificationNodes

    private val _nodeId2Index = mutableMapOf<Long, Int>()
    private val _folders = mutableStateListOf<Folder>()

    val nodeId2Index: Map<Long, Int> = _nodeId2Index
    val folders: MutableList<Folder> = _folders

    private val _mutex = Mutex()

    private val _able = MutableLiveData(true)
    val able: LiveData<Boolean> = _able

    init {
        Log.e(TAG, "ViewModelV2 init...")
        _db = AppDatabase.getInstance(application)!!
        _nodeDao = _db.nodeDao()
        _edgeDao = _db.edgeDao()
        _folderDao = _db.folderDao()
        _historyDao = _db.historyDao()

        viewModelScope.launch(Dispatchers.IO) {

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
            updateElements(_currentFolder, init = true)

            draw()
        }
    }

    fun draw() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                _mutex.withLock { operateMain() }
                Thread.sleep(10)
            }
        }
    }

    fun opAble() {
        if (!_able.value!!) _able.value = true
    }
    fun opDisable() {
        if (_able.value!!) _able.value = false
    }

    fun updateElements(folder: Folder, init: Boolean = false) {
        if (_currentFolderId.value != folder.id || init) {
            _currentFolder = folder
            _currentFolderId.postValue(folder.id)
            viewModelScope.launch(Dispatchers.IO) {
                _mutex.withLock {
                    _nodes.clear()
                    _edges.clear()
                    _nodeId2Index.clear()

                    val data1 = _nodeDao.getNodesByFolder(folder.id)
                    val data2 = _edgeDao.getEdgesByFolder(folder.id)

                    // visualSizeOfNodes(data1) /* TEST */
                    data1.forEach { node -> _nodes.add(mutableStateOf(node)) }
                    _nodes.forEachIndexed { index, nodeState ->
                        _nodeId2Index[nodeState.value.id] = index
                    }
                    data2.forEach { edge -> _edges.add(mutableStateOf(edge)) }
                }
            }
        }
    }

    private fun visualSizeOfNodes(nodes: List<Node>) {

        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = Double.MIN_VALUE
        var maxY = Double.MIN_VALUE

        nodes.forEach {
            if(it.x > maxX) maxX = it.x
            if(it.y > maxY) maxY = it.y
            if(it.x < minX) minX = it.x
            if(it.y < minY) minY = it.y
        }

        Log.i(TAG, "visualSizeOfNodes: $minX < node.x < $maxX, $minY < node.y < $maxY")
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
                _nodes[_nodeId2Index[node.id]!!].value = node.copy()
            }
        }
    }
    fun deleteNode(node: Node) {
        viewModelScope.launch(Dispatchers.IO) {
            _mutex.withLock {
                _db.runInTransaction {
                    val targetIndex = _nodes.size - 1
                    _db.nodeDao().deleteNodes(listOf(node))
                    _db.edgeDao().deleteEdgesByNodeId(node.id)
                    swapNode(_nodeId2Index[node.id]!!, targetIndex) // send target node to list tail
                    _edges.removeIf { it.value.node1 == node.id || it.value.node2 == node.id }
                    _nodes.removeAt(targetIndex)
                    _nodeId2Index.remove(node.id)
                }
            }
        }
    }
    private fun swapNode(index1: Int, index2: Int) {
        val temp1 = _nodes[index1].value.copy()
        val temp2 = _nodes[index2].value.copy()

        _nodes[index1].value = temp2
        _nodes[index2].value = temp1
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
            _nodes.add(mutableStateOf(node.copy()))
            _nodeId2Index[id] = _nodes.size - 1
            Log.e(TAG, "addNode: In runInTransaction")
        }
    }
    fun addNode(link: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _mutex.withLock {
                val capsule = try{
                    linkParserV2(link)
                } catch(e: IllegalArgumentException) {
                    LinkCapsule(title = link)
                }
                subAddNode(link, capsule)
            }
        }
    }
    private fun findCollisionNode(
        selfId: Int,
        selfX: Double,
        selfY: Double,
    ) {
        var targetId: Int = -1
        _nodes.forEachIndexed { index, nodeState ->
            if (isCollision(nodeState.value.x, nodeState.value.y, selfX, selfY)) {
                if (index != selfId) {
                    targetId = index
                }
            }
        }
        if (targetId == -1) {
            _notificationNodes.clear()
        } else {
            _notificationNodes.clear()
            _notificationNodes.add(
                mutableStateOf(_nodes[targetId].value.copy())
            )
        }
    }

    private fun operateMain() {

        if(!_able.value!!) {
            Thread.sleep(20)
            return
        }

        for (i in 0 until _nodes.size) {
            var fx = 0.0
            var fy = 0.0
            for (j in 0 until _nodes.size) {
                val distX = (_nodes[i].value.x + _nodes[i].value.size / 2) - (_nodes[j].value.x + _nodes[i].value.size / 2)
                val distY = (_nodes[i].value.y + _nodes[i].value.size / 2) - (_nodes[j].value.y + _nodes[i].value.size / 2)
                var rsq = distX * distX + distY * distY
                val rsqRound = rsq.toInt() * 100
                rsq = (rsqRound / 100).toDouble()

                var coulombDistX = COULOMB * distX
                var coulombDistY = COULOMB * distY
                val coulombDistRoundX = coulombDistX.toInt() * 100
                val coulombDistRoundY = coulombDistY.toInt() * 100
                coulombDistX = (coulombDistRoundX / 100).toDouble()
                coulombDistY = (coulombDistRoundY / 100).toDouble()

                if (rsq != 0.0 && sqrt(rsq) < DISTANCE) {
                    fx += coulombDistX / rsq
                    fy += coulombDistY / rsq
                }
            }

            val distXC = -1 * (_nodes[i].value.x + _nodes[i].value.size / 2)
            val distYC = -1 * (_nodes[i].value.y + _nodes[i].value.size / 2)
            fx += GRAVITY * distXC
            fy += GRAVITY * distYC

            for (j in 0 until _edges.size) {
                var distX = 0.0
                var distY = 0.0
                if (i == _nodeId2Index[_edges[j].value.node1]) {
                    distX = _nodes[_nodeId2Index[_edges[j].value.node2]!!].value.x - _nodes[i].value.x
                    distY = _nodes[_nodeId2Index[_edges[j].value.node2]!!].value.y - _nodes[i].value.y
                } else if (i == _nodeId2Index[_edges[j].value.node2]) {
                    distX = _nodes[_nodeId2Index[_edges[j].value.node1]!!].value.x - _nodes[i].value.x
                    distY = _nodes[_nodeId2Index[_edges[j].value.node1]!!].value.y - _nodes[i].value.y
                }

                fx += BOUNCE * distX
                fy += BOUNCE * distY
            }
            try {
                if (_onMovedNode.isNotEmpty() && _nodeId2Index[_onMovedNode[0].id] == i) continue
            } catch (e: Exception) { }

            _nodes[i].value.dx = (_nodes[i].value.dx + fx) * ATTENUATION
            _nodes[i].value.dy = (_nodes[i].value.dy + fy) * ATTENUATION

            _nodes[i].value.x += _nodes[i].value.dx
            _nodes[i].value.y += _nodes[i].value.dy

            _nodes[i].value = _nodes[i].value.copy()
        }
    }

    private fun linkParserV2(link: String): LinkCapsule {
        var result = LinkCapsule()
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

    private suspend fun linkParser(link: String): LinkCapsule {
        return viewModelScope.async(Dispatchers.IO) {
            val capsule = LinkCapsule()
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
            capsule.title = title ?: ""
            capsule.description = description ?: ""
            capsule.imageUrl = imageUrl ?: ""
            capsule
        }.await()
    }

    private fun isCollision(
        x1: Double,
        y1: Double,
        x2: Double,
        y2: Double,
    ): Boolean = (abs(x1 - x2) < THRESHOLD && abs(y1 - y2) < THRESHOLD)

    fun saveData() {
        viewModelScope.launch(Dispatchers.IO) {
            val nodeSavable: MutableList<Node> = mutableListOf()
            val edgeSavable: MutableList<Edge> = mutableListOf()
            _nodes.forEach { nodeState -> nodeSavable.add(nodeState.value) }
            _edges.forEach { edgeState -> edgeSavable.add(edgeState.value) }
            _nodeDao.updateNodes(nodeSavable)
            _edgeDao.updateEdges(edgeSavable)

            _historyDao.updateHistory(
                _historyDao.getAllHistories()[0].copy(latestFolder = _currentFolderId.value ?: -1)
            )

            Log.i(TAG, "saveData: Node, Edge save: success")
        }
    }

    data class LinkCapsule(
        var title: String = "",
        var description: String = "",
        var imageUrl: String = "",
    )

    private val _onMovedNode = mutableStateListOf<Node>()
    fun onNodeDragStart(node: Node) {
        if (_onMovedNode.size != 0) {
            _onMovedNode.clear()
        }
        _onMovedNode.add(node.copy())
    }


    fun onNodeDragEnd(index: Int, node: Node) {

        viewModelScope.launch(Dispatchers.IO) {
            _mutex.withLock {
                run a@ {
                    if (_notificationNodes.size != 1) {
                        _onMovedNode.clear()
                        _notificationNodes.clear()
                        return@a
                    }

                    addEdge(node.id, _notificationNodes[0].value.id)?.let { currentEdge ->
                        _edges.add(mutableStateOf(currentEdge))
                        _onMovedNode.clear()
                        _notificationNodes.clear()
                    }
                }
            }
        }
    }

    fun onNodeMoved(index: Int, offset: Offset) {
        val target = _nodes[index].value
        val x = target.x + offset.x
        val y = target.y + offset.y
        _nodes[index].value = _nodes[index].value.copy(x = x, y = y)
        findCollisionNode(index, x, y)
    }

    /**
     * TODO: Duplication Detect
     * */
    private fun addEdge(node1: Long, node2: Long): Edge? {
        if(_edgeDao.isEdge(node1, node2).isEmpty()) {
            val edgeId = _edgeDao.insertEdge(node1, node2, _currentFolderId.value ?: -1)
            _nodes[_nodeId2Index[node1]!!].value.mass += 1
            _nodes[_nodeId2Index[node2]!!].value.mass += 1
            return _edgeDao.getEdgeById(edgeId)
        }
        return null
    }
}
