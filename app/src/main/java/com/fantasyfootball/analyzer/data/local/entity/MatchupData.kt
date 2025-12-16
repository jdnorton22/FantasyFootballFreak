package com.fantasyfootball.analyzer.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing historical matchup data between a player and opponent team.
 * Used for analyzing player performance against specific opponents.
 */
@Entity(
    tableName = "matchup_data",
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
        Index(value = ["opponentTeam"]),
        Index(value = ["season", "week"]),
        Index(value = ["gameDate"])
    ]
)
data class MatchupData(
    @PrimaryKey 
    val id: String,
    val playerId: String,
    val opponentTeam: String,
    val gameDate: Long,
    val fantasyPoints: Double,
    val performanceRating: Double,
    val season: Int,
    val week: Int
)