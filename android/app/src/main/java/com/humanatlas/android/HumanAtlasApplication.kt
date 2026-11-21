package com.humanatlas.android

import android.app.Application
import com.humanatlas.android.data.local.AtlasDatabase
import com.humanatlas.android.data.repository.StudyRepository

class HumanAtlasApplication : Application() {

    val database: AtlasDatabase by lazy {
        AtlasDatabase.getInstance(this)
    }

    val studyRepository: StudyRepository by lazy {
        StudyRepository(database.studyEntryDao())
    }
}
