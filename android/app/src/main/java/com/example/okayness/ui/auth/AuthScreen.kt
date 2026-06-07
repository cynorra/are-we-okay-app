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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
fun MockGoogleAccountPickerDialog(
    onDismiss: () -> Unit,
    onAccountSelected: (email: String, name: String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDADCE0))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Google Logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    GoogleIcon(modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Google",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF202124)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Okayness uygulamasına devam et",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF202124),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Bir hesap seçin",
                    fontSize = 13.sp,
                    color = Color(0xFF5F6368),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )
                
                // Accounts list
                val accounts = listOf(
                    Pair("Eren Şen", "erens.dev@gmail.com"),
                    Pair("Ahmet Yılmaz", "ahmet.yilmaz@gmail.com"),
                    Pair("Buse Kaya", "buse.kaya@gmail.com")
                )
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { (name, email) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onAccountSelected(email, name) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F0FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name.take(1),
                                    color = Color(0xFF1A73E8),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3C4043)
                                )
                                Text(
                                    text = email,
                                    fontSize = 12.sp,
                                    color = Color(0xFF5F6368)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFF1F3F4))
                        )
                    }
                    
                    // Other option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                val randomId = UUID.randomUUID().toString().substring(0, 4)
                                onAccountSelected("kullanici_$randomId@gmail.com", "Yeni Kullanıcı")
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Başka hesap",
                            tint = Color(0xFF1A73E8),
                            modifier = Modifier.size(24.dp).padding(start = 6.dp)
                        )
                        Spacer(modifier = Modifier.width(18.dp))
                        Text(
                            text = "Başka bir hesap kullan",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A73E8)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Devam etmek için Google; adınızı, e-posta adresinizi, profil resminizi ve dil tercihinizi Okayness ile paylaşır. Kullanmaya başlamadan önce gizlilik politikasını okuyabilirsiniz.",
                    fontSize = 11.sp,
                    color = Color(0xFF5F6368),
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dataRepository = LocalDataRepository.current
    var selectedMood by remember { mutableStateOf<String?>(null) }
    var showGooglePicker by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

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
                contentDescription = "Okayness",
                tint = OkOrange,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Okayness",
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
                text = "Are We Okay?",
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OkBlack,
                textAlign = TextAlign.Center,
                lineHeight = 50.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "A global wellbeing movement. Check in daily, share how you feel anonymously, and support each other with zero judgment.",
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
                    Text(
                        text = "How is your day going?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OkBlack
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Mood Buttons (directly triggers Google Sign-in on click)
                    val moodButtons = listOf(
                        Triple("good", "😎", "We're Good"),
                        Triple("bad", "😔", "We're Not"),
                        Triple("unsure", "🤔", "Not Sure")
                    )

                    moodButtons.forEach { (moodKey, emoji, label) ->
                        Button(
                            onClick = {
                                selectedMood = moodKey
                                showGooglePicker = true
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
                            showGooglePicker = true 
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
            
            // Footer Text
            Text(
                text = "Together, we're better. ❤️",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = OkOrangeShade
            )
        }

        // Google Picker Dialog
        if (showGooglePicker) {
            MockGoogleAccountPickerDialog(
                onDismiss = { showGooglePicker = false },
                onAccountSelected = { email, name ->
                    showGooglePicker = false
                    val result = dataRepository.signIn(email)
                    val userProfile = if (result.isSuccess) {
                        result.getOrNull()
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
                                "good" -> com.example.okayness.data.MoodState.good
                                "bad" -> com.example.okayness.data.MoodState.bad
                                else -> com.example.okayness.data.MoodState.unsure
                            }
                            dataRepository.createCheckin(
                                mood = moodState,
                                note = "",
                                isPublic = true
                            )
                        }
                        onAuthSuccess()
                    }
                }
            )
        }
    }
}


