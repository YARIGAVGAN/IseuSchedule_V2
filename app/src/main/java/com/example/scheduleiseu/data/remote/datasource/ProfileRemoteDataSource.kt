package com.example.scheduleiseu.data.remote.datasource

import com.example.scheduleiseu.data.remote.model.ProfileData

interface ProfileRemoteDataSource {
    suspend fun getProfile(): ProfileData
}
