package com.oshicue.kit.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Reads the bundled template documents shipped with the app. */
fun loadTemplateText(context: Context, name: String): String = try {
    context.assets.open("templates/$name").bufferedReader(Charsets.UTF_8).use { it.readText() }
} catch (_: Exception) {
    ""
}

/** Reads every selected document as UTF-8 text. */
fun readDocumentText(context: Context, uri: Uri): String {
    context.contentResolver.openInputStream(uri)?.use { stream ->
        return stream.readBytes().toString(Charsets.UTF_8)
    }
    throw IllegalStateException("Unable to read the selected file.")
}

/**
 * An open-document launcher. Content is delivered through a callback, mirroring the
 * iOS fileImporter that is allowed to select several JSON files at once.
 */
@Composable
fun rememberFilePicker(
    multiple: Boolean,
    onLoaded: (List<String>) -> Unit
): () -> Unit {
    val context = LocalContext.current
    if (multiple) {
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->
            if (uris.isNullOrEmpty()) return@rememberLauncherForActivityResult
            try {
                onLoaded(uris.map { readDocumentText(context, it) })
            } catch (failure: Exception) {
                onLoaded(emptyList())
            }
        }
        return remember(launcher) {
            { launcher.launch(arrayOf("application/json", "text/plain", "text/*", "application/octet-stream")) }
        }
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            onLoaded(listOf(readDocumentText(context, uri)))
        } catch (_: Exception) {
            onLoaded(emptyList())
        }
    }
    return remember(launcher) {
        { launcher.launch(arrayOf("application/json", "text/plain", "text/*", "application/octet-stream")) }
    }
}

/**
 * Creates a document through the system picker. Used by every export action so the
 * user keeps control of the destination, as with the iOS fileExporter.
 */
@Composable
fun rememberTextExporter(onError: (String) -> Unit): (String, String) -> Unit {
    val context = LocalContext.current
    val pending = remember { mutableStateOf<Pair<String, String>?>(null) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val content = pending.value
        pending.value = null
        if (uri == null || content == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(content.second.toByteArray(Charsets.UTF_8))
            } ?: onError("Unable to write the selected file.")
        } catch (failure: Exception) {
            onError(failure.localizedMessage ?: "Unable to write the selected file.")
        }
    }
    return remember(launcher) { { name: String, content: String ->
        pending.value = name to content
        launcher.launch(name)
    } }
}

/** A monospaced JSON editor that never applies smart punctuation. */
@Composable
fun JsonEditorField(
    text: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minHeight: Dp = 220.dp
) {
    val kit = LocalKit.current
    val scroll = rememberScrollState()
    BasicTextField(
        value = text,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(14.dp))
            .background(kit.surface)
            .padding(14.dp),
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = kit.ink
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            autoCorrectEnabled = false,
            capitalization = KeyboardCapitalization.None
        ),
        decorationBox = { inner ->
            Box {
                if (text.isEmpty()) {
                    Text(placeholder, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = kit.muted)
                }
                inner()
            }
        }
    )
}

/** Shares plain text through any installed application. */
fun shareText(context: Context, text: String, subject: String? = null) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        if (subject != null) putExtra(Intent.EXTRA_SUBJECT, subject)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(Intent.createChooser(intent, subject).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        // No sharing application is installed.
    }
}

fun copyToClipboard(context: Context, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    manager.setPrimaryClip(android.content.ClipData.newPlainText("Oshicue Kit", text))
}
