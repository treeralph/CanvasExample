package com.example.canvasexample.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface NodeDao {
    @Query("INSERT INTO Node (x, y, imgUri, linkUrl, content, description, folder) " +
            "VALUES (:x, :y, :imgUri, :linkUrl, :content, :description, :folder)")
    fun insertNode(
        x: Double,
        y: Double,
        imgUri: String = "",
        linkUrl: String = "",
        content: String = "",
        description: String = "",
        folder: Long = -1
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
    @Query("SELECT * FROM Node WHERE id = :id")
    fun getNodeById(id: Long): Node
    @Query("SELECT * FROM Node WHERE folder = :folder")
    fun getNodesByFolder(folder: Long): List<Node>
}