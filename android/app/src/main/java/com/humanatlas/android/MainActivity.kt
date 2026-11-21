package com.humanatlas.android

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.humanatlas.android.ui.screens.AtlasRootScreen
import com.humanatlas.android.ui.theme.HumanAtlasTheme
import com.humanatlas.android.ui.viewmodel.AppLanguage
import com.humanatlas.android.ui.viewmodel.AtlasViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: AtlasViewModel by viewModels {
        val app = application as HumanAtlasApplication
        AtlasViewModel.Factory(app.studyRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val darkOverride by viewModel.darkThemeOverride.collectAsStateWithLifecycle()
            val languageOverride by viewModel.languageOverride.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = darkOverride ?: systemDark

            LaunchedEffect(useDarkTheme) {
                val barStyle = if (useDarkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            }

            val baseContext = LocalContext.current
            val baseConfig = LocalConfiguration.current

            val (localizedContext, localizedConfig) = remember(baseContext, baseConfig, languageOverride) {
                if (languageOverride == AppLanguage.SYSTEM) {
                    baseContext to baseConfig
                } else {
                    val locale = Locale.forLanguageTag(languageOverride.tag)
                    val config = Configuration(baseConfig).apply {
                        setLocale(locale)
                        setLayoutDirection(locale)
                    }
                    baseContext.createConfigurationContext(config) to config
                }
            }

            val layoutDirection = when (languageOverride) {
                AppLanguage.ARABIC -> LayoutDirection.Rtl
                AppLanguage.ENGLISH -> LayoutDirection.Ltr
                AppLanguage.SYSTEM -> LocalLayoutDirection.current
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfig,
                LocalLayoutDirection provides layoutDirection
            ) {
                HumanAtlasTheme(darkTheme = useDarkTheme) {
                    AtlasRootScreen(
                        viewModel = viewModel,
                        isDarkTheme = useDarkTheme
                    )
                }
            }
        }
    }
}
