package com.humanatlas.android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.humanatlas.android.R
import com.humanatlas.android.bridge.AnatomyWebController
import com.humanatlas.android.ui.components.AtlasWebView
import com.humanatlas.android.ui.components.AttributionDialog
import com.humanatlas.android.ui.components.FloatingSelectionActionBar
import com.humanatlas.android.ui.components.StudyNoteBottomSheet
import com.humanatlas.android.ui.viewmodel.AppDestination
import com.humanatlas.android.ui.viewmodel.AtlasEvent
import com.humanatlas.android.ui.viewmodel.AtlasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtlasRootScreen(
    viewModel: AtlasViewModel,
    isDarkTheme: Boolean
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val selectedStructure by viewModel.selectedStructure.collectAsStateWithLifecycle()
    val selectedEntry by viewModel.selectedStructureEntry.collectAsStateWithLifecycle()
    val isStudySheetOpen by viewModel.isStudySheetOpen.collectAsStateWithLifecycle()
    val isAttributionOpen by viewModel.isAttributionOpen.collectAsStateWithLifecycle()
    val libraryEntries by viewModel.filteredLibraryEntries.collectAsStateWithLifecycle()
    val savedCount by viewModel.savedCount.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val libraryFilter by viewModel.libraryFilter.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val webController = remember { AnatomyWebController() }
    val context = LocalContext.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    BackHandler(enabled = destination != AppDestination.VIEWER) {
        viewModel.selectDestination(AppDestination.VIEWER)
    }

    LaunchedEffect(viewModel, context) {
        viewModel.events.collect { event ->
            when (event) {
                is AtlasEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(context.getString(event.messageRes))
                }
                is AtlasEvent.FocusStructureIn3D -> {
                    webController.focusStructure(event.structureId)
                }
                AtlasEvent.ResetCameraIn3D -> {
                    webController.resetCamera()
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.app_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (destination == AppDestination.VIEWER) {
                        IconButton(
                            onClick = { viewModel.requestResetCamera() },
                            modifier = Modifier.testTag("top_reset_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.RestartAlt,
                                contentDescription = stringResource(R.string.action_reset_camera)
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.cycleLanguage(currentlyRtl = isRtl) },
                        modifier = Modifier.testTag("top_language_toggle_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Translate,
                                contentDescription = stringResource(R.string.action_toggle_language),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.toggleDarkTheme(currentlyDark = isDarkTheme) },
                        modifier = Modifier.testTag("top_theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = stringResource(R.string.action_toggle_theme)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.openAttribution() },
                        modifier = Modifier.testTag("top_attribution_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.action_attribution)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    selected = destination == AppDestination.VIEWER,
                    onClick = { viewModel.selectDestination(AppDestination.VIEWER) },
                    modifier = Modifier.testTag("nav_viewer_tab"),
                    icon = {
                        Icon(
                            imageVector = if (destination == AppDestination.VIEWER) {
                                Icons.Filled.ViewInAr
                            } else {
                                Icons.Outlined.ViewInAr
                            },
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(R.string.nav_viewer)) }
                )

                NavigationBarItem(
                    selected = destination == AppDestination.LIBRARY,
                    onClick = { viewModel.selectDestination(AppDestination.LIBRARY) },
                    modifier = Modifier.testTag("nav_library_tab"),
                    icon = {
                        BadgedBox(
                            badge = {
                                if (savedCount > 0) {
                                    Badge {
                                        Text(
                                            text = if (savedCount > 99) "99+" else savedCount.toString(),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (destination == AppDestination.LIBRARY) {
                                    Icons.Filled.Bookmarks
                                } else {
                                    Icons.Outlined.Bookmarks
                                },
                                contentDescription = null
                            )
                        }
                    },
                    label = { Text(stringResource(R.string.nav_library)) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Keep the 3D WebGL WebView mounted across tab switches for instant jump-to-structure
            AtlasWebView(
                controller = webController,
                onStructureSelected = viewModel::onStructureSelectedFromBridge,
                onSelectionCleared = viewModel::onSelectionClearedFromBridge,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (destination == AppDestination.VIEWER) 1f else 0f)
            )

            // Floating selection pill triggered via AnatomyBridge when a 3D mesh is selected
            AnimatedVisibility(
                visible = destination == AppDestination.VIEWER && selectedStructure != null,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                selectedStructure?.let { structure ->
                    FloatingSelectionActionBar(
                        structure = structure,
                        savedEntry = selectedEntry,
                        onToggleBookmark = viewModel::toggleBookmarkForSelected,
                        onOpenNotes = viewModel::openStudySheet
                    )
                }
            }

            // Native Saved Library overlay
            if (destination == AppDestination.LIBRARY) {
                LibraryScreen(
                    entries = libraryEntries,
                    totalSavedCount = savedCount,
                    searchQuery = searchQuery,
                    activeFilter = libraryFilter,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onFilterChange = viewModel::selectLibraryFilter,
                    onSelectEntry = viewModel::focusFromLibrary,
                    onToggleBookmark = viewModel::toggleBookmarkForEntry,
                    onDeleteEntry = viewModel::deleteEntry,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (isStudySheetOpen) {
        selectedStructure?.let { structure ->
            StudyNoteBottomSheet(
                structure = structure,
                existingEntry = selectedEntry,
                onDismiss = viewModel::closeStudySheet,
                onSave = viewModel::saveStudyNote,
                onDelete = viewModel::deleteEntry,
                onFocusIn3D = viewModel::focusSelectedStructure
            )
        }
    }

    if (isAttributionOpen) {
        AttributionDialog(onDismiss = viewModel::closeAttribution)
    }
}
