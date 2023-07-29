package com.norasoderlund.ridetrackerapp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.norasoderlund.ridetrackerapp.entities.Session
import com.norasoderlund.ridetrackerapp.entities.SessionLocation

@Dao
interface SessionDao {
    @Insert
    fun createSession(session: Session);

    @Query("SELECT * FROM sessions ORDER BY timestamp ASC")
    fun getSessions(): List<Session>;

    @Query("SELECT * FROM session_locations WHERE session = :sessionId ORDER BY timestamp ASC")
    fun getSessionLocations(sessionId: String): List<SessionLocation>;

    @Query("SELECT * FROM sessions ORDER BY timestamp DESC LIMIT 1")
    fun getLastSession(): List<Session>;

    @Query("SELECT * FROM session_locations ORDER BY timestamp ASC")
    fun getLocations(): List<SessionLocation>;

    @Insert(SessionLocation::class)
    fun addLocation(location: SessionLocation);

    @Query("DELETE FROM session_locations")
    fun clear();

    @Query("DELETE FROM sessions WHERE (SELECT COUNT(*) FROM session_locations WHERE session = sessions.id) < 2")
    fun clearEmptySessions();
}