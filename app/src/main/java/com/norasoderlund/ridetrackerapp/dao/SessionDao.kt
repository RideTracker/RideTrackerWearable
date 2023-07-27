package com.norasoderlund.ridetrackerapp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.norasoderlund.ridetrackerapp.entities.Session
import com.norasoderlund.ridetrackerapp.entities.SessionLocation

@Dao
interface SessionDao {
    @Query("SELECT * FROM session_locations WHERE session = :sessionId")
    fun getLocations(sessionId: String): List<SessionLocation>;

    @Insert(Session::class)
    fun create(session: Session);

    @Insert(SessionLocation::class)
    fun addLocation(location: SessionLocation);
}