package com.sadvakassov.vlur

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import com.sadvakassov.vlur.internal.PreviewData

@Composable
fun rememberVulkanState(
    isDebugEnabled: Boolean = false
): VlurState {
    val context = LocalContext.current
    val previewData = assemblePreviewData()

    val vulkanState = remember {
        VlurState(
            context = context,
            isDebugEnabled = isDebugEnabled,
            previewData = previewData,
        )
    }

    DisposableEffect(key1 = Unit) {
        onDispose {
            vulkanState.clear()
        }
    }
    return vulkanState
}

@Composable
fun rememberVulkanState(
    context: Context,
    isDebugEnabled: Boolean = false
): VlurState {
    val previewData = assemblePreviewData()

    val vulkanState = remember {
        VlurState(
            context = context,
            isDebugEnabled = isDebugEnabled,
            previewData = previewData
        )
    }

    DisposableEffect(key1 = Unit) {
        onDispose {
            vulkanState.clear()
        }
    }
    return vulkanState
}

@Composable
private fun assemblePreviewData() = PreviewData(
    isInspectionMode = LocalInspectionMode.current,
    fontFamilyResolver = LocalFontFamilyResolver.current,
    density = LocalDensity.current,
    layoutDirection = LocalLayoutDirection.current,
)
