package com.humanatlas.android.ui.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.humanatlas.android.R
import com.humanatlas.android.data.local.StudyEntryEntity
import com.humanatlas.android.data.repository.StudyRepository
import com.humanatlas.android.model.SelectedStructure
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppDestination {
    VIEWER,
    LIBRARY
}

enum class LibraryFilter {
    ALL,
    BOOKMARKS,
    NOTES
}

enum class AppLanguage(val tag: String) {
    SYSTEM("system"),
    ENGLISH("en"),
    ARABIC("ar")
}

sealed interface AtlasEvent {
    data class ShowMessage(@StringRes val messageRes: Int) : AtlasEvent
    data class FocusStructureIn3D(val structureId: String) : AtlasEvent
    data object ResetCameraIn3D : AtlasEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class AtlasViewModel(
    private val repository: StudyRepository
) : ViewModel() {

    private val _destination = MutableStateFlow(AppDestination.VIEWER)
    val destination: StateFlow<AppDestination> = _destination.asStateFlow()

    private val _selectedStructure = MutableStateFlow<SelectedStructure?>(null)
    val selectedStructure: StateFlow<SelectedStructure?> = _selectedStructure.asStateFlow()

    private val _isStudySheetOpen = MutableStateFlow(false)
    val isStudySheetOpen: StateFlow<Boolean> = _isStudySheetOpen.asStateFlow()

    private val _isAttributionOpen = MutableStateFlow(false)
    val isAttributionOpen: StateFlow<Boolean> = _isAttributionOpen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _libraryFilter = MutableStateFlow(LibraryFilter.ALL)
    val libraryFilter: StateFlow<LibraryFilter> = _libraryFilter.asStateFlow()

    // null = follow system dark mode; true/false = explicit user toggle
    private val _darkThemeOverride = MutableStateFlow<Boolean?>(null)
    val darkThemeOverride: StateFlow<Boolean?> = _darkThemeOverride.asStateFlow()

    private val _languageOverride = MutableStateFlow(AppLanguage.SYSTEM)
    val languageOverride: StateFlow<AppLanguage> = _languageOverride.asStateFlow()

    private val _events = MutableSharedFlow<AtlasEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<AtlasEvent> = _events.asSharedFlow()

    val savedCount: StateFlow<Int> = repository.savedCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    val selectedStructureEntry: StateFlow<StudyEntryEntity?> = _selectedStructure
        .flatMapLatest { structure ->
            if (structure == null) flowOf(null) else repository.observeEntry(structure.id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val filteredLibraryEntries: StateFlow<List<StudyEntryEntity>> = combine(
        repository.allEntries,
        _searchQuery,
        _libraryFilter
    ) { entries, query, filter ->
        val trimmed = query.trim()
        entries.filter { entry ->
            val matchesFilter = when (filter) {
                LibraryFilter.ALL -> true
                LibraryFilter.BOOKMARKS -> entry.isBookmarked
                LibraryFilter.NOTES -> entry.noteText.isNotBlank()
            }
            val matchesQuery = trimmed.isEmpty() ||
                entry.structureName.contains(trimmed, ignoreCase = true) ||
                entry.structureId.contains(trimmed, ignoreCase = true) ||
                entry.systemId.contains(trimmed, ignoreCase = true) ||
                entry.noteText.contains(trimmed, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun onStructureSelectedFromBridge(structure: SelectedStructure) {
        _selectedStructure.value = structure
    }

    fun onSelectionClearedFromBridge() {
        _selectedStructure.value = null
        _isStudySheetOpen.value = false
    }

    fun selectDestination(next: AppDestination) {
        _destination.value = next
        if (next == AppDestination.LIBRARY) {
            _isStudySheetOpen.value = false
        }
    }

    fun openStudySheet() {
        if (_selectedStructure.value != null) {
            _isStudySheetOpen.value = true
        }
    }

    fun closeStudySheet() {
        _isStudySheetOpen.value = false
    }

    fun openAttribution() {
        _isAttributionOpen.value = true
    }

    fun closeAttribution() {
        _isAttributionOpen.value = false
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectLibraryFilter(filter: LibraryFilter) {
        _libraryFilter.value = filter
    }

    fun toggleDarkTheme(currentlyDark: Boolean) {
        _darkThemeOverride.value = !currentlyDark
    }

    fun cycleLanguage(currentlyRtl: Boolean) {
        _languageOverride.value = if (currentlyRtl) AppLanguage.ENGLISH else AppLanguage.ARABIC
    }

    fun requestResetCamera() {
        _selectedStructure.value = null
        _isStudySheetOpen.value = false
        _events.tryEmit(AtlasEvent.ResetCameraIn3D)
    }

    fun focusFromLibrary(entry: StudyEntryEntity) {
        _selectedStructure.value = SelectedStructure(
            id = entry.structureId,
            name = entry.structureName,
            system = entry.systemId
        )
        _destination.value = AppDestination.VIEWER
        _events.tryEmit(AtlasEvent.FocusStructureIn3D(entry.structureId))
    }

    fun focusSelectedStructure() {
        val current = _selectedStructure.value ?: return
        _isStudySheetOpen.value = false
        _events.tryEmit(AtlasEvent.FocusStructureIn3D(current.id))
    }

    fun toggleBookmarkForSelected() {
        val current = _selectedStructure.value ?: return
        viewModelScope.launch {
            val bookmarked = repository.toggleBookmark(current)
            _events.emit(
                AtlasEvent.ShowMessage(
                    if (bookmarked) R.string.snackbar_bookmark_added else R.string.snackbar_bookmark_removed
                )
            )
        }
    }

    fun saveStudyNote(noteText: String, isBookmarked: Boolean) {
        val current = _selectedStructure.value ?: return
        viewModelScope.launch {
            repository.saveNote(
                structure = current,
                noteText = noteText,
                isBookmarked = isBookmarked
            )
            _isStudySheetOpen.value = false
            _events.emit(AtlasEvent.ShowMessage(R.string.snackbar_note_saved))
        }
    }

    fun toggleBookmarkForEntry(entry: StudyEntryEntity) {
        viewModelScope.launch {
            repository.toggleBookmarkByEntry(entry)
        }
    }

    fun deleteEntry(structureId: String) {
        viewModelScope.launch {
            repository.deleteEntry(structureId)
            if (_selectedStructure.value?.id == structureId) {
                _isStudySheetOpen.value = false
            }
            _events.emit(AtlasEvent.ShowMessage(R.string.snackbar_entry_deleted))
        }
    }

    class Factory(private val repository: StudyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AtlasViewModel::class.java)) {
                return AtlasViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
