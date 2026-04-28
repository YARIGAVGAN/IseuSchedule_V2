package com.example.scheduleiseu.domain.core.repository

interface BootstrapRepository {
    suspend fun isStudentBootstrapCompleted(accountKey: String): Boolean
    suspend fun markStudentBootstrapCompleted(accountKey: String)
    suspend fun resetStudentBootstrap()
}