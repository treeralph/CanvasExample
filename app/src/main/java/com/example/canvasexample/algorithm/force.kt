package com.example.canvasexample.algorithm

import com.example.canvasexample.db.Edge
import com.example.canvasexample.db.Node
import kotlin.math.log
import kotlin.math.pow
import kotlin.math.sqrt

fun applyRepulsion(
    repulsion: RepulsionForce,
    nodes: MutableList<Node>,
    barnes_hut_optimize: Boolean = true,
    region: Quadtree,
    barnes_hut_theta: Double = 1.2
) {
    if(!barnes_hut_optimize) {
        for(i in 0 until nodes.size) {
            for(j in 0 until i) {
                repulsion.applyNodeToNode(nodes[i], nodes[j])
            }
        }
    }else {
        nodes.forEach { node ->
            region.applyForce(node, repulsion, barnes_hut_theta)
        }
    }
}

fun applyGravity(
    repulsion: RepulsionForce,
    nodes: MutableList<Node>,
    gravity: Double,
    scalingRatio: Double
) {
    nodes.forEach { node ->
        repulsion.applyGravitation(node, gravity / scalingRatio)
    }
}

fun applyAttraction(
    attraction: AttractionForce,
    nodes: MutableList<Node>,
    edges: MutableList<Edge>,
    nodeId2Index: MutableMap<Long, Int>,
    edgeWeightInfluence: Double
) {
    if(edgeWeightInfluence == 0.0) {
        edges.forEach { edge ->
            attraction.apply(nodes[nodeId2Index[edge.node1]!!], nodes[nodeId2Index[edge.node2]!!], 1.0)
        }
    }else if(edgeWeightInfluence == 1.0) {
        edges.forEach { edge ->
            attraction.apply(nodes[nodeId2Index[edge.node1]!!], nodes[nodeId2Index[edge.node2]!!], edge.weight)
        }
    }else {
        edges.forEach { edge ->
            attraction.apply(nodes[nodeId2Index[edge.node1]!!], nodes[nodeId2Index[edge.node2]!!], Math.pow(edge.weight, edgeWeightInfluence))
        }
    }
}

fun getRepulsion(coefficient: Double) = LinRepulsionAntiCollision(coefficient)
fun getStrongGravity(coefficient: Double) = StrongGravity(coefficient)
fun getAttraction(
    coefficient: Double,
    linLogMode: Boolean,
): AttractionForce {
    return if(linLogMode) {
        LogAttractionDegreeDistributedAntiCollision(coefficient)
    }else {
        LinAttractionDegreeDistributedAntiCollision(coefficient)
    }
}

class LogAttractionDegreeDistributedAntiCollision(
    private var coefficient: Double
): AttractionForce(coefficient) {
    override fun apply(node1: Node, node2: Node, edgeWeight: Double) {
        var xDist = node1.x - node2.x
        var yDist = node1.y - node2.y
        var distance = sqrt(xDist * xDist + yDist * yDist) - node1.size - node2.size
        if(distance > 0) {
            var factor = -1 * coefficient * edgeWeight * log(1 + distance, 2.0) / distance / node1.mass
            node1.dx += xDist * factor
            node1.dy += yDist * factor
            node2.dx -= xDist * factor
            node2.dy -= yDist * factor
        }
    }
}

class LinAttractionDegreeDistributedAntiCollision(
    private var coefficient: Double
): AttractionForce(coefficient) {
    override fun apply(node1: Node, node2: Node, edgeWeight: Double) {
        var xDist = node1.x - node2.x
        var yDist = node1.y - node2.y
        var distance = sqrt(xDist.pow(2) + yDist.pow(2)) - node1.size - node2.size
        if(distance > 0) {
            val factor = -1 * coefficient * edgeWeight / node1.mass
            node1.dx += xDist * factor
            node1.dy += yDist * factor
            node2.dx -= xDist * factor
            node2.dy -= yDist * factor
        }
    }
}

class StrongGravity(
    var coefficient: Double
): RepulsionForce(coefficient) {
    override fun applyNodeToNode(node1: Node, node2: Node) { }
    override fun applyApproximation(node: Node, region: Quadtree) { }
    override fun applyGravitation(node: Node, gravity: Double) {
        var xDist = node.x
        var yDist = node.y
        var distance = sqrt(xDist * xDist + yDist * yDist)
        if(distance > 0) {
            var factor = coefficient * node.mass * gravity
            node.dx -= xDist * factor
            node.dy -= yDist * factor
        }
    }
}
class LinRepulsionAntiCollision(
    var coefficient: Double
): RepulsionForce(coefficient) {
    override fun applyNodeToNode(node1: Node, node2: Node) {
        var xDist = node1.x - node2.x
        var yDist = node1.y - node2.y
        var distance = sqrt(xDist * xDist + yDist * yDist) - node1.size - node2.size
        if(distance > 0) {
            var factor = coefficient * node1.mass * node2.mass / distance / distance
            node1.dx += xDist * factor
            node1.dy += yDist * factor
            node2.dx -= xDist * factor
            node2.dy -= yDist * factor
        }
        if(distance < 0) {
            var factor = 100 * coefficient * node1.mass * node2.mass
            node1.dx += xDist * factor
            node1.dy += yDist * factor
            node2.dx -= xDist * factor
            node2.dy -= yDist * factor
        }
    }

    override fun applyApproximation(node: Node, region: Quadtree) {
        var xDist = node.x - region.centerX
        var yDist = node.y - region.centerY
        var distance = sqrt(xDist * xDist + yDist * yDist)
        if(distance > 0) {
            var factor = coefficient * node.mass * region.sumMass / distance / distance
            node.dx += xDist * factor
            node.dy += yDist * factor
        }
        if(distance < 0) {
            var factor = -1 * coefficient * node.mass * region.sumMass / distance
            node.dx += xDist * factor
            node.dy += yDist * factor
        }
    }

    override fun applyGravitation(node: Node, gravity: Double) {
        var xDist = node.x
        var yDist = node.y
        var distance = sqrt(xDist * xDist + yDist * yDist)
        if(distance > 0) {
            var factor = coefficient * node.mass * gravity / distance
            node.dx -= xDist * factor
            node.dy -= yDist * factor
        }
    }

}

abstract class AttractionForce(coefficient: Double) {
    abstract fun apply(node1: Node, node2: Node, edgeWeight: Double)
}

abstract class RepulsionForce(coefficient: Double) {
    abstract fun applyNodeToNode(node1: Node, node2: Node)
    abstract fun applyApproximation(node: Node, region: Quadtree)
    abstract fun applyGravitation(node: Node, gravity: Double)
}

