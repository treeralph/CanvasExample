package com.example.canvasexample.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface NodeDao {
    @Query("INSERT INTO Node (x, y, imgUri, linkUrl, content, description) " +
            "VALUES (:x, :y, :imgUri, :linkUrl, :content, :description)")
    fun insertNode(
        x: Double,
        y: Double,
        imgUri: String = "",
        linkUrl: String = "",
        content: String = "",
        description: String = ""
    ): Long

    @Insert
    fun insertNodes(nodes: List<Node>)
    @Update
    fun updateNodes(nodes: List<Node>)
    @Delete
    fun deleteNodes(nodes: List<Node>)
    @Query("SELECT * FROM Node")
    fun getAllNodes(): MutableList<Node>
    @Query("SELECT * FROM Node ORDER BY createdTime DESC LIMIT 1")
    fun getLatestNode(): Node
    @Query("SELECT * FROM NODE WHERE id = :id")
    fun getNodeById(id: Long): Node

}