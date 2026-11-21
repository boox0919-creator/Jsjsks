package com.humanatlas.android.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.humanatlas.android.R

data class SelectedStructure(
    val id: String,
    val name: String,
    val system: String
)

enum class AnatomicalSystem(
    val key: String,
    @StringRes val labelRes: Int,
    val accentColor: Color
) {
    SKELETAL("skeletal", R.string.sys_skeletal, Color(0xFFD4C698)),
    MUSCULAR("muscular", R.string.sys_muscular, Color(0xFFA85B50)),
    CARDIAC("cardiac", R.string.sys_cardiac, Color(0xFFB96760)),
    SENSORY("sensory", R.string.sys_sensory, Color(0xFF7FA6B0)),
    ARTERIAL("arterial", R.string.sys_arterial, Color(0xFFC05245)),
    VENOUS("venous", R.string.sys_venous, Color(0xFF527C9F)),
    NERVOUS("nervous", R.string.sys_nervous, Color(0xFFC9A045)),
    RESPIRATORY("respiratory", R.string.sys_respiratory, Color(0xFFB98991)),
    DIGESTIVE("digestive", R.string.sys_digestive, Color(0xFFB8916B)),
    URINARY("urinary", R.string.sys_urinary, Color(0xFFB47961)),
    LYMPHATIC("lymphatic", R.string.sys_lymphatic, Color(0xFF7B9870)),
    ENDOCRINE("endocrine", R.string.sys_endocrine, Color(0xFFB88C86)),
    REPRODUCTIVE("reproductive", R.string.sys_reproductive, Color(0xFFAC8D85)),
    INTEGUMENTARY("integumentary", R.string.sys_integumentary, Color(0xFFAA8B6E)),
    CONNECTIVE("connective", R.string.sys_connective, Color(0xFF7E9C92)),
    UNKNOWN("unknown", R.string.sys_unknown, Color(0xFF458A85));

    companion object {
        fun fromKey(key: String?): AnatomicalSystem =
            entries.firstOrNull { it.key.equals(key?.trim(), ignoreCase = true) } ?: UNKNOWN
    }
}
