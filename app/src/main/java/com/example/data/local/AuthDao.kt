package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AuthCredentials

@Dao
interface AuthDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredentials(credentials: AuthCredentials)

    @Query("SELECT * FROM auth_credentials WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getCredentialsByEmail(email: String): AuthCredentials?

    @Query("SELECT * FROM auth_credentials WHERE user_id = :userId LIMIT 1")
    suspend fun getCredentialsByUserId(userId: String): AuthCredentials?

    @Query("UPDATE auth_credentials SET password_hash = :newHash, salt = :newSalt, updated_at = :updatedAt, reset_token = NULL WHERE LOWER(email) = LOWER(:email)")
    suspend fun updatePassword(
        email: String,
        newHash: String,
        newSalt: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE auth_credentials SET reset_token = :token, updated_at = :updatedAt WHERE LOWER(email) = LOWER(:email)")
    suspend fun updateResetToken(
        email: String,
        token: String?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE auth_credentials SET reset_token = :token WHERE LOWER(email) = LOWER(:email)")
    suspend fun setResetToken(email: String, token: String?)

    @Query("DELETE FROM auth_credentials WHERE user_id = :userId")
    suspend fun deleteCredentials(userId: String)
}
