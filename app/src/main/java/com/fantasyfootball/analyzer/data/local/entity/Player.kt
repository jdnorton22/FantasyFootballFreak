package com.fantasyfootball.analyzer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing an NFL player.
 * Stores basic player information and metadata.
 */
@Entity(tableName = "players")
data class Player(
    @PrimaryKey 
    val playerId: String,
    val name: String,
    val position: String,
    val team: String,
    val injuryStatus: String?,
    val isActive: Boolean,
    val lastUpdated: Long
)