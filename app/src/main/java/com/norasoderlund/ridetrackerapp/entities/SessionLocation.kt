package com.norasoderlund.ridetrackerapp.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_locations")
data class SessionLocation(
    @ColumnInfo(name = "session")
    val session: String,

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "altitude")
    val altitude: Double?,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long
){
    @PrimaryKey(true)
    @ColumnInfo(name = "id")
    var id: Int = 0;
};