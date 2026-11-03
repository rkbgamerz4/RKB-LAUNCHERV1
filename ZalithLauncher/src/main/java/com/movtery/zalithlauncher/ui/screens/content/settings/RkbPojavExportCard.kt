/*
 * RKB LAUNCHER - export the selected control layout as a PojavLauncher-compatible JSON file. GPLv3.
 */
package com.movtery.zalithlauncher.ui.screens.content.settings

import android.content.res.Resources
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.movtery.layer_controller.layout.loadLayoutFromFile
import com.movtery.layer_controller.pojav.PojavLayoutConverter
import com.movtery.zalithlauncher.game.control.ControlManager
import com.movtery.zalithlauncher.ui.components.TitleAndSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RkbPojavExportCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selected = ControlManager.selectedLayout.collectAsState().value
    var notes by remember { mutableStateOf<List<String>>(emptyList()) }

    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val data = ControlManager.selectedLayout.value
        if (uri != null && data != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val dm = Resources.getSystem().displayMetrics
                        val layout = loadLayoutFromFile(data.file)
                        val out = PojavLayoutConverter.exportToPojav(layout, dm.widthPixels, dm.heightPixels, dm.density)
                        val stream = context.contentResolver.openOutputStream(uri) ?: error("cannot open output")
                        stream.use { it.write(out.json.toByteArray()) }
                        out.warnings
                    }
                }
                result.onSuccess {
                    Toast.makeText(context, "Exported for PojavLauncher", Toast.LENGTH_SHORT).show()
                    notes = it
                }.onFailure {
                    Toast.makeText(context, "Export failed: " + (it.message ?: it.toString()), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TitleAndSummary(
                modifier = Modifier.weight(1f),
                title = "PojavLauncher layouts",
                summary = "Import: use Import in Control Manager and pick a Pojav .json (converted automatically). Export: save the selected layout for PojavLauncher."
            )
            Button(
                enabled = selected != null,
                onClick = { saver.launch("rkb_layout_pojav.json") }
            ) { Text("Export") }
        }
    }

    if (notes.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { notes = emptyList() },
            confirmButton = { TextButton(onClick = { notes = emptyList() }) { Text("OK") } },
            title = { Text("Export notes") },
            text = { Text(notes.joinToString("\n") { "- $it" }) }
        )
    }
}
