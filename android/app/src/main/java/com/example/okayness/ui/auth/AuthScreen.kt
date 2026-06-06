package com.example.okayness.ui.auth

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.okayness.data.LocalDataRepository
import com.example.okayness.theme.OkBeige
import com.example.okayness.theme.OkBlack
import com.example.okayness.theme.OkBorder
import com.example.okayness.theme.OkMuted
import com.example.okayness.theme.OkOrange
import com.example.okayness.theme.OkOrangeLight
import com.example.okayness.theme.OkOrangeShade
import com.example.okayness.theme.OkSurface

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dataRepository = LocalDataRepository.current
    var selectedMood by remember { mutableStateOf<String?>(null) }
    var showModal by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OkBeige)
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        // App Name Header
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Okayness",
                tint = OkOrange,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Okayness",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = OkBlack
            )
        }

        // Hero Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 100.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Are We Okay?",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OkBlack,
                textAlign = TextAlign.Center,
                lineHeight = 48.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "A global wellbeing movement. Check in daily, share how you feel anonymously, and support each other with zero judgment.",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = OkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(48.dp))

            // Mood Buttons
            val moodButtons = listOf(
                Triple("good", "😎", "We're Good"),
                Triple("bad", "😔", "We're Not"),
                Triple("unsure", "🤔", "Not Sure")
            )

            moodButtons.forEach { (moodKey, emoji, label) ->
                Button(
                    onClick = {
                        selectedMood = moodKey
                        showModal = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OkSurface),
                    shape = RoundedCornerShape(32.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OkBorder)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = emoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = OkBlack
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))
            
            // Footer Text
            Text(
                text = "Together, we're better. ❤️",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = OkOrangeShade
            )
        }

        // Dialog Modal for Register/Login
        if (showModal && selectedMood != null) {
            AuthModal(
                mood = selectedMood!!,
                onDismiss = { showModal = false },
                onAuthSuccess = onAuthSuccess
            )
        }
    }
}

@Composable
fun AuthModal(
    mood: String,
    onDismiss: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val dataRepository = LocalDataRepository.current
    var isSignUp by remember { mutableStateOf(true) }

    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🌙") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val emojis = listOf("🌙", "😎", "😔", "🤔", "💻", "🎓", "⛵", "🌱", "🍕", "🐱", "🐶", "🦊")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = OkSurface),
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, OkBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header emoji based on mood
                val (modalEmoji, title, desc) = when (mood) {
                    "good" -> Triple("😎", "Wonderful 😊", "So glad you're doing well. 🫂")
                    "bad" -> Triple("😔", "We hear you 💙", "Hard times don't last. 🫂")
                    else -> Triple("🤔", "That's okay too 🤗", "Not knowing is also an answer. 🫂")
                }

                Text(text = modalEmoji, fontSize = 48.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = OkBlack,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    fontSize = 14.sp,
                    color = OkMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .background(OkOrangeLight, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Join 2,451 people checked in today",
                        color = OkOrangeShade,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = Color.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMsg = null },
                    label = { Text("Email or Username") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OkOrange,
                        unfocusedBorderColor = OkBorder,
                        focusedLabelColor = OkOrange,
                        unfocusedLabelColor = OkMuted
                    )
                )

                if (isSignUp) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Username
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; errorMsg = null },
                        label = { Text("Display Username") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OkOrange,
                            unfocusedBorderColor = OkBorder,
                            focusedLabelColor = OkOrange,
                            unfocusedLabelColor = OkMuted
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Avatar emoji picker
                    Text(
                        text = "Choose Your Avatar:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OkBlack,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Display 6 emojis in first row, 6 in second
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                emojis.take(6).forEach { emoji ->
                                    val isSelected = selectedEmoji == emoji
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) OkOrangeLight else Color.Transparent)
                                            .border(
                                                1.dp,
                                                if (isSelected) OkOrange else OkBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedEmoji = emoji }
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 18.sp)
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                emojis.drop(6).take(6).forEach { emoji ->
                                    val isSelected = selectedEmoji == emoji
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) OkOrangeLight else Color.Transparent)
                                            .border(
                                                1.dp,
                                                if (isSelected) OkOrange else OkBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedEmoji = emoji }
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (email.isEmpty()) {
                            errorMsg = "Please enter your email or username."
                            return@Button
                        }
                        if (isSignUp && username.isEmpty()) {
                            errorMsg = "Please enter a display username."
                            return@Button
                        }

                        if (isSignUp) {
                            val result = dataRepository.signUp(email, username, selectedEmoji)
                            if (result.isSuccess) {
                                onAuthSuccess()
                                onDismiss()
                            } else {
                                errorMsg = result.exceptionOrNull()?.message ?: "Signup failed."
                            }
                        } else {
                            val result = dataRepository.signIn(email)
                            if (result.isSuccess) {
                                onAuthSuccess()
                                onDismiss()
                            } else {
                                errorMsg = result.exceptionOrNull()?.message ?: "Login failed."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OkBlack),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (isSignUp) "Create Profile & Check-in" else "Sign In & Check-in",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle Mode Button
                TextButton(
                    onClick = {
                        isSignUp = !isSignUp
                        errorMsg = null
                    }
                ) {
                    Text(
                        text = if (isSignUp) "Already have an account? Sign In" else "New to Okayness? Create Account",
                        color = OkOrangeShade,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
