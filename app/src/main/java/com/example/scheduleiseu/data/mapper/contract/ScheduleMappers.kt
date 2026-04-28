package com.example.scheduleiseu.data.mapper.contract

import com.example.scheduleiseu.data.remote.model.CurrentWeekInfo
import com.example.scheduleiseu.data.remote.model.TeacherTimeTableData
import com.example.scheduleiseu.data.remote.model.TimeTableData
import com.example.scheduleiseu.domain.core.model.ScheduleContext
import com.example.scheduleiseu.domain.core.model.ScheduleWeek
import com.example.scheduleiseu.domain.core.model.WeekInfo

fun interface CurrentWeekInfoToWeekInfoMapper : DataToDomainMapper<CurrentWeekInfo, WeekInfo>

fun interface StudentTimeTableDataToScheduleContextMapper : DataToDomainMapper<TimeTableData, ScheduleContext>

fun interface TeacherTimeTableDataToScheduleContextMapper : DataToDomainMapper<TeacherTimeTableData, ScheduleContext>

fun interface StudentTimeTableDataToScheduleWeekMapper : DataToDomainMapper<TimeTableData, ScheduleWeek>

fun interface TeacherTimeTableDataToScheduleWeekMapper : DataToDomainMapper<TeacherTimeTableData, ScheduleWeek>
