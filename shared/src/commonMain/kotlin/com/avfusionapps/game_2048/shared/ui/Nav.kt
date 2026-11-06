package com.avfusionapps.game_2048.shared.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Replaces the Android NavController in shared code. Routes keep the Android names:
 * "main", "game?resume=true", "game?resume=false", "timeAttack", "dropMerge", "achievements", "profile".
 * `navController.navigate(x)` → `Nav.navigate(x)`, `navController.popBackStack()` → `Nav.back()`.
 */
object Nav {
    private val stack = mutableStateListOf("main")
    var route by mutableStateOf("main"); private set
    fun navigate(r: String) { stack.add(r); route = r }
    fun back(): Boolean { if (stack.size <= 1) return false; stack.removeAt(stack.lastIndex); route = stack.last(); return true }
    fun home() { stack.clear(); stack.add("main"); route = "main" }
}
