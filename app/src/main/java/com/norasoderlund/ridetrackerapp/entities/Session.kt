package com.norasoderlund.ridetrackerapp.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String
);