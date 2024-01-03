package com.example.geminiapp

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContentProviderCompat.requireContext
import coil.compose.AsyncImage
import com.example.geminiapp.ui.theme.GeminiAppTheme
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    val apiKey = "AIzaSyBdRYBHZwEEQkedE5tWdku_XbKfsXtGsfE"
    private var data = mutableStateOf("")
    private var generateDataClicked = mutableStateOf(false)
    var prompt = ""
    private var image: Bitmap? = null
    private var imageUri = mutableStateOf("")
    private var modelName = mutableStateOf("gemini-pro")
    private var generativeModel: GenerativeModel? = null
    private val pickMedia =
        this.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                imageUri.value = uri.toString()
                image = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(
                            contentResolver,
                            uri
                        )
                    )
                } else {
                    MediaStore.Images.Media.getBitmap(
                        contentResolver, uri
                    )
                }
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GeminiAppTheme {
                var text by remember {
                    mutableStateOf("")
                }
                val trailingIconView = @Composable {
                    IconButton(
                        onClick = {
                            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    ) {
                        Icon(
                            Icons.Default.AccountBox,
                            contentDescription = "",
                            tint = Color.Black
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.background, modifier = Modifier.padding(16.dp)
                ) {
                    Column {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            label = { Text(text = "Message") },
                            trailingIcon = trailingIconView,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                data.value = ""
                                prompt = text
                                generateDataClicked.value = !generateDataClicked.value
                                generateData()
                            },
                            Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(text = "Ask Gemini")
                        }
                        if (imageUri.value != "") {
                            Column {
                                AsyncImage(
                                    model = imageUri.value,
                                    contentDescription = "",
                                    modifier = Modifier.fillMaxHeight(0.4f)
                                )
                                IconButton(
                                    onClick = {
                                        image = null
                                        imageUri.value = ""
                                        modelName.value = ""
                                        data.value = ""
                                    },
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "",
                                        tint = Color.Black
                                    )
                                }
                            }
                        }
                        Greeting(
                            data.value,
                            Modifier,
                            generateDataClicked = generateDataClicked.value
                        )
                    }
                }
            }
        }
    }

    private fun generateData() {
        if (imageUri.value != "") {
            modelName.value = "gemini-pro-vision"
            generativeModel = GenerativeModel(
                modelName = modelName.value,
                apiKey = apiKey
            )
            CoroutineScope(Dispatchers.Main).launch {
                val inputContent = content {
                    image(image!!)
                    text(prompt)
                }

                val response = generativeModel?.generateContent(inputContent)
                data.value = response?.text.toString()
            }
        } else {
            modelName.value = "gemini-pro"
            generativeModel = GenerativeModel(
                modelName = modelName.value,
                apiKey = apiKey
            )
            CoroutineScope(Dispatchers.Main).launch {
                generativeModel?.generateContentStream(prompt)?.collect { chunk ->
                    data.value += chunk.text
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier, generateDataClicked: Boolean) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = modifier.align(
                Alignment.TopStart
            )
        ) {
            if (generateDataClicked && name.isEmpty())
                Box(modifier.size(24.dp)) {
                    CircularProgressIndicator()
                }
            else
                Text(
                    text = name,
                    modifier = modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                )
        }
    }
}