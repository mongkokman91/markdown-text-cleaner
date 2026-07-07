package com.example.markdowntextcleaner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.markdowntextcleaner.ui.theme.MarkdownTextCleanerTheme
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class MainActivity : ComponentActivity() {

    private var cleanedText by mutableStateOf("")

    // Activity Result Launcher for SAF Create Document
    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri: Uri? ->
        if (uri != null) {
            saveTextToUri(uri)
        } else {
            Toast.makeText(this, "Save cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming share intent
        handleIntent(intent)

        setContent {
            MarkdownTextCleanerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text("Markdown Text Cleaner") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    },
                    floatingActionButton = {
                        ExtendedFloatingActionButton(
                            onClick = {
                                if (cleanedText.isNotEmpty()) {
                                    createDocumentLauncher.launch("Cleaned_AI_Prompt.md")
                                } else {
                                    Toast.makeText(this, "No text to save", Toast.LENGTH_SHORT).show()
                                }
                            },
                            icon = { Icon(painterResource(id = R.drawable.ic_save), contentDescription = "Save as .md") },
                            text = { Text("Save as .md") },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                ) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        cleanedText = cleanedText,
                        onTextChanged = { cleanedText = it },
                        onShareClicked = { shareCleanedText() },
                        onCopyClicked = { copyToClipboard(cleanedText, showToast = true) }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                cleanedText = cleanMarkdownText(sharedText)
                // Automatically copy the cleaned text to the clipboard as soon as it arrives.
                copyToClipboard(cleanedText, showToast = true)
            }
        }
    }

    private fun copyToClipboard(text: String, showToast: Boolean = true) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Cleaned Text", text)
        clipboard.setPrimaryClip(clip)
        if (showToast) {
            Toast.makeText(this, "Copied cleaned text to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cleanMarkdownText(text: String): String {
        // a) Replace all line breaks, newlines (\n), and carriage returns (\r) with a single space.
        var result = text.replace(Regex("[\\r\\n]+"), " ")
        // b) Replace all double/multiple spaces, tabs, and excess whitespace with a single space.
        result = result.replace(Regex("\\s+"), " ")
        // c) Trim any trailing or leading whitespace from the start and end of the document.
        return result.trim()
    }

    private fun shareCleanedText() {
        if (cleanedText.isEmpty()) {
            Toast.makeText(this, "Nothing to share", Toast.LENGTH_SHORT).show()
            return
        }
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, cleanedText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Cleaned Text")
        startActivity(shareIntent)
    }

    private fun saveTextToUri(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                    writer.write(cleanedText)
                }
            }
            Toast.makeText(this, "Saved successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    cleanedText: String,
    onTextChanged: (String) -> Unit,
    onShareClicked: () -> Unit,
    onCopyClicked: () -> Unit
) {
    val wordCount = if (cleanedText.isEmpty()) 0 else cleanedText.split(Regex("\\s+")).size
    val charCount = cleanedText.length
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Indicator
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Text Processed: $wordCount words | $charCount characters",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Action controls if they want to paste manually
        var manualInput by remember { mutableStateOf("") }
        var isManualMode by remember { mutableStateOf(false) }

        if (cleanedText.isEmpty() && !isManualMode) {
            // Empty state helper
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "No shared text received yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Button(onClick = { isManualMode = true }) {
                        Text("Paste Text Manually")
                    }
                }
            }
        } else if (isManualMode && cleanedText.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { manualInput = it },
                    label = { Text("Paste Markdown or text to clean") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val cleanResult = manualInput.replace(Regex("[\\r\\n]+"), " ").replace(Regex("\\s+"), " ").trim()
                            onTextChanged(cleanResult)
                            isManualMode = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clean Text")
                    }
                    OutlinedButton(
                        onClick = { isManualMode = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        } else {
            // Main Cleaned Text Viewer
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Cleaned Text Preview:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = onCopyClicked) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Text")
                    }
                    IconButton(onClick = onShareClicked) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share Text")
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    shape = MaterialTheme.shapes.medium,
                    border = CardDefaults.outlinedCardBorder(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    SelectionContainer {
                        Text(
                            text = cleanedText,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            // Reset
                            onTextChanged("")
                            manualInput = ""
                            isManualMode = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Clear")
                    }
                }
            }
        }
    }
}
