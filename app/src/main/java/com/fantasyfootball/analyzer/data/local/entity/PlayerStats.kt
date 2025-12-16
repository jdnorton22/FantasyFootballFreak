package com.fantasyfootball.analyzer.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing player statistics for a specific game or season.
 * Links to Player entity via foreign key relationship.
 */
@Entity(
    tableName = "player_stats",
    foreignKeys = [
        ForeignKey(
            entity = Player::class,
            parentColumns = ["playerId"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playerId"]),
        Index(value = ["season", "week"]),
        Index(value = ["gameDate"])
    ]
)
data class PlayerStats(
    @PrimaryKey 
    val id: String,
    val playerId: String,
    val season: Int,
    val week: Int?,
    val fantasyPoints: Double,
    val rushingYards: Int,
    val passingYards: Int,
    val receivingYards: Int,
    val touchdowns: Int,
    val gameDate: Long
)