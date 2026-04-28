package com.example.scheduleiseu.data.remote.datasource.impl

import com.example.scheduleiseu.data.remote.datasource.ProfileRemoteDataSource
import com.example.scheduleiseu.data.remote.model.ProfileData
import com.example.scheduleiseu.data.remote.parser.BsuParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileRemoteDataSourceImpl(
    private val parser: BsuParser
) : ProfileRemoteDataSource {

    override suspend fun getProfile(): ProfileData = withContext(Dispatchers.IO) {
        val cabinetHtml = parser.getPage(BsuParser.CABINET_URL)

        val progressHtml = runCatching {
            parser.getPage(BsuParser.STUD_PROGRESS_URL)
        }.getOrNull()

        val photoBytes = runCatching {
            parser.loadPhotoImage()
        }.getOrNull()

        parser.parseProfileData(
            cabinetHtml = cabinetHtml,
            progressHtml = progressHtml,
            photoBytes = photoBytes
        )
    }
}
