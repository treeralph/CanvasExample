package com.example.canvasexample

import android.app.Application
import android.content.res.Resources
import android.util.Log
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

class GraphViewModel(application: Application) : ViewModel() {

    companion object {
        private const val TAG: String = "GraphViewModel"
    }

    private var _db: AppDatabase
    private var _nodeDao: NodeDao
    private var _edgeDao: EdgeDao

    private var _nodes = mutableStateListOf<Node>()
    private var _nodesTemper = mutableStateListOf<Node>()
    private var _edges = mutableStateListOf<Edge>()
    private var _edgesTemper = mutableStateListOf<Edge>()
    private var _notificationNodes = mutableStateListOf<Node>()
    var nodes: MutableList<Node> = _nodesTemper
    var edges: MutableList<Edge> = _edgesTemper
    var notificationNodes: MutableList<Node> = _notificationNodes

    private var _able = MutableLiveData(true)
    var able: LiveData<Boolean> = _able

    private val screenWidthCenter = Resources.getSystem().displayMetrics.widthPixels / 2
    private val screenHeightCenter = Resources.getSystem().displayMetrics.heightPixels / 2
    private var _dx = MutableLiveData(screenWidthCenter)
    private var _dy = MutableLiveData(screenHeightCenter)
    var dx: LiveData<Int> = _dx
    var dy: LiveData<Int> = _dy

    init {
        Log.e(TAG, "ViewModel init...")
        _db = AppDatabase.getInstance(application)!!
        _nodeDao = _db.nodeDao()
        _edgeDao = _db.edgeDao()
        viewModelScope.launch(Dispatchers.IO) {
            val data1 = _nodeDao.getAllNodes()
            val data2 = _edgeDao.getAllEdges()
            _nodes.addAll(data1)
            _nodesTemper.addAll(_nodes)
            _edges.addAll(data2)
            operateMain()
        }
    }

    var speed = 1.0
    var speedEfficiency = 1.0

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

    private var repulsion = getRepulsion(1.0)
    private var gravityForce = if (isStrongGravityMode) getStrongGravity(1.0) else repulsion
    private var outboundAttractionCompensation: Double = _nodes.let {
        var sum = 0.0
        it.forEach { node -> sum += node.mass }
        sum / it.size
    }
    private var attractionCoef =
        if (outboundAttractionDistribution) outboundAttractionCompensation else 1.0
    private var attraction = getAttraction(attractionCoef)

    fun addNode() {
        viewModelScope.launch(Dispatchers.IO) {
            var id = _nodeDao.insertNode(
                x = Random.nextDouble(
                    graphCenterOffset.x.toDouble() - 10.0,
                    graphCenterOffset.x.toDouble() + 10.0
                ),
                y = Random.nextDouble(
                    graphCenterOffset.y.toDouble() - 10.0,
                    graphCenterOffset.y.toDouble() + 10.0
                )
            )
            var latestNode = _nodeDao.getNodeById(id)
            _nodes.add(latestNode)
            _nodesTemper.add(latestNode)
            operateMain()
        }
    }

    fun operateV2() {
        _edges.clear()
        _nodes.clear()
        _edgesTemper.clear()
        _nodesTemper.clear()
        viewModelScope.launch(Dispatchers.IO) {
            _able.postValue(false)
            operate()
            _able.postValue(true)
        }
    }

    fun operateMainV2(index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _able.postValue(false)
            _nodes[index] = _nodes[index].copy(size = 512.0)
            operateMain()
            _able.postValue(true)
        }
    }

    fun operateMainV3(index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _able.postValue(false)
            _nodes[index] = _nodes[index].copy(size = 32.0)
            operateMain()
            _able.postValue(true)
        }
    }

    fun move(coordinate: Offset) {
        _dx.value = _dx.value!! - (coordinate.x - screenWidthCenter).toInt()
        _dy.value = _dy.value!! - (coordinate.y - screenHeightCenter).toInt()
    }


    fun operateMain(iteration: Int = 800) {
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
            applyAttraction(attraction, _nodes, _edges, edgeWeightInfluence)

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

                    _nodesTemper[index] = node.copy()
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
    }

    fun operateVersion2(
        iteration: Int = 1000,
    ) {
        _nodes.forEach { node ->
            node.old_dx = 0.0
            node.old_dy = 0.0
            node.dx = 0.0
            node.dy = 0.0
            node.x = Random.nextDouble(
                graphCenterOffset.x.toDouble() - 10.0,
                graphCenterOffset.x.toDouble() + 10.0
            )
            node.y = Random.nextDouble(
                graphCenterOffset.y.toDouble() - 10.0,
                graphCenterOffset.y.toDouble() + 10.0
            )
        }
        _nodesTemper.addAll(_nodes)
        //_edgesTemper.addAll(_edges)

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
            applyAttraction(attraction, _nodes, _edges, edgeWeightInfluence)

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

                    _nodesTemper[index] = node.copy()
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
        _edgesTemper.addAll(_edges)
    }


    fun operate(
        graph: List<List<Int>> = initGraph(),
        iteration: Int = 1000,
        graphCenterOffset: Offset = Offset(0f, 0f),
        isStrongGravityMode: Boolean = false,
        barnesHutOptimize: Boolean = true,
        gravity: Double = 1.0,
        scalingRatio: Double = 2.0,
        edgeWeightInfluence: Double = 1.0,
        preventingOverlapping: Boolean = true,
        jitterTolerence: Double = 1.0,
        barnesHutTheta: Double = 0.5,
        outboundAttractionDistribution: Boolean = false,
    ) {
        graph.forEachIndexed { rowIndex, info ->
            var node = Node()
            node.mass = 1 + info.let { row ->
                var sum = 0.0
                row.forEachIndexed { columnIndex, elem ->
                    if (elem != 0) {
                        sum++
                        if (columnIndex < rowIndex) {
                            var edge = Edge(node1 = rowIndex, node2 = columnIndex, weight = 1.0)
                            _edges.add(edge)
                        }
                    }
                }
                sum
            }
            node.old_dx = 0.0
            node.old_dy = 0.0
            node.dx = 0.0
            node.dy = 0.0
            node.x = Random.nextDouble(
                graphCenterOffset.x.toDouble() - 10.0,
                graphCenterOffset.x.toDouble() + 10.0
            )
            node.y = Random.nextDouble(
                graphCenterOffset.y.toDouble() - 10.0,
                graphCenterOffset.y.toDouble() + 10.0
            )
            // node.size = node.size + node.mass
            _nodes.add(node)
        }

        _nodesTemper.addAll(_nodes)
        //_edgesTemper.addAll(_edges)

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
            applyAttraction(attraction, _nodes, _edges, edgeWeightInfluence)

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

                    _nodesTemper[index] = node.copy()
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
        _edgesTemper.addAll(_edges)
    }


}