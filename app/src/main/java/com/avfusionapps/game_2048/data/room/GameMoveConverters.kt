package com.avfusionapps.game_2048.data.room

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Type converters for Room to handle complex data types like List<List<Int>>
 */
class GameMoveConverters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromGridToString(grid: List<List<Int>>): String {
        return gson.toJson(grid)
    }
    
    @TypeConverter
    fun fromStringToGrid(gridString: String): List<List<Int>> {
        val type = object : TypeToken<List<List<Int>>>() {}.type
        return gson.fromJson(gridString, type)
    }
}