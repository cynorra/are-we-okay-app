package com.cynorra.areweokay.ui.auth

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import com.cynorra.areweokay.R
import com.cynorra.areweokay.data.LocalDataRepository
import com.cynorra.areweokay.theme.OkBeige
import com.cynorra.areweokay.theme.OkBlack
import com.cynorra.areweokay.theme.OkBorder
import com.cynorra.areweokay.theme.OkMuted
import com.cynorra.areweokay.theme.OkOrange
import com.cynorra.areweokay.theme.OkOrangeLight
import com.cynorra.areweokay.theme.OkOrangeShade
import com.cynorra.areweokay.theme.OkSurface
import java.util.UUID

@Composable
fun GoogleIcon(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val sizePx = size.minDimension
        val stroke = sizePx * 0.22f
        val r = (sizePx - stroke) / 2f
        
        // Draw Red segment (top)
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 180f + 40f,
            sweepAngle = 140f,
            useCenter = false,
            style = Stroke(width = stroke)
        )
        // Draw Yellow segment (left)
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 180f - 40f,
            sweepAngle = 80f,
            useCenter = false,
            style = Stroke(width = stroke)
        )
        // Draw Green segment (bottom)
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 40f,
            sweepAngle = 100f,
            useCenter = false,
            style = Stroke(width = stroke)
        )
        // Draw Blue segment (right)
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = 320f,
            sweepAngle = 80f,
            useCenter = false,
            style = Stroke(width = stroke)
        )
        
        // Draw the horizontal bar of G (Blue)
        val barLength = r * 0.95f
        val barHeight = stroke
        drawRect(
            color = Color(0xFF4285F4),
            topLeft = androidx.compose.ui.geometry.Offset(center.x, center.y - barHeight / 2f),
            size = androidx.compose.ui.geometry.Size(barLength, barHeight)
        )
    }
}

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        shape = RoundedCornerShape(27.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, OkBorder),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            GoogleIcon(modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Google ile Devam Et",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = OkBlack
            )
        }
    }
}



@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dataRepository = LocalDataRepository.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var selectedMood by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var showEmailLogin by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    val triggerGoogleSignIn = {
        coroutineScope.launch {
            try {
                val clientId = context.getString(R.string.google_web_client_id)
                if (clientId.isBlank() || clientId.startsWith("your_google_web_client_id")) {
                    errorMessage = "Lütfen strings.xml dosyasında geçerli bir Google Web Client ID tanımlayın."
                    return@launch
                }
                
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )
                
                val credential = result.credential
                if (credential is GoogleIdTokenCredential) {
                    val email = credential.id
                    val name = credential.displayName ?: email.substringBefore("@")
                    
                    val signInResult = dataRepository.signIn(email)
                    val userProfile = if (signInResult.isSuccess) {
                        signInResult.getOrNull()
                    } else {
                        // User does not exist, automatically sign them up
                        val cleanUsername = email.substringBefore("@").replace(".", "_")
                        val signUpResult = dataRepository.signUp(
                            email = email,
                            username = cleanUsername,
                            avatarEmoji = "😎"
                        )
                        signUpResult.getOrNull()
                    }
                    
                    if (userProfile != null) {
                        // Automatically create public checkin if mood button was clicked
                        selectedMood?.let { moodKey ->
                            val moodState = when (moodKey) {
                                "good" -> com.cynorra.areweokay.data.MoodState.good
                                "bad" -> com.cynorra.areweokay.data.MoodState.bad
                                else -> com.cynorra.areweokay.data.MoodState.unsure
                            }
                            dataRepository.createCheckin(
                                mood = moodState,
                                note = "",
                                isPublic = true
                            )
                        }
                        onAuthSuccess()
                    } else {
                        errorMessage = "Kullanıcı profil kaydı oluşturulamadı."
                    }
                } else {
                    errorMessage = "Geçersiz kimlik bilgisi türü alındı."
                }
            } catch (e: GetCredentialCancellationException) {
                // User cancelled, do nothing
            } catch (e: GetCredentialException) {
                errorMessage = "Google girişi başarısız oldu: ${e.localizedMessage}"
            } catch (e: Exception) {
                errorMessage = "Beklenmeyen hata: ${e.localizedMessage}"
            }
        }
    }

    // Premium background gradient
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            OkBeige,
            Color(0xFFFAF2EB),
            OkOrangeLight.copy(alpha = 0.35f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
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
                contentDescription = "Are We Okay",
                tint = OkOrange,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Are We Okay",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OkBlack
            )
        }

        // Hero Content in Premium Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 80.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "İyi Miyiz?",
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OkBlack,
                textAlign = TextAlign.Center,
                lineHeight = 50.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Global bir iyilik hareketi. Her gün check-in yap, nasıl hissettiğini anonim paylaş ve yargılanmadan destek bul.",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = OkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(40.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, OkBorder, RoundedCornerShape(28.dp)),
                colors = CardDefaults.cardColors(containerColor = OkSurface),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!showEmailLogin) {
                        Text(
                            text = "Günün nasıl geçiyor?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = OkBlack
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Mood Buttons (directly triggers Google Sign-in on click)
                        val moodButtons = listOf(
                            Triple("good", "😎", "İyiyiz"),
                            Triple("bad", "😔", "İyi Değiliz"),
                            Triple("unsure", "🤔", "Emin Değilim")
                        )

                        moodButtons.forEach { (moodKey, emoji, label) ->
                            Button(
                                onClick = {
                                    selectedMood = moodKey
                                    triggerGoogleSignIn()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OkBeige.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(28.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, OkBorder)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(text = emoji, fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = label,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OkBlack
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Google Login Button directly
                        GoogleSignInButton(
                            onClick = { 
                                selectedMood = null
                                triggerGoogleSignIn()
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(
                            onClick = { showEmailLogin = true }
                        ) {
                            Text(
                                text = "E-posta ile Giriş Yap",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = OkOrange
                            )
                        }
                    } else {
                        Text(
                            text = "E-posta ile Giriş",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = OkBlack
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("E-posta Adresi") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OkOrange,
                                unfocusedBorderColor = OkBorder,
                                focusedLabelColor = OkOrange
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Şifre") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OkOrange,
                                unfocusedBorderColor = OkBorder,
                                focusedLabelColor = OkOrange
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (emailInput.isNotBlank() && passwordInput.isNotBlank()) {
                                    coroutineScope.launch {
                                        val res = dataRepository.signInWithPassword(emailInput.trim(), passwordInput)
                                        if (res.isSuccess) {
                                            onAuthSuccess()
                                        } else {
                                            errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Giriş başarısız oldu."
                                        }
                                    }
                                } else {
                                    errorMessage = "Lütfen e-posta ve şifrenizi girin."
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OkOrange),
                            shape = RoundedCornerShape(27.dp)
                        ) {
                            Text(
                                text = "Giriş Yap",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = { showEmailLogin = false }
                        ) {
                            Text(
                                text = "Geri Dön",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = OkMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
            
            // Footer Text
            Text(
                text = "Birlikte daha iyiyiz. ❤️",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = OkOrangeShade
            )
        }

        // Error Dialog
        if (errorMessage != null) {
            Dialog(onDismissRequest = { errorMessage = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, OkBorder, RoundedCornerShape(28.dp)),
                    colors = CardDefaults.cardColors(containerColor = OkSurface),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Giriş Hatası",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = OkBlack
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 14.sp,
                            color = OkMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { errorMessage = null },
                            colors = ButtonDefaults.buttonColors(containerColor = OkOrange),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(text = "Kapat", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}


