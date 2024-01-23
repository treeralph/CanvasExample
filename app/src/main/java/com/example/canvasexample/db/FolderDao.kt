package com.example.canvasexample.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface FolderDao {
    @Query("INSERT INTO Folder(folderName, folderInfo) VALUES (:folderName, :folderInfo)")
    fun insertFolder(folderName: String, folderInfo: String = ""): Long
    @Insert
    fun insertFolders(folders: List<Folder>)
    @Update
    fun updateFolders(folders: List<Folder>)
    @Delete
    fun deleteFolders(folders: List<Folder>)
    @Query("SELECT * FROM Folder")
    fun getAllFolders(): List<Folder>
    @Query("SELECT * FROM Folder WHERE id = :id")
    fun getFolderById(id: Long): Folder
}