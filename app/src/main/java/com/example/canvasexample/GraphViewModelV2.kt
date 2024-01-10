package com.example.canvasexample

import android.app.Application
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.canvasexample.db.AppDatabase
import com.example.canvasexample.db.Edge
import com.example.canvasexample.db.EdgeDao
import com.example.canvasexample.db.Node
import com.example.canvasexample.db.NodeDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.Dispatcher
import org.jsoup.Jsoup
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class GraphViewModelV2(
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

    // todo: snackBarHostState 잘 동작 하는지 살펴 보기.
    val snackBarHostState = SnackbarHostState()

    private val _db: AppDatabase
    private val _nodeDao: NodeDao
    private val _edgeDao: EdgeDao

    private val _nodes = mutableStateListOf<Node>()
    private val _nodesTemper = mutableStateListOf<Node>()
    private val _edges = mutableStateListOf<Edge>()
    private val _edgesTemper = mutableStateListOf<Edge>()
    private val _notificationNodes = mutableStateListOf<Node>()
    private val _nodeId2Index = mutableMapOf<Long, Int>()
    val nodes: MutableList<Node> = _nodesTemper
    val edges: MutableList<Edge> = _edgesTemper
    val notificationNodes: MutableList<Node> = _notificationNodes
    val nodeId2Index: Map<Long, Int> = _nodeId2Index

    private val _able = MutableLiveData(true)
    val able: MutableLiveData<Boolean> = _able

    init {
        Log.e(TAG, "ViewModelV2 init...")
        _db = AppDatabase.getInstance(application)!!
        _nodeDao = _db.nodeDao()
        _edgeDao = _db.edgeDao()
        viewModelScope.launch(Dispatchers.IO) {
            val data1 = _nodeDao.getAllNodes()
            val data2 = _edgeDao.getAllEdges()
            _nodes.addAll(data1)
            _nodes.forEachIndexed { index, node -> _nodeId2Index[node.id] = index }
            _nodesTemper.addAll(data1)
            _edges.addAll(data2)
            _edgesTemper.addAll(data2)
            draw()
        }
    }

    private fun draw() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                if(_able.value!!) {
                    val temp = measureTimeMillis {
                        operateMain()
                        Thread.sleep(10)
                    }
                    Log.e(TAG, "draw: operateMain() op time: $temp")
                }
            }
        }
    }

    fun able2True() { _able.value = true }
    fun able2False() { _able.value = false }

    fun deleteNode(node: Node) {
        viewModelScope.launch(Dispatchers.IO) {
            _able.postValue(false)
            Thread.sleep(500)
            _db.runInTransaction {
                /** delete edge & node ( including _edges, _nodes, _nodeId2Index ) */
                val targetIndex = _nodes.size - 1
                _db.nodeDao().deleteNodes(listOf(node))
                _db.edgeDao().deleteEdgesByNodeId(node.id)
                swapNode(_nodeId2Index[node.id]!!, targetIndex) // send target node to list tail
                _edges.removeIf { it.node1 == node.id || it.node2 == node.id }
                _edgesTemper.removeIf { it.node1 == node.id || it.node2 == node.id }
                _nodes.removeAt(targetIndex)
                _nodesTemper.removeAt(targetIndex)
                _nodeId2Index.remove(node.id)

                Log.i(TAG, "deleteNode: _nodes size: ${_nodes.size}")
                Log.i(TAG, "deleteNode: _nodesTemper size: ${_nodesTemper.size}")
            }
            _able.postValue(true)
        }
    }

    private fun swapNode(index1: Int, index2: Int) {
        val temp1 = _nodes[index1].copy()
        val temp2 = _nodes[index2].copy()
        val temp3 = _nodesTemper[index1].copy()
        val temp4 = _nodesTemper[index2].copy()

        _nodes[index1] = temp2
        _nodes[index2] = temp1
        _nodesTemper[index1] = temp4
        _nodesTemper[index2] = temp3
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
                description = capsule.description
            )
            val node = _nodeDao.getNodeById(id)
            Log.e(TAG, "addNode: $node")
            _nodes.add(node.copy())
            _nodesTemper.add(node.copy())
            _nodeId2Index[id] = _nodes.size - 1
            Log.e(TAG, "addNode: In runInTransaction")
        }
    }

    fun addNode(link: String, isNonLink: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val capsule = if(isNonLink) {
                LinkCapsule(title = link)
            }else {
                linkParserV2(link)
            }
            subAddNode(link, capsule)
        }
    }

    private fun findCollisionNode(
        selfId: Int,
        selfX: Double,
        selfY: Double,
    ) {
        var targetId: Int = -1
        _nodes.forEachIndexed { index, node ->
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
            _notificationNodes.add(_nodes[targetId].copy())
        }
        Log.e(TAG, "findCollisionNode: ${_notificationNodes.size}")
    }

    private fun operateMain() {
        for (i in 0 until _nodes.size) {
            var fx = 0.0
            var fy = 0.0
            for (j in 0 until _nodes.size) {
                val distX = (_nodes[i].x + _nodes[i].size / 2) - (_nodes[j].x + _nodes[i].size / 2)
                val distY = (_nodes[i].y + _nodes[i].size / 2) - (_nodes[j].y + _nodes[i].size / 2)
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

            val distXC = -1 * (_nodes[i].x + _nodes[i].size / 2)
            val distYC = -1 * (_nodes[i].y + _nodes[i].size / 2)
            fx += GRAVITY * distXC
            fy += GRAVITY * distYC

            for (j in 0 until _edges.size) {
                var distX = 0.0
                var distY = 0.0
                if (i == _nodeId2Index[_edges[j].node1]) {
                    distX = _nodes[_nodeId2Index[_edges[j].node2]!!].x - _nodes[i].x
                    distY = _nodes[_nodeId2Index[_edges[j].node2]!!].y - _nodes[i].y
                } else if (i == _nodeId2Index[_edges[j].node2]) {
                    distX = _nodes[_nodeId2Index[_edges[j].node1]!!].x - _nodes[i].x
                    distY = _nodes[_nodeId2Index[_edges[j].node1]!!].y - _nodes[i].y
                }

                fx += BOUNCE * distX
                fy += BOUNCE * distY
            }
            try {
                if (!_onMovedNode.isEmpty() && _nodeId2Index[_onMovedNode[0].id] == i) continue
            } catch (e: Exception) { }

            _nodes[i].dx = (_nodes[i].dx + fx) * ATTENUATION
            _nodes[i].dy = (_nodes[i].dy + fy) * ATTENUATION

            _nodes[i].x += nodes[i].dx
            _nodes[i].y += nodes[i].dy

            _nodesTemper[i] = _nodes[i].copy()
        }
    }

    private fun linkParserV2(link: String): LinkCapsule {
        var result = LinkCapsule()
        if (!link.isEmpty()) {
            val doc = Jsoup.connect(link).get()
            val metaTags = doc.select("meta[property^=og:]")
            var title: String? = null
            var description: String? = null
            var imageUrl: String? = null
            for (metaTag in metaTags) {
                val property = metaTag.attr("property")
                val content = metaTag.attr("content")
                when (property) {
                    "og:title" -> { title = content }
                    "og:description" -> { description = content }
                    "og:image" -> { imageUrl = content }
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
            var capsule = LinkCapsule()
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

    fun isCollision(
        x1: Double,
        y1: Double,
        x2: Double,
        y2: Double,
    ): Boolean = (abs(x1 - x2) < THRESHOLD && abs(y1 - y2) < THRESHOLD)

    fun saveData() {
        viewModelScope.launch(Dispatchers.IO) {
            var nodeSavable: MutableList<Node> = mutableListOf()
            var edgeSavable: MutableList<Edge> = mutableListOf()
            _nodes.forEach { node -> nodeSavable.add(node) }
            _edges.forEach { edge -> edgeSavable.add(edge) }
            _db.nodeDao().updateNodes(nodeSavable)
            _db.edgeDao().updateEdges(edgeSavable)
            Log.i(TAG, "saveData: Node, Edge save: success")
        }
    }

    data class LinkCapsule(
        var title: String = "",
        var description: String = "",
        var imageUrl: String = ""
    )

    /** TEST */
    private val _onMovedNode = mutableStateListOf<Node>()
    fun onNodeDragStart(node: Node) {
        if (_onMovedNode.size != 0) {
            _onMovedNode.clear()
        }
        _onMovedNode.add(node.copy())
    }

    fun onNodeDragEnd(index: Int, node: Node) {

        if (_notificationNodes.size != 1) {
            // _nodesTemper[index] = _onMovedNode[0].copy()
            _nodes[index] = _nodesTemper[index].copy()
            _onMovedNode.clear()
            _notificationNodes.clear()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val currentEdge = addEdge(node.id, _notificationNodes[0].id)
            _edges.add(currentEdge)
            _edgesTemper.add(currentEdge)
            // operateMain()
            _onMovedNode.clear()
            _notificationNodes.clear()
        }
    }

    fun onNodeMoved(index: Int, offset: Offset) {
        val target = _nodesTemper[index]
        val x = target.x + offset.x
        val y = target.y + offset.y
        _nodesTemper[index] = _nodesTemper[index].copy(x = x, y = y)
        // _nodes[index] = _nodesTemper[index].copy() // anti collision
        findCollisionNode(index, x, y)
    }

    /**
     * TODO: Duplication Detect
     * */
    private suspend fun addEdge(node1: Long, node2: Long): Edge {
        val edgeId = _edgeDao.insertEdge(node1, node2)
        _nodes[_nodeId2Index[node1]!!].mass += 1
        _nodes[_nodeId2Index[node2]!!].mass += 1
        return _edgeDao.getEdgeById(edgeId)
    }
}
