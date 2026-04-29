package com.example.scheduleiseu.data.repository.core

import com.example.scheduleiseu.data.local.cache.ScheduleCacheCodec
import com.example.scheduleiseu.data.local.db.ScheduleCacheDao
import com.example.scheduleiseu.data.local.preferences.AppPreferencesDataSource
import com.example.scheduleiseu.data.mapper.contract.CurrentWeekInfoToWeekInfoMapper
import com.example.scheduleiseu.data.mapper.contract.StudentTimeTableDataToScheduleContextMapper
import com.example.scheduleiseu.data.mapper.contract.StudentTimeTableDataToScheduleWeekMapper
import com.example.scheduleiseu.data.mapper.contract.TeacherTimeTableDataToScheduleContextMapper
import com.example.scheduleiseu.data.mapper.contract.TeacherTimeTableDataToScheduleWeekMapper
import com.example.scheduleiseu.data.remote.datasource.ScheduleRemoteDataSource
import com.example.scheduleiseu.data.remote.model.TimeTableData
import com.example.scheduleiseu.data.session.ScheduleSessionStore
import com.example.scheduleiseu.domain.core.model.ScheduleContext
import com.example.scheduleiseu.domain.core.model.ScheduleWeek
import com.example.scheduleiseu.domain.core.model.WeekInfo
import com.example.scheduleiseu.domain.core.repository.ScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class ScheduleRepositoryImpl(
    private val scheduleRemoteDataSource: ScheduleRemoteDataSource,
    private val sessionStore: ScheduleSessionStore,
    private val studentContextMapper: StudentTimeTableDataToScheduleContextMapper,
    private val teacherContextMapper: TeacherTimeTableDataToScheduleContextMapper,
    private val studentWeekMapper: StudentTimeTableDataToScheduleWeekMapper,
    private val teacherWeekMapper: TeacherTimeTableDataToScheduleWeekMapper,
    private val weekInfoMapper: CurrentWeekInfoToWeekInfoMapper,
    private val scheduleCacheDao: ScheduleCacheDao? = null,
    private val preferencesDataSource: AppPreferencesDataSource? = null
) : ScheduleRepository {

    private val scheduleRefreshMutex = Mutex()

    override fun observeCachedStudentSchedule(
        facultyId: String,
        departmentId: String,
        courseId: String,
        groupId: String,
        week: WeekInfo?
    ): Flow<ScheduleWeek?> {
        val dao = scheduleCacheDao ?: return flowOf(null)
        val ownerId = buildStudentOwnerId(facultyId, departmentId, courseId, groupId)
        val source = week
            ?.let { selectedWeek -> dao.observeByKey(buildCacheKey(ROLE_STUDENT, ownerId, selectedWeek)) }
            ?: dao.observeCurrentOrLatest(ROLE_STUDENT, ownerId)

        return source.map { cached ->
            cached?.let { entity ->
                withContext(Dispatchers.Default) { ScheduleCacheCodec.fromEntity(entity) }
            }
        }
    }

    override fun observeCachedTeacherSchedule(
        teacherId: String,
        week: WeekInfo?
    ): Flow<ScheduleWeek?> {
        val dao = scheduleCacheDao ?: return flowOf(null)
        val ownerId = teacherId.trim()
        val source = week
            ?.let { selectedWeek -> dao.observeByKey(buildCacheKey(ROLE_TEACHER, ownerId, selectedWeek)) }
            ?: dao.observeCurrentOrLatest(ROLE_TEACHER, ownerId)

        return source.map { cached ->
            cached?.let { entity ->
                withContext(Dispatchers.Default) { ScheduleCacheCodec.fromEntity(entity) }
            }
        }
    }

    override fun observeCachedStudentWeekValues(
        facultyId: String,
        departmentId: String,
        courseId: String,
        groupId: String
    ): Flow<Set<String>> {
        val dao = scheduleCacheDao ?: return flowOf(emptySet())
        val ownerId = buildStudentOwnerId(facultyId, departmentId, courseId, groupId)
        return dao.observeCachedWeekValues(ROLE_STUDENT, ownerId).map { it.toSet() }
    }

    override fun observeCachedTeacherWeekValues(teacherId: String): Flow<Set<String>> {
        val dao = scheduleCacheDao ?: return flowOf(emptySet())
        return dao.observeCachedWeekValues(ROLE_TEACHER, teacherId.trim()).map { it.toSet() }
    }

    override fun observeCachedStudentWeeks(
        facultyId: String,
        departmentId: String,
        courseId: String,
        groupId: String
    ): Flow<List<WeekInfo>> {
        val dao = scheduleCacheDao ?: return flowOf(emptyList())
        val ownerId = buildStudentOwnerId(facultyId, departmentId, courseId, groupId)
        return dao.observeWeeksForOwner(ROLE_STUDENT, ownerId).map { entities ->
            withContext(Dispatchers.Default) { toCachedWeekInfos(entities) }
        }
    }

    override fun observeCachedTeacherWeeks(teacherId: String): Flow<List<WeekInfo>> {
        val dao = scheduleCacheDao ?: return flowOf(emptyList())
        return dao.observeWeeksForOwner(ROLE_TEACHER, teacherId.trim()).map { entities ->
            withContext(Dispatchers.Default) { toCachedWeekInfos(entities) }
        }
    }

    override fun clearTeacherSessionState() {
        sessionStore.clearTeacherRawState()
    }
    override suspend fun refreshStudentScheduleContext(): ScheduleContext {
        val data = scheduleRemoteDataSource.getInitialStudentPage()
            ?: throw IllegalStateException("Не удалось загрузить контекст расписания студента")
        sessionStore.saveStudentRawState(data)
        return withContext(Dispatchers.Default) { studentContextMapper.map(data) }
    }

    override suspend fun refreshTeacherScheduleContext(): ScheduleContext {
        val data = scheduleRemoteDataSource.getInitialTeacherPage()
            ?: throw IllegalStateException("Не удалось загрузить контекст расписания преподавателя")
        sessionStore.saveTeacherRawState(data)
        return withContext(Dispatchers.Default) { teacherContextMapper.map(data) }
    }

    override suspend fun refreshStudentCurrentWeek(): WeekInfo? {
        return scheduleRemoteDataSource.getCurrentStudentWeek()?.let(weekInfoMapper::map)
    }

    override suspend fun refreshTeacherCurrentWeek(): WeekInfo? {
        return scheduleRemoteDataSource.getCurrentTeacherWeek()?.let(weekInfoMapper::map)
    }

    override suspend fun updateStudentContextByFaculty(
        context: ScheduleContext,
        facultyId: String
    ): ScheduleContext {
        val currentData = requireStudentStateOrLoad()
        val updated = scheduleRemoteDataSource.updateOnFacultySelect(facultyId, currentData)
            ?: throw IllegalStateException("Не удалось обновить контекст после выбора факультета")
        sessionStore.saveStudentRawState(updated)
        return withContext(Dispatchers.Default) { studentContextMapper.map(updated) }
    }

    override suspend fun updateStudentContextByDepartment(
        context: ScheduleContext,
        facultyId: String,
        departmentId: String
    ): ScheduleContext {
        val currentData = requireStudentStateOrLoad()
        val updated = scheduleRemoteDataSource.updateOnDepartmentSelect(
            facultyId = facultyId,
            departmentId = departmentId,
            currentData = currentData
        ) ?: throw IllegalStateException("Не удалось обновить контекст после выбора формы обучения")
        sessionStore.saveStudentRawState(updated)
        return withContext(Dispatchers.Default) { studentContextMapper.map(updated) }
    }

    override suspend fun updateStudentContextByCourse(
        context: ScheduleContext,
        facultyId: String,
        departmentId: String,
        courseId: String
    ): ScheduleContext {
        val currentData = requireStudentStateOrLoad()
        val updated = scheduleRemoteDataSource.updateOnCourseSelect(
            facultyId = facultyId,
            departmentId = departmentId,
            courseId = courseId,
            currentData = currentData
        ) ?: throw IllegalStateException("Не удалось обновить контекст после выбора курса")
        sessionStore.saveStudentRawState(updated)
        return withContext(Dispatchers.Default) { studentContextMapper.map(updated) }
    }

    override suspend fun refreshStudentSchedule(
        context: ScheduleContext,
        facultyId: String,
        departmentId: String,
        courseId: String,
        groupId: String,
        week: WeekInfo
    ): ScheduleWeek = scheduleRefreshMutex.withLock {
        val ownerId = buildStudentOwnerId(facultyId, departmentId, courseId, groupId)
        val selectedState = selectStudentContext(
            facultyId = facultyId,
            departmentId = departmentId,
            courseId = courseId,
            groupId = groupId
        )

        val weekState = scheduleRemoteDataSource.getStudentTimeTable(
            facultyId = facultyId,
            departmentId = departmentId,
            courseId = courseId,
            groupId = groupId,
            weekDate = week.value,
            currentData = selectedState
        ) ?: throw IllegalStateException("Не удалось загрузить расписание студента")

        sessionStore.saveStudentRawState(weekState)
        val scheduleWeek = withContext(Dispatchers.Default) { studentWeekMapper.map(weekState) }
        val requestedWeek = week.normalizedAgainst(scheduleWeek.week)

        saveScheduleCache(
            role = ROLE_STUDENT,
            ownerId = ownerId,
            requestedWeek = requestedWeek,
            context = context,
            scheduleWeek = scheduleWeek.copy(week = requestedWeek)
        )
        preloadAdditionalStudentPolicyWeeks(
            context = context,
            facultyId = facultyId,
            departmentId = departmentId,
            courseId = courseId,
            groupId = groupId,
            requestedWeek = requestedWeek,
            ownerId = ownerId
        )

        scheduleWeek.copy(week = requestedWeek)
    }

    override suspend fun refreshTeacherSchedule(
        context: ScheduleContext,
        teacherId: String,
        week: WeekInfo
    ): ScheduleWeek = scheduleRefreshMutex.withLock {
        val ownerId = teacherId.trim()
        val currentData = sessionStore.getTeacherRawState()
            ?: scheduleRemoteDataSource.getInitialTeacherPage()
            ?: throw IllegalStateException("Не удалось загрузить контекст расписания преподавателя")

        sessionStore.saveTeacherRawState(currentData)

        val weekState = scheduleRemoteDataSource.getTeacherTimeTable(
            teacherId = teacherId,
            weekDate = week.value,
            currentData = currentData
        ) ?: throw IllegalStateException("Не удалось загрузить расписание преподавателя")

        sessionStore.saveTeacherRawState(weekState)
        val scheduleWeek = withContext(Dispatchers.Default) { teacherWeekMapper.map(weekState) }
        val requestedWeek = week.normalizedAgainst(scheduleWeek.week)

        saveScheduleCache(
            role = ROLE_TEACHER,
            ownerId = ownerId,
            requestedWeek = requestedWeek,
            context = context,
            scheduleWeek = scheduleWeek.copy(week = requestedWeek)
        )
        preloadAdditionalTeacherPolicyWeeks(
            context = context,
            teacherId = teacherId,
            requestedWeek = requestedWeek,
            ownerId = ownerId
        )

        scheduleWeek.copy(week = requestedWeek)
    }

    override suspend fun clearAllCachedScheduleWeeks() {
        scheduleCacheDao?.deleteAllWeeksForRole(ROLE_STUDENT)
    }

    private suspend fun requireStudentStateOrLoad(): TimeTableData {
        return sessionStore.getStudentRawState()
            ?: scheduleRemoteDataSource.getInitialStudentPage()
                ?.also(sessionStore::saveStudentRawState)
            ?: throw IllegalStateException("Не удалось инициализировать состояние расписания студента")
    }

    private suspend fun selectStudentContext(
        facultyId: String,
        departmentId: String,
        courseId: String,
        groupId: String
    ): TimeTableData {
        var current = scheduleRemoteDataSource.getInitialStudentPage()
            ?: throw IllegalStateException("Не удалось загрузить контекст расписания студента")

        if (current.faculties.none { it.value == facultyId }) {
            throw IllegalStateException("Факультет не найден в текущем контексте")
        }
        current = scheduleRemoteDataSource.updateOnFacultySelect(facultyId, current)
            ?: throw IllegalStateException("Не удалось выбрать факультет")

        if (current.departments.none { it.value == departmentId }) {
            throw IllegalStateException("Форма обучения не найдена в текущем контексте")
        }
        current = scheduleRemoteDataSource.updateOnDepartmentSelect(facultyId, departmentId, current)
            ?: throw IllegalStateException("Не удалось выбрать форму обучения")

        if (current.courses.none { it.value == courseId }) {
            throw IllegalStateException("Курс не найден в текущем контексте")
        }
        current = scheduleRemoteDataSource.updateOnCourseSelect(facultyId, departmentId, courseId, current)
            ?: throw IllegalStateException("Не удалось выбрать курс")

        if (current.groups.none { it.value == groupId }) {
            throw IllegalStateException("Группа не найдена в текущем контексте")
        }
        current = scheduleRemoteDataSource.updateOnGroupSelect(facultyId, departmentId, courseId, groupId, current)
            ?: throw IllegalStateException("Не удалось выбрать группу")

        sessionStore.saveStudentRawState(current)
        return current
    }

    private suspend fun saveScheduleCache(
        role: String,
        ownerId: String,
        requestedWeek: WeekInfo,
        context: ScheduleContext,
        scheduleWeek: ScheduleWeek
    ) {
        val dao = scheduleCacheDao ?: return
        val cacheEnabled = role == ROLE_TEACHER || preferencesDataSource?.isCacheCurrentAndPreviousWeekEnabled() == true
        if (!cacheEnabled) {
            dao.deleteAllWeeksForOwner(role = role, ownerId = ownerId)
            return
        }
        val policyWeeks = resolveAllowedWeeks(context = context, selectedWeek = requestedWeek)
        val current = policyWeeks.firstOrNull { it.isCurrent }
        val cacheableWeek = policyWeeks.firstOrNull { it.value == requestedWeek.value }

        if (cacheableWeek != null) {
            dao.upsert(
                withContext(Dispatchers.Default) {
                    ScheduleCacheCodec.toEntity(
                    cacheKey = buildCacheKey(role = role, ownerId = ownerId, week = cacheableWeek),
                    role = role,
                    ownerId = ownerId,
                    week = scheduleWeek.copy(week = cacheableWeek.copy(isCached = true)),
                    isCurrentWeek = current?.value == cacheableWeek.value,
                    isNextWeek = false
                )
                }
            )
        }

        val allowedValues = policyWeeks.map { it.value }.ifEmpty { listOf(requestedWeek.value) }
        dao.deleteWeeksOutsidePolicy(role = role, ownerId = ownerId, allowedWeekValues = allowedValues)
    }

    private suspend fun preloadAdditionalStudentPolicyWeeks(
        context: ScheduleContext,
        facultyId: String,
        departmentId: String,
        courseId: String,
        groupId: String,
        requestedWeek: WeekInfo,
        ownerId: String
    ) {
        val dao = scheduleCacheDao ?: return
        val weeksToPreload = resolveAllowedWeeks(context = context, selectedWeek = requestedWeek)
            .filterNot { it.value == requestedWeek.value }

        weeksToPreload.forEach { targetWeek ->
            val targetKey = buildCacheKey(ROLE_STUDENT, ownerId, targetWeek)
            if (dao.getByKey(targetKey) != null) return@forEach

            runCatching {
                val selectedState = selectStudentContext(facultyId, departmentId, courseId, groupId)
                val targetState = scheduleRemoteDataSource.getStudentTimeTable(
                    facultyId = facultyId,
                    departmentId = departmentId,
                    courseId = courseId,
                    groupId = groupId,
                    weekDate = targetWeek.value,
                    currentData = selectedState
                ) ?: return@runCatching
                val scheduleWeek = withContext(Dispatchers.Default) { studentWeekMapper.map(targetState) }
                val normalizedWeek = targetWeek.normalizedAgainst(scheduleWeek.week)
                saveScheduleCache(
                    role = ROLE_STUDENT,
                    ownerId = ownerId,
                    requestedWeek = normalizedWeek,
                    context = context,
                    scheduleWeek = scheduleWeek.copy(week = normalizedWeek)
                )
            }
        }
    }

    private suspend fun preloadAdditionalTeacherPolicyWeeks(
        context: ScheduleContext,
        teacherId: String,
        requestedWeek: WeekInfo,
        ownerId: String
    ) {
        val dao = scheduleCacheDao ?: return
        val weeksToPreload = resolveAllowedWeeks(context = context, selectedWeek = requestedWeek)
            .filterNot { it.value == requestedWeek.value }

        weeksToPreload.forEach { targetWeek ->
            val targetKey = buildCacheKey(ROLE_TEACHER, ownerId, targetWeek)
            if (dao.getByKey(targetKey) != null) return@forEach

            runCatching {
                val currentData = sessionStore.getTeacherRawState()
                    ?: scheduleRemoteDataSource.getInitialTeacherPage()
                    ?: return@runCatching
                val targetState = scheduleRemoteDataSource.getTeacherTimeTable(
                    teacherId = teacherId,
                    weekDate = targetWeek.value,
                    currentData = currentData
                ) ?: return@runCatching
                val scheduleWeek = withContext(Dispatchers.Default) { teacherWeekMapper.map(targetState) }
                val normalizedWeek = targetWeek.normalizedAgainst(scheduleWeek.week)
                saveScheduleCache(
                    role = ROLE_TEACHER,
                    ownerId = ownerId,
                    requestedWeek = normalizedWeek,
                    context = context,
                    scheduleWeek = scheduleWeek.copy(week = normalizedWeek)
                )
            }
        }
    }

    private fun resolveAllowedWeeks(
        context: ScheduleContext,
        selectedWeek: WeekInfo
    ): List<WeekInfo> {
        val current = resolveCurrentWeek(context = context, selectedWeek = selectedWeek).copy(isCurrent = true)
        val next = resolveNextWeek(context = context, selectedWeek = current)
        return listOfNotNull(current, next).distinctBy { it.value }
    }

    private fun resolveCurrentWeek(context: ScheduleContext, selectedWeek: WeekInfo): WeekInfo {
        return context.currentWeek
            ?: context.weeks.firstOrNull { it.isCurrent }
            ?: context.weeks.firstOrNull { it.value == selectedWeek.value }?.copy(isCurrent = selectedWeek.isCurrent)
            ?: selectedWeek
    }

    private fun resolveNextWeek(context: ScheduleContext, selectedWeek: WeekInfo): WeekInfo? {
        return resolveCalendarNextWeek(
            weeks = context.weeks,
            baseWeek = selectedWeek
        )
    }

    private fun resolveCalendarNextWeek(
        weeks: List<WeekInfo>,
        baseWeek: WeekInfo
    ): WeekInfo? {
        val baseDate = baseWeek.toStartDateOrNull()
        val datedWeeks = weeks.mapNotNull { week ->
            week.toStartDateOrNull()?.let { date -> week to date }
        }

        if (baseDate != null && datedWeeks.isNotEmpty()) {
            val exactDate = baseDate.plusDays(7)
            datedWeeks.firstOrNull { it.second == exactDate }?.let { return it.first }

            return datedWeeks
                .filter { it.second.isAfter(baseDate) }
                .minByOrNull { it.second }
                ?.first
        }

        val rawIndex = weeks.indexOfFirst { it.value == baseWeek.value }
        if (rawIndex < 0) return null
        return weeks.getOrNull(rawIndex - 1)
    }
    private fun WeekInfo.normalizedAgainst(parsedWeek: WeekInfo): WeekInfo {
        return copy(
            title = title.ifBlank { parsedWeek.title },
            isCurrent = isCurrent || parsedWeek.isCurrent,
            isCached = parsedWeek.isCached
        )
    }

    private fun WeekInfo.toStartDateOrNull(): LocalDate? {
        val rawDate = Regex("\\d{2}\\.\\d{2}\\.\\d{4}").find(value)?.value
            ?: Regex("\\d{2}\\.\\d{2}\\.\\d{4}").find(title)?.value
            ?: return null
        return runCatching { LocalDate.parse(rawDate, WEEK_DATE_FORMATTER) }.getOrNull()
    }

    private fun buildStudentOwnerId(
        facultyId: String,
        departmentId: String,
        courseId: String,
        groupId: String
    ): String = listOf(facultyId, departmentId, courseId, groupId).joinToString(separator = ":")

    private fun buildCacheKey(role: String, ownerId: String, week: WeekInfo): String {
        return listOf(role, ownerId, week.value).joinToString(separator = "|")
    }

    private fun toCachedWeekInfos(
        entities: List<com.example.scheduleiseu.data.local.db.CachedScheduleWeekEntity>
    ): List<WeekInfo> {
        return entities.map { entity ->
            WeekInfo(
                value = entity.weekValue,
                title = entity.weekTitle,
                isCurrent = entity.isCurrentWeek,
                isCached = true
            )
        }.distinctBy { it.value }
    }

    private companion object {
        const val ROLE_STUDENT = "student"
        const val ROLE_TEACHER = "teacher"
        val WEEK_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("ru"))
    }
}
