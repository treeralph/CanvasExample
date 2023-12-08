package com.example.canvasexample.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface NodeDao {
    @Query("INSERT INTO Node (x, y) VALUES (:x, :y)")
    fun insertNode(x: Double, y: Double): Long
    @Insert
    fun insertNodes(vararg nodes: Node)
    @Update
    fun updateNodes(vararg nodes: Node)
    @Delete
    fun deleteNodes(vararg nodes: Node)
    @Query("SELECT * FROM Node")
    fun getAllNodes(): MutableList<Node>
    @Query("SELECT * FROM Node ORDER BY createdTime DESC LIMIT 1")
    fun getLatestNode(): Node
    @Query("SELECT * FROM NODE WHERE id = :id")
    fun getNodeById(id: Long): Node

}