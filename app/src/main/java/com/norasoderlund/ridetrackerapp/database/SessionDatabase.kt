package com.norasoderlund.ridetrackerapp.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.norasoderlund.ridetrackerapp.dao.SessionDao
import com.norasoderlund.ridetrackerapp.entities.Session
import com.norasoderlund.ridetrackerapp.entities.SessionLocation

@Database(entities = [ Session::class, SessionLocation::class ], version = 1, exportSchema = false)
abstract class SessionDatabase : RoomDatabase() {
    abstract fun getDao(): SessionDao;
}