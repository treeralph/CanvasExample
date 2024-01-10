package com.example.canvasexample

import com.example.canvasexample.db.Node

const val MIN_ZOOM = 0.5f
const val MAX_ZOOM = 5.0f
const val NODE_RADIUS = 64.0

const val STROKE_WIDTH = 2f
const val STROKE_ALPHA = 0.7f
const val STROKE_BOX_SIZE_THRESHOLD = 6.0

const val MODE_SWITCHING_SCALE_THRESHOLD = 1.5f

const val NODE_TYPE_NON_LINK = "NODE_TYPE_NON_LINK"
const val NODE_TYPE_LINK = "NODE_TYPE_LINK"

var testNode: Node = Node(
    imgUri = "https://i.ytimg.com/vi/Kdgm-6IShIg/maxresdefault.jpg",
    linkUrl = "https://youtu.be/Kdgm-6IShIg?si=yYg1-jt92707Ym7V",
    content = "눈치게임1",
    description = "가만히 있으면 이렇게 되는 세상😂《런닝맨 / Legend 예능 / RunningMan 》E. 387#런닝맨다시보기​ #런닝맨​ #Runningman"
)