package com.avfusionapps.game_2048.shared.game

/** Encodes grids as "2,0,4,0;0,0,0,0;..." for the key-value store. */
object GridCodec {
    fun encode(grid: List<List<Int>>): String = grid.joinToString(";") { it.joinToString(",") }
    fun decode(s: String): List<List<Int>>? = try {
        if (s.isBlank()) null else s.split(";").map { row -> row.split(",").map { it.trim().toInt() } }
            .takeIf { g -> g.isNotEmpty() && g.all { it.size == g.size } }
    } catch (e: Exception) { null }
}

/** One saved Classic move (replaces the Room GameMove row). */
data class GameMove(val id: Long, val grid: List<List<Int>>, val score: Int, val moveNumber: Int)

/** Last few Classic moves, persisted in KeyValueStore (replaces GameMoveRepository). */
class GameMoveRepository {
    private val key = "classic_moves"
    private var moves: MutableList<GameMove> = load()
    private var nextId: Long = (moves.maxOfOrNull { it.id } ?: 0L) + 1

    private fun load(): MutableList<GameMove> {
        val raw = com.avfusionapps.game_2048.shared.platform.KeyValueStore.getString(key) ?: return mutableListOf()
        return raw.split("|").mapNotNull { e ->
            val p = e.split(":")
            if (p.size != 4) null else {
                val g = GridCodec.decode(p[3])
                val id = p[0].toLongOrNull(); val sc = p[1].toIntOrNull(); val mn = p[2].toIntOrNull()
                if (g == null || id == null || sc == null || mn == null) null else GameMove(id, g, sc, mn)
            }
        }.toMutableList()
    }

    private fun persist() {
        com.avfusionapps.game_2048.shared.platform.KeyValueStore.putString(
            key, moves.joinToString("|") { "${it.id}:${it.score}:${it.moveNumber}:${GridCodec.encode(it.grid)}" }
        )
    }

    fun saveMove(grid: List<List<Int>>, score: Int, moveNumber: Int): Long {
        val id = nextId++
        moves.add(GameMove(id, grid.map { it.toList() }, score, moveNumber))
        persist()
        return id
    }
    fun getLastMove(): GameMove? = moves.lastOrNull()
    /** Newest first. */
    fun getLastMoves(n: Int): List<GameMove> = moves.takeLast(n).reversed()
    fun keepOnlyLastMoves(n: Int) { if (moves.size > n) { moves = moves.takeLast(n).toMutableList(); persist() } }
    fun deleteMoveById(id: Long) { moves.removeAll { it.id == id }; persist() }
    fun clearAllMoves() { moves.clear(); persist() }
    fun getMoveCount(): Int = moves.size
}
