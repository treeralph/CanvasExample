package com.example.canvasexample.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface EdgeDao {
    @Insert
    fun insertEdges(vararg edges: Edge)
    @Update
    fun updateEdges(vararg edges: Edge)
    @Delete
    fun deleteEdges(vararg edges: Edge)
    @Query("SELECT * FROM Edge")
    fun getAllEdges(): List<Edge>
}