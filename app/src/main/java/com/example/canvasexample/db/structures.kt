package com.example.canvasexample.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Node")
data class Node(
    @PrimaryKey(autoGenerate = true) var id: Long = -1,
    @ColumnInfo(defaultValue = "0.0")
    var dx: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0")
    var dy: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0")
    var old_dx: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0")
    var old_dy: Double = 0.0,
    @ColumnInfo(defaultValue = "1.0")
    var mass: Double = 1.0,
    @ColumnInfo(defaultValue = "0.0")
    var x: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0")
    var y: Double = 0.0,
    @ColumnInfo(defaultValue = "32.0")
    var size: Double = 32.0,
    @ColumnInfo(defaultValue = "1.0")
    var weight: Double = 1.0,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    var createdTime: String = "",
)

@Entity(tableName = "Edge")
data class Edge(
    @PrimaryKey(autoGenerate = true) var id: Long = -1,
    @ColumnInfo(defaultValue = "-1")
    var node1: Int = 0,
    @ColumnInfo(defaultValue = "-1")
    var node2: Int = 0,
    @ColumnInfo(defaultValue = "10.0")
    var weight: Double = 10.0,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    var createdTime: String = "",
)