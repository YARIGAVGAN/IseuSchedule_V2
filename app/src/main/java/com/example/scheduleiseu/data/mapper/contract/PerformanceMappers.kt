package com.example.scheduleiseu.data.mapper.contract

import com.example.scheduleiseu.data.remote.model.ProgressTableResult
import com.example.scheduleiseu.data.remote.model.SemesterLink
import com.example.scheduleiseu.domain.core.model.SemesterPerformance
import com.example.scheduleiseu.domain.core.model.SemesterReference

fun interface SemesterLinkToSemesterReferenceMapper : DataToDomainMapper<SemesterLink, SemesterReference>

fun interface ProgressTableResultToSemesterPerformanceMapper : DataToDomainMapper<ProgressTableResult, SemesterPerformance>
