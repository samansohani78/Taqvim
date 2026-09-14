/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

/**
 * Koin bindings of the Tools screen; `:app` provides [ToolsSettingsSource], `kotlin.time.Clock` and optionally a
 * [ToolsBoardStore]. An optional [String] parameter is the converter's initial text.
 */
val toolsFeatureModule: Module =
    module {
        viewModel { parameters ->
            ToolsViewModel(get(), get(), getOrNull() ?: ToolsBoardStore.NONE, parameters.getOrNull<String>())
        }
    }

/**
 * The Tools screen bound to its [ToolsViewModel]; QR codes are shared as PNG images. [converterText] opens the date
 * converter with that text.
 */
@Composable
fun ToolsRoute(
    modifier: Modifier = Modifier,
    converterText: String? = null,
    viewModel: ToolsViewModel = koinViewModel(parameters = { parametersOf(converterText) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val actions =
        remember(viewModel, context, resources) {
            ToolsActions(
                onSelectTab = viewModel::onSelectTab,
                onInputsChange = viewModel::onInputsChange,
                onAddZone = viewModel::onAddZone,
                onRemoveZone = viewModel::onRemoveZone,
                onShareQr = {
                    viewModel.qrToShare()?.let { (text, matrix) ->
                        QrShare.share(context, matrix, text, resources.getString(R.string.tools_qr_share))
                    }
                },
            )
        }
    ToolsScreen(state, actions, modifier)
}

/** Shares a QR code as a PNG in the cache, through the module's `FileProvider`. */
internal object QrShare {
    private const val TARGET_PIXELS = 1_024
    private const val MIN_MODULE_PIXELS = 4
    private const val QUIET_ZONE_MODULES = 4
    private const val PNG_QUALITY = 100
    private const val DIRECTORY = "shared"
    private const val FILE_NAME = "qr.png"
    private const val AUTHORITY_SUFFIX = ".tools.files"
    private const val MIME_TYPE = "image/png"

    /** The authority declared in this module's manifest for the application [context]. */
    fun authority(context: Context): String = context.packageName + AUTHORITY_SUFFIX

    /** [matrix] as black modules on white with a four-module quiet zone, about [TARGET_PIXELS] wide. */
    fun bitmap(matrix: QrMatrix): Bitmap {
        val modules = matrix.size + 2 * QUIET_ZONE_MODULES
        val modulePixels = maxOf(MIN_MODULE_PIXELS, TARGET_PIXELS / modules)
        val side = modules * modulePixels
        val pixels =
            IntArray(side * side) { index ->
                val x = index % side / modulePixels - QUIET_ZONE_MODULES
                val y = index / side / modulePixels - QUIET_ZONE_MODULES
                val dark = x in 0 until matrix.size && y in 0 until matrix.size && matrix.isDark(x, y)
                if (dark) Color.BLACK else Color.WHITE
            }
        return Bitmap.createBitmap(pixels, side, side, Bitmap.Config.ARGB_8888)
    }

    /** Opens the share sheet with [matrix] as an image and [text] alongside; `false` when that failed. */
    fun share(
        context: Context,
        matrix: QrMatrix,
        text: String,
        chooserTitle: String,
    ): Boolean =
        runCatching {
            val directory = File(context.cacheDir, DIRECTORY).apply { mkdirs() }
            val file = File(directory, FILE_NAME)
            file.outputStream().use { bitmap(matrix).compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
            val uri = FileProvider.getUriForFile(context, authority(context), file)
            val send =
                Intent(Intent.ACTION_SEND)
                    .setType(MIME_TYPE)
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .putExtra(Intent.EXTRA_TEXT, text)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
}
