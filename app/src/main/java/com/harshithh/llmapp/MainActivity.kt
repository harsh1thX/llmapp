package com.harshithh.llmapp // Your app's package name

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.* // Ensure this is Material 3
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.harshithh.llmapp.ui.theme.OfflineLLMAppTheme // Ensure this path is correct
import kotlinx.coroutines.launch

// Data class to represent a chat message
data class Message(val text: String, val isUser: Boolean)

class MainActivity : ComponentActivity() {

    private lateinit var llmManager: LLMInferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        llmManager = LLMInferenceManager(applicationContext)

        setContent {
            OfflineLLMAppTheme { // Applies your Material 3 theme (including expressive adjustments if made)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background // Uses theme background color
                ) {
                    ChatScreen(llmManager)
                }
            }
        }

        // Initiate model loading when the activity is created
        lifecycleScope.launch {
            llmManager.loadModel()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        llmManager.close() // Release LLM resources when the activity is destroyed
    }
}

/**
 * A Composable function to display a single chat message bubble.
 * MOVED THIS DEFINITION HERE (ABOVE CHATSCREEN) TO ENSURE RESOLUTION.
 */
@Composable
fun MessageBubble(message: Message) {
    val bubbleColor = if (message.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start // Align user messages right, bot messages left
    ) {
        Card( // Using M3 Card
            shape = MaterialTheme.shapes.medium, // Using M3 shapes from your theme
            colors = CardDefaults.cardColors(containerColor = bubbleColor)
        ) {
            Text(
                text = message.text,
                color = textColor,
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.bodyLarge // Using M3 typography
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(llmManager: LLMInferenceManager) {
    val messages = remember { mutableStateListOf<Message>() }
    var inputText by remember { mutableStateOf("") }
    val isModelLoaded by llmManager.isModelLoaded.collectAsState() // Observe if the model is loaded
    val scope = rememberCoroutineScope() // Coroutine scope for asynchronous operations
    var isGenerating by remember { mutableStateOf(false) } // State to track if the model is generating a response

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Offline LLM Chat", style = MaterialTheme.typography.titleLarge) }) // Using M3 typography
        },
        bottomBar = {
            BottomAppBar( // Using M3 BottomAppBar for expressive feel if customized
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField( // Using M3 OutlinedTextField
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text("Enter your message") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        // Input field is enabled only if the model is loaded and not currently generating
                        enabled = isModelLoaded && !isGenerating
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button( // Using M3 Button
                        onClick = {
                            // Process message only if input is not blank, model is loaded, and not generating
                            if (inputText.isNotBlank() && isModelLoaded && !isGenerating) {
                                val userMessage = inputText.trim()
                                messages.add(Message(userMessage, true)) // Add user's message to chat
                                inputText = "" // Clear the input field

                                isGenerating = true // Set generating state
                                scope.launch {
                                    // Add a "Thinking..." message for immediate feedback
                                    val botThinkingMessage = Message("Thinking...", false)
                                    messages.add(botThinkingMessage)

                                    // Collect the response from the LLM
                                    llmManager.generateResponse(userMessage).collect { response ->
                                        // Replace the "Thinking..." message with the actual response
                                        // This assumes `generateResponse` emits one final string.
                                        if (messages.lastOrNull() == botThinkingMessage) {
                                            messages[messages.lastIndex] = Message(response, false)
                                        } else {
                                            messages.add(Message(response, false))
                                        }
                                        isGenerating = false // Generation complete
                                    }
                                }
                            }
                        },
                        // Button enabled only if model is loaded, not generating, and there's text to send
                        enabled = isModelLoaded && !isGenerating && inputText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send message")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Show loading indicator and text if the model is not yet loaded
            if (!isModelLoaded) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) // M3 LinearProgressIndicator
                Text(
                    text = "Loading LLM model...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium, // Using M3 typography
                    color = MaterialTheme.colorScheme.primary // Using M3 color scheme
                )
            }

            // Display chat messages in a scrollable column (newest at bottom)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                reverseLayout = true // Show latest messages at the bottom
            ) {
                items(messages.reversed()) { message ->
                    MessageBubble(message = message) // This should now definitely resolve
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewChatScreen() {
    OfflineLLMAppTheme {
        // This is a placeholder for the preview. The LLM won't actually run here.
        Text("Chat Screen Preview (LLM not active in preview)")
    }
}