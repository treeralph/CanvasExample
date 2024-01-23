package com.example.canvasexample.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface EdgeDao {
    @Query("INSERT INTO Edge (node1, node2, folder) VALUES (:node1, :node2, :folder)")
    fun insertEdge(node1: Long, node2: Long, folder: Long = -1): Long
    @Insert
    fun insertEdges(edges: List<Edge>)
    @Update
    fun updateEdges(edges: List<Edge>)
    @Delete
    fun deleteEdges(edges: List<Edge>)
    @Query("SELECT * FROM Edge")
    fun getAllEdges(): List<Edge>
    @Query("SELECT * FROM Edge WHERE id = :id")
    fun getEdgeById(id: Long): Edge
    @Query("DELETE FROM Edge WHERE node1=:id OR node2=:id")
    fun deleteEdgesByNodeId(id: Long)
    @Query("SELECT * FROM Edge WHERE folder = :folder")
    fun getEdgesByFolder(folder: Long): List<Edge>
    @Query("SELECT * FROM Edge WHERE (node1 = :node1 AND node2 = :node2) OR (node1 = :node2 AND node2 = :node1)")
    fun isEdge(node1: Long, node2: Long): List<Edge>
}