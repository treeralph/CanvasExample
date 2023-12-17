package com.example.canvasexample.algorithm

import com.example.canvasexample.db.Node
import kotlin.math.max
import kotlin.math.sqrt

class Quadtree(
    var nodes: MutableList<Node> = mutableListOf(),
) {
    var sumMass = 0.0
    var centerX = 0.0
    var centerY = 0.0
    var size = Double.MIN_VALUE

    var NWTree: Quadtree? = null
    var NETree: Quadtree? = null
    var SWTree: Quadtree? = null
    var SETree: Quadtree? = null

    init {
        if(nodes.size > 0) {
            var sumX = 0.0
            var sumY = 0.0
            nodes.forEach { node ->
                sumMass += node.mass
                sumX += node.x * node.mass
                sumY += node.y * node.mass
            }
            centerX = sumX / sumMass
            centerY = sumY / sumMass

            nodes.forEach { node ->
                var distance = sqrt(
                    (node.x - centerX) * (node.x - centerX) + (node.y - centerY) * (node.y - centerY)
                )
                size = max(size, 2 * distance)
            }
        }
    }

    fun build() {
        if(nodes.size > 1) {

            var NW = mutableListOf<Node>()
            var NE = mutableListOf<Node>()
            var SW = mutableListOf<Node>()
            var SE = mutableListOf<Node>()

            nodes.forEach { node ->
                if(node.y > centerY) {
                    if(node.x > centerX) NE.add(node)
                    else NW.add(node)
                }else {
                    if(node.x > centerX) SW.add(node)
                    else SE.add(node)
                }
            }

            NWTree = Quadtree(NW)
            NETree = Quadtree(NE)
            SWTree = Quadtree(SW)
            SETree = Quadtree(SE)

            listOf(NWTree, NETree, SWTree, SETree).forEach { quadtree->
                quadtree?.build()
            }
        }
    }

    fun applyForce(
        node: Node,
        force: RepulsionForce,
        theta: Double
    ) {
        if(nodes.size > 1) {
            var distance = sqrt((node.x - centerX) * (node.x - centerX) + (node.y - centerY) * (node.y - centerY))
            if(distance * theta > size) {
                force.applyApproximation(node, this)
            }else {
                listOf(NWTree, NETree, SWTree, SETree).forEach { quadtree->
                    quadtree?.applyForce(node, force, theta)
                }
            }
        }
        if(nodes.size == 1) {
            force.applyNodeToNode(node, nodes[0])
        }
    }
}