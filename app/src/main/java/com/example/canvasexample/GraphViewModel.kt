package com.example.canvasexample

import android.app.Application
import android.content.res.Resources
import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.canvasexample.algorithm.Quadtree
import com.example.canvasexample.algorithm.applyAttraction
import com.example.canvasexample.algorithm.applyGravity
import com.example.canvasexample.algorithm.applyRepulsion
import com.example.canvasexample.algorithm.getAttraction
import com.example.canvasexample.algorithm.getRepulsion
import com.example.canvasexample.algorithm.getStrongGravity
import com.example.canvasexample.db.AppDatabase
import com.example.canvasexample.db.Edge
import com.example.canvasexample.db.EdgeDao
import com.example.canvasexample.db.Node
import com.example.canvasexample.db.NodeDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.jsoup.Jsoup
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

class GraphViewModel(
    application: Application,
) : ViewModel() {

    companion object {
        private const val TAG: String = "GraphViewModel"
        private const val THRESHOLD = 45
        private const val SNACK_BAR_MESSAGE = "Algorithm running ..."
    }

    // todo: snackBarHostState 잘 동작 하는지 살펴 보기.
    var snackBarHostState = SnackbarHostState()

    private var _db: AppDatabase
    private var _nodeDao: NodeDao
    private var _edgeDao: EdgeDao

    private var _nodes = mutableStateListOf<Node>()
    private var _nodesTemper = mutableStateListOf<Node>()
    private var _edges = mutableStateListOf<Edge>()
    private var _edgesTemper = mutableStateListOf<Edge>()
    private var _notificationNodes = mutableStateListOf<Node>()
    private var _nodeId2Index = mutableMapOf<Long, Int>()
    var nodes: MutableList<Node> = _nodesTemper
    var edges: MutableList<Edge> = _edgesTemper
    var notificationNodes: MutableList<Node> = _notificationNodes
    var nodeId2Index: Map<Long, Int> = _nodeId2Index

    private var _able = MutableLiveData(true)
    var able: LiveData<Boolean> = _able

    /**
     * todo: operate fun 이 한 시점에 한번만 실행 되는 것을 보장하기 위해서 mutex lock을 사용할 예정이다.
     * */

    private var speed = 1.0
    private var speedEfficiency = 1.0
    private var graphCenterOffset: Offset = Offset(0f, 0f)
    private var isStrongGravityMode: Boolean = false
    private var barnesHutOptimize: Boolean = true
    private var gravity: Double = 1.0
    private var scalingRatio: Double = 2.0
    private var edgeWeightInfluence: Double = 1.0
    private var preventingOverlapping: Boolean = true
    private var jitterTolerence: Double = 1.0
    private var barnesHutTheta: Double = 0.5
    private var outboundAttractionDistribution: Boolean = false

    init {
        Log.e(TAG, "ViewModel init...")
        _db = AppDatabase.getInstance(application)!!
        _nodeDao = _db.nodeDao()
        _edgeDao = _db.edgeDao()
        showSnackBar()
        var temp = viewModelScope.async(Dispatchers.IO) {
            val data1 = _nodeDao.getAllNodes()
            val data2 = _edgeDao.getAllEdges()
            _nodes.addAll(data1)
            _nodes.forEachIndexed { index, node -> _nodeId2Index[node.id] = index }
            _nodesTemper.addAll(_nodes)
            _edges.addAll(data2)
            _able.postValue(false)
            operateMain()
            _able.postValue(true)
        }
        dismissSnackBar(temp)
    }

    fun operateByZoomIn(index: Int) {
        if (_able.value!!) {
            showSnackBar()
            var temp = viewModelScope.async(Dispatchers.IO) {
                _able.postValue(false)
                _nodes[index] = _nodes[index].copy(size = 512.0)
                operateMain()
                _able.postValue(true)
            }
            dismissSnackBar(temp)
        }
    }


    // todo: async를 주어 synchronous를 주었는데 잘 동작하는지 확인할 것
    fun operateByZoomOut(index: Int) {
        if (_able.value!!) {
            showSnackBar()
            var temp = viewModelScope.async(Dispatchers.IO) {
                _able.postValue(false)
                _nodes[index] = _nodes[index].copy(size = 32.0)
                operateMain()
                _able.postValue(true)
            }
            dismissSnackBar(temp)
        }
    }

    fun addNode(link: String) {
        if (_able.value!!) {
            showSnackBar()
            var temp = viewModelScope.async(Dispatchers.IO) {
                Log.e(TAG, "addNode: enter async")
                _able.postValue(false)
                var capsule = linkParser(link)
                _db.runInTransaction {
                    val id = _nodeDao.insertNode(
                        x = Random.nextDouble(-10.0, 10.0),
                        y = Random.nextDouble(-10.0, 10.0),
                        imgUri = capsule.imageUrl,
                        linkUrl = link,
                        content = capsule.title,
                        description = capsule.description
                    )
                    val node = _nodeDao.getNodeById(id)
                    Log.e(TAG, "addNode: $node")
                    _nodes.add(node)
                    _nodesTemper.add(node)
                    _nodeId2Index[id] = _nodes.size - 1
                    Log.e(TAG, "addNode: In runInTransaction")
                }
                operateMain()
                _able.postValue(true)
            }

            dismissSnackBar(temp)
        }
    }

    fun showSnackBar() {
        viewModelScope.launch {
            snackBarHostState.showSnackbar(
                message = SNACK_BAR_MESSAGE,
                duration = SnackbarDuration.Indefinite
            )
        }
    }

    fun dismissSnackBar(temp: Any) {
        snackBarHostState.currentSnackbarData?.dismiss()
    }

    fun findCollisionNode(
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
    }

    private fun operateMain(iteration: Int = 800) {

        var repulsion = getRepulsion(1.0)
        var gravityForce = if (isStrongGravityMode) getStrongGravity(1.0) else repulsion
        var outboundAttractionCompensation: Double = _nodes.let {
            var sum = 0.0
            it.forEach { node -> sum += node.mass }
            sum / it.size
        }
        var attractionCoef =
            if (outboundAttractionDistribution) outboundAttractionCompensation else 1.0
        var attraction = getAttraction(attractionCoef)

        for (i in 1..iteration) {
            _nodes.forEach { node ->
                node.old_dx = node.dx
                node.old_dy = node.dy
                node.dx = 0.0
                node.dy = 0.0
            }

            var rootRegion = Quadtree(_nodes)
            rootRegion.build()

            applyRepulsion(repulsion, _nodes, barnesHutOptimize, rootRegion, barnesHutTheta)
            applyGravity(gravityForce, _nodes, gravity, scalingRatio)
            applyAttraction(attraction, _nodes, _edges, _nodeId2Index, edgeWeightInfluence)

            var totalSwinging = 0.0
            var totalEffectiveTraction = 0.0
            _nodes.forEach { node ->
                var swinging =
                    sqrt((node.old_dx - node.dx) * (node.old_dx - node.dx) + (node.old_dy - node.dy) * (node.old_dy - node.dy))
                totalSwinging += node.mass * swinging
                totalEffectiveTraction += .5 * node.mass * sqrt((node.old_dx + node.dx) * (node.old_dx + node.dx) + (node.old_dy + node.dy) * (node.old_dy + node.dy))
            }

            var estimatedOptimalJitterTolerance = .05 * sqrt(_nodes.size.toDouble())
            var minJt = sqrt(estimatedOptimalJitterTolerance)
            var maxJt = 10.0
            var jt = jitterTolerence * max(
                minJt,
                min(
                    maxJt,
                    estimatedOptimalJitterTolerance * totalEffectiveTraction / (_nodes.size * _nodes.size)
                )
            )

            var minSpeedEfficiency = 0.05
            if (totalSwinging / totalEffectiveTraction > 2.0) {
                if (speedEfficiency > minSpeedEfficiency) {
                    speedEfficiency *= 0.5
                }
                jt = max(jt, 1.0)
            }

            var targetSpeed = jt * speedEfficiency * totalEffectiveTraction / totalSwinging
            if (totalSwinging > jt * totalEffectiveTraction) {
                if (speedEfficiency > minSpeedEfficiency) {
                    speedEfficiency *= .7
                }
            } else if (speed < 1000) {
                speedEfficiency *= 1.3
            }

            var maxRise = .5
            speed = speed + min(targetSpeed - speed, maxRise * speed)

            if (preventingOverlapping) {
                _nodes.forEachIndexed { index, node ->
                    var swinging =
                        node.mass * sqrt((node.old_dx - node.dx) * (node.old_dx - node.dx) + (node.old_dy - node.dy) * (node.old_dy - node.dy))
                    var factor = 0.1 * speed / (1 + sqrt(speed * swinging))
                    var df = sqrt(node.dx * node.dx + node.dy * node.dy)
                    factor = min(factor * df, 10.0) / df
                    node.x = node.x + (node.dx * factor)
                    node.y = node.y + (node.dy * factor)
                }
            } else {
                _nodes.forEach { node ->
                    var swinging =
                        node.mass * sqrt((node.old_dx - node.dx) * (node.old_dx - node.dx) + (node.old_dy - node.dy) * (node.old_dy - node.dy))
                    var factor = speed / (1 + sqrt(speed * swinging))
                    node.x = node.x + (node.dx * factor)
                    node.y = node.y + (node.dy * factor)
                }
            }
            if (i % 50 == 0) {
                Log.e("TAG", "operate: #$i")
            }
        }
        _nodes.forEachIndexed { index, node ->
            Log.e(TAG, "operateMain: update $index")
            _nodesTemper[index] = node.copy()
        }
        _edgesTemper.addAll(_edges)
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
                if (property == "og:title") {
                    title = content
                } else if (property == "og:description") {
                    description = content
                } else if (property == "og:image") {
                    imageUrl = content
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

//    fun operateVersion2(
//        iteration: Int = 1000,
//    ) {
//        _nodes.forEach { node ->
//            node.old_dx = 0.0
//            node.old_dy = 0.0
//            node.dx = 0.0
//            node.dy = 0.0
//            node.x = Random.nextDouble(
//                graphCenterOffset.x.toDouble() - 10.0,
//                graphCenterOffset.x.toDouble() + 10.0
//            )
//            node.y = Random.nextDouble(
//                graphCenterOffset.y.toDouble() - 10.0,
//                graphCenterOffset.y.toDouble() + 10.0
//            )
//        }
//        _nodesTemper.addAll(_nodes)
//        //_edgesTemper.addAll(_edges)
//
//        for (i in 1..iteration) {
//            _nodes.forEach { node ->
//                node.old_dx = node.dx
//                node.old_dy = node.dy
//                node.dx = 0.0
//                node.dy = 0.0
//            }
//
//            var rootRegion = Quadtree(_nodes)
//            rootRegion.build()
//
//            applyRepulsion(repulsion, _nodes, barnesHutOptimize, rootRegion, barnesHutTheta)
//            applyGravity(gravityForce, _nodes, gravity, scalingRatio)
//            applyAttraction(attraction, _nodes, _edges, edgeWeightInfluence)
//
//            var totalSwinging = 0.0
//            var totalEffectiveTraction = 0.0
//            _nodes.forEach { node ->
//                var swinging =
//                    sqrt((node.old_dx - node.dx) * (node.old_dx - node.dx) + (node.old_dy - node.dy) * (node.old_dy - node.dy))
//                totalSwinging += node.mass * swinging
//                totalEffectiveTraction += .5 * node.mass * sqrt((node.old_dx + node.dx) * (node.old_dx + node.dx) + (node.old_dy + node.dy) * (node.old_dy + node.dy))
//            }
//
//            var estimatedOptimalJitterTolerance = .05 * sqrt(_nodes.size.toDouble())
//            var minJt = sqrt(estimatedOptimalJitterTolerance)
//            var maxJt = 10.0
//            var jt = jitterTolerence * max(
//                minJt,
//                min(
//                    maxJt,
//                    estimatedOptimalJitterTolerance * totalEffectiveTraction / (_nodes.size * _nodes.size)
//                )
//            )
//
//            var minSpeedEfficiency = 0.05
//            if (totalSwinging / totalEffectiveTraction > 2.0) {
//                if (speedEfficiency > minSpeedEfficiency) {
//                    speedEfficiency *= 0.5
//                }
//                jt = max(jt, 1.0)
//            }
//
//            var targetSpeed = jt * speedEfficiency * totalEffectiveTraction / totalSwinging
//            if (totalSwinging > jt * totalEffectiveTraction) {
//                if (speedEfficiency > minSpeedEfficiency) {
//                    speedEfficiency *= .7
//                }
//            } else if (speed < 1000) {
//                speedEfficiency *= 1.3
//            }
//
//            var maxRise = .5
//            speed = speed + min(targetSpeed - speed, maxRise * speed)
//
//            if (preventingOverlapping) {
//                _nodes.forEachIndexed { index, node ->
//                    var swinging =
//                        node.mass * sqrt((node.old_dx - node.dx) * (node.old_dx - node.dx) + (node.old_dy - node.dy) * (node.old_dy - node.dy))
//                    var factor = 0.1 * speed / (1 + sqrt(speed * swinging))
//                    var df = sqrt(node.dx * node.dx + node.dy * node.dy)
//                    factor = min(factor * df, 10.0) / df
//                    node.x = node.x + (node.dx * factor)
//                    node.y = node.y + (node.dy * factor)
//
//                    _nodesTemper[index] = node.copy()
//                }
//            } else {
//                _nodes.forEach { node ->
//                    var swinging =
//                        node.mass * sqrt((node.old_dx - node.dx) * (node.old_dx - node.dx) + (node.old_dy - node.dy) * (node.old_dy - node.dy))
//                    var factor = speed / (1 + sqrt(speed * swinging))
//                    node.x = node.x + (node.dx * factor)
//                    node.y = node.y + (node.dy * factor)
//                }
//            }
//            if (i % 50 == 0) {
//                Log.e("TAG", "operate: #$i")
//            }
//
//        }
//        _edgesTemper.addAll(_edges)
//    }
}