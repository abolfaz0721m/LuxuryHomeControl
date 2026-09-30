package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.BotFileItem
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.BotViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FilesScreen(
    viewModel: BotViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val files by viewModel.files.collectAsState()
    val editingFile by viewModel.selectedFileForEdit.collectAsState()
    val editingContent by viewModel.editingContent.collectAsState()

    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var deleteCandidate by remember { mutableStateOf<BotFileItem?>(null) }

    // SAF Document Picker (supports multiple files)
    val safPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                viewModel.importFile(uri) { success, name ->
                    if (success) {
                        Toast.makeText(context, "فایل '$name' وارد شد", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Top Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "مدیریت فایل‌های پایتون و کانفیگ",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = NeonGreen
                )
                Text(
                    text = "فایل‌های ربات در حافظه امن داخلی اپ ذخیره می‌شوند. می‌توانید فایل‌های .py، .json و .env را از گوشی ایمپورت کنید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            safPickerLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF00363D))
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("وارد کردن فایل (SAF)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { showNewFileDialog = true },
                        modifier = Modifier.weight(0.7f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("فایل جدید", fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Files List Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "فایل‌های موجود در دایرکتوری ربات (${files.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // File List
        val sdf = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(files, key = { it.name }) { file ->
                FileItemCard(
                    file = file,
                    dateFormatted = sdf.format(Date(file.lastModified)),
                    onEditClick = { viewModel.openFileForEdit(file) },
                    onSetMainClick = { viewModel.setAsMainFile(file.name) },
                    onDeleteClick = { deleteCandidate = file }
                )
            }
        }
    }

    // Built-in Code Editor Dialog
    if (editingFile != null) {
        CodeEditorDialog(
            file = editingFile!!,
            content = editingContent,
            onContentChange = { viewModel.updateEditingContent(it) },
            onSave = {
                viewModel.saveEditingContent {
                    Toast.makeText(context, "تغییرات ذخیره شد", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { viewModel.closeEditor() }
        )
    }

    // New File Dialog
    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("ساخت فایل جدید", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("نام فایل را همراه با پسوند وارد کنید (مثال: sub_bot.py یا config.json):", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        singleLine = true,
                        placeholder = { Text("custom_script.py") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            viewModel.createNewFile(newFileName.trim(), "# Custom Script\n") { ok ->
                                if (ok) {
                                    Toast.makeText(context, "فایل ایجاد شد", Toast.LENGTH_SHORT).show()
                                    showNewFileDialog = false
                                    newFileName = ""
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00391A))
                ) {
                    Text("ایجاد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text("انصراف", color = TextSecondary)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (deleteCandidate != null) {
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("حذف فایل", fontWeight = FontWeight.Bold, color = NeonRed) },
            text = {
                Text("آیا از حذف دائمی فایل '${deleteCandidate!!.name}' اطمینان دارید؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteCandidate?.let { viewModel.deleteFile(it.name) }
                        deleteCandidate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed, contentColor = Color.White)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("انصراف", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun FileItemCard(
    file: BotFileItem,
    dateFormatted: String,
    onEditClick: () -> Unit,
    onSetMainClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (file.isMain) Color(0xFF13222B) else DarkSurfaceElevated
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (file.isMain) NeonCyan.copy(alpha = 0.6f) else DarkOutline
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    val icon = when (file.extension) {
                        "py" -> Icons.Default.Code
                        "json" -> Icons.Default.Description
                        else -> Icons.Default.Description
                    }
                    val iconColor = when (file.extension) {
                        "py" -> NeonGreen
                        "json" -> NeonAmber
                        else -> NeonCyan
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(iconColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                            color = TextPrimary
                        )
                        val sizeKb = (file.sizeBytes / 1024.0)
                        Text(
                            text = "%.1f KB • %s".format(sizeKb, dateFormatted),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextTertiary
                        )
                    }
                }

                if (file.isMain) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonCyan.copy(alpha = 0.2f))
                            .border(1.dp, NeonCyan, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("MAIN SCRIPT", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = NeonCyan)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!file.isMain && file.extension == "py") {
                    TextButton(onClick = onSetMainClick) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonAmber)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تنظیم به عنوان فایل اصلی", style = MaterialTheme.typography.labelSmall, color = NeonAmber)
                    }
                }

                IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = NeonGreen, modifier = Modifier.size(18.dp))
                }

                if (file.name != "hrbot_full-31.py") {
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = NeonRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CodeEditorDialog(
    file: BotFileItem,
    content: String,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = TerminalBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Editor Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "بستن", tint = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                            color = NeonGreen
                        )
                    }

                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00391A))
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ذخیره تغییرات", fontWeight = FontWeight.Bold)
                    }
                }

                // Editor Body (LTR Monospace)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = onContentChange,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = TerminalBg,
                            unfocusedContainerColor = TerminalBg
                        )
                    )
                }
            }
        }
    }
}
