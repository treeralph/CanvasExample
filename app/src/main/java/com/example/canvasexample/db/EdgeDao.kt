package com.example.canvasexample.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface EdgeDao {
    @Query("INSERT INTO Edge (node1, node2) VALUES (:node1, :node2)")
    fun insertEdge(node1: Long, node2: Long): Long
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
}