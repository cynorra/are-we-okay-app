package com.example.okayness.ui.home

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.okayness.data.Checkin
import com.example.okayness.data.Comment
import com.example.okayness.data.LocalDataRepository
import com.example.okayness.data.MoodState
import com.example.okayness.data.Post
import com.example.okayness.data.UserProfile
import com.example.okayness.theme.OkBeige
import com.example.okayness.theme.OkBlack
import com.example.okayness.theme.OkBorder
import com.example.okayness.theme.OkMuted
import com.example.okayness.theme.OkOrange
import com.example.okayness.theme.OkOrangeLight
import com.example.okayness.theme.OkOrangeShade
import com.example.okayness.theme.OkSurface
import com.example.okayness.theme.OkTeal
import com.example.okayness.theme.OkTealLight

@Composable
fun HomeScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    val navigationItems = listOf(
        Triple("Check-in", Icons.Default.Check, 0),
        Triple("Feed", Icons.Default.Home, 1),
        Triple("Friends", Icons.Default.Person, 2),
        Triple("Insights", Icons.Default.Star, 3),
        Triple("Settings", Icons.Default.Settings, 4)
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = OkSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.border(1.dp, OkBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                navigationItems.forEach { (label, icon, index) ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = { Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(24.dp)) },
                        label = { Text(text = label, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OkOrange,
                            selectedTextColor = OkOrange,
                            indicatorColor = OkOrangeLight,
                            unselectedIconColor = OkMuted,
                            unselectedTextColor = OkMuted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OkBeige)
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> CheckinTab()
                1 -> FeedTab()
                2 -> FriendsTab()
                3 -> InsightsTab()
                4 -> SettingsTab(onSignOut = onSignOut)
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: CHECK-IN
// -------------------------------------------------------------
@Composable
fun CheckinTab() {
    val dataRepository = LocalDataRepository.current
    var mood by remember { mutableStateOf<MoodState?>(null) }
    var note by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    var checkinSuccess by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Are we okay today?",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = OkBlack
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select your current mood and reflect.",
            fontSize = 15.sp,
            color = OkMuted
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (checkinSuccess) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OkSurface),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OkBorder)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🫂", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Check-in Recorded!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = OkBlack
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Thank you for checking in. Together we support each other with zero judgment.",
                        fontSize = 14.sp,
                        color = OkMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            mood = null
                            note = ""
                            checkinSuccess = false
                            errorMsg = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OkOrange),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("New Check-in", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Mood Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    Pair(MoodState.good, "😎\nGood"),
                    Pair(MoodState.bad, "😔\nNot Okay"),
                    Pair(MoodState.unsure, "🤔\nUnsure")
                ).forEach { (mState, label) ->
                    val isSelected = mood == mState
                    Column(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) OkOrangeLight else OkSurface)
                            .border(
                                1.dp,
                                if (isSelected) OkOrange else OkBorder,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                mood = mState
                                errorMsg = null
                            }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val parts = label.split("\n")
                        Text(text = parts[0], fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = parts[1],
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) OkOrangeShade else OkBlack
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (mood != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = OkSurface),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OkBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Add a reflection note (optional):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = OkBlack
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            placeholder = { Text("How has your day been? Write it here...", color = OkMuted) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OkOrange,
                                unfocusedBorderColor = OkBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Share publicly row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isPublic = !isPublic }
                        ) {
                            Checkbox(
                                checked = isPublic,
                                onCheckedChange = { isPublic = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = OkOrange,
                                    uncheckedColor = OkBorder
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "Share publicly in community feed",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OkBlack
                                )
                                Text(
                                    text = "Posted anonymously, with mood indicator.",
                                    fontSize = 12.sp,
                                    color = OkMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        if (errorMsg != null) {
                            Text(
                                text = errorMsg!!,
                                color = Color.Red,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Button(
                            onClick = {
                                val result = dataRepository.createCheckin(mood!!, note, isPublic)
                                if (result.isSuccess) {
                                    checkinSuccess = true
                                } else {
                                    errorMsg = result.exceptionOrNull()?.message ?: "Check-in failed"
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OkBlack),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Complete Check-in", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: COMMUNITY FEED
// -------------------------------------------------------------
@Composable
fun FeedTab() {
    val dataRepository = LocalDataRepository.current
    val posts by dataRepository.feedPosts.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(OkSurface)
                .border(1.dp, OkBorder)
                .padding(vertical = 16.dp, horizontal = 24.dp)
        ) {
            Text(
                text = "Community Feed",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OkBlack
            )
        }

        if (posts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "⛅", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Public Posts Yet",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OkBlack
                )
                Text(
                    text = "Check in and share your reflection note publicly!",
                    fontSize = 13.sp,
                    color = OkMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                items(posts) { post ->
                    FeedPostCard(
                        post = post,
                        onReactionClick = { reaction ->
                            val active = post.userReactions.contains(reaction)
                            if (active) {
                                dataRepository.removeReaction(post.id, reaction)
                            } else {
                                dataRepository.addReaction(post.id, reaction)
                            }
                        },
                        onAddComment = { content ->
                            val comment = dataRepository.addComment(post.id, content)
                            comment != null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FeedPostCard(
    post: Post,
    onReactionClick: (String) -> Unit,
    onAddComment: (String) -> Boolean
) {
    var expandedComments by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = OkSurface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, OkBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(OkBeige),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = post.avatar_emoji, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = OkBlack
                    )
                    Text(
                        text = if (post.is_anonymous) "Shared Anonymously" else "Shared Profile",
                        fontSize = 11.sp,
                        color = OkMuted
                    )
                }
                
                // Mood Badge
                val moodDetails = when (post.mood) {
                    MoodState.good -> Triple("😎 Good", OkOrangeLight, OkOrangeShade)
                    MoodState.bad -> Triple("😔 Not Okay", OkTealLight, OkTeal)
                    else -> Triple("🤔 Unsure", OkBeige, OkMuted)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(moodDetails.second)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = moodDetails.first,
                        color = moodDetails.third,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content
            Text(
                text = post.content,
                fontSize = 15.sp,
                color = OkBlack,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Reactions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val reactionsList = listOf(
                    Triple("hug", "🫂", post.reactions.hug),
                    Triple("feel_this", "💙", post.reactions.feel_this),
                    Triple("strength", "⚡", post.reactions.strength),
                    Triple("you_got_this", "🔥", post.reactions.you_got_this)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    reactionsList.forEach { (rType, emoji, count) ->
                        val isReacted = post.userReactions.contains(rType)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isReacted) OkOrangeLight else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isReacted) OkOrange else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onReactionClick(rType) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = emoji, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = count.toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isReacted) OkOrangeShade else OkMuted
                            )
                        }
                    }
                }

                // Comment Toggle Button
                Row(
                    modifier = Modifier
                        .clickable { expandedComments = !expandedComments }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${post.comments.size} Comments",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = OkMuted
                    )
                    Text(
                        text = if (expandedComments) " ▲" else " ▼",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = OkMuted
                    )
                }
            }

            // Expanded Comments Section
            if (expandedComments) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(OkBorder)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // List comments
                if (post.comments.isEmpty()) {
                    Text(
                        text = "No comments yet. Leave a kind word!",
                        fontSize = 13.sp,
                        color = OkMuted,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    post.comments.forEach { comment ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(text = comment.avatar_emoji, fontSize = 18.sp, modifier = Modifier.padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = comment.username,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = OkBlack
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "1h ago", // Simplified
                                        fontSize = 10.sp,
                                        color = OkMuted
                                    )
                                }
                                Text(
                                    text = comment.content,
                                    fontSize = 13.sp,
                                    color = OkBlack,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Add comment row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Write a supportive comment...", fontSize = 13.sp, color = OkMuted) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (commentText.isNotEmpty()) {
                                    if (onAddComment(commentText)) {
                                        commentText = ""
                                    }
                                }
                                focusManager.clearFocus()
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OkOrange,
                            unfocusedBorderColor = OkBorder
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(OkBlack)
                            .clickable {
                                if (commentText.isNotEmpty()) {
                                    if (onAddComment(commentText)) {
                                        commentText = ""
                                    }
                                }
                                focusManager.clearFocus()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: FRIENDS
// -------------------------------------------------------------
@Composable
fun FriendsTab() {
    val dataRepository = LocalDataRepository.current
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(emptyList<UserProfile>()) }
    var friendRequests by remember { mutableStateOf(dataRepository.getFriendRequests()) }
    var friendsWithMoods by remember { mutableStateOf(dataRepository.getFriendsWithMoods()) }

    // Search query update
    val performSearch = { query: String ->
        searchResults = if (query.isEmpty()) emptyList() else dataRepository.searchUsers(query)
    }

    val refreshLists = {
        friendRequests = dataRepository.getFriendRequests()
        friendsWithMoods = dataRepository.getFriendsWithMoods()
        performSearch(searchQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Friends",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = OkBlack
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Search card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OkSurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, OkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        performSearch(it)
                    },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = OkMuted) },
                    placeholder = { Text("Search profiles to connect...", color = OkMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OkOrange,
                        unfocusedBorderColor = OkBorder
                    )
                )

                if (searchResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Search Results:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = OkMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    searchResults.forEach { resultUser ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(OkBeige),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = resultUser.avatar_emoji, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = resultUser.username,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = OkBlack
                                )
                            }

                            Button(
                                onClick = {
                                    if (dataRepository.sendFriendRequest(resultUser.id)) {
                                        refreshLists()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OkOrange),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Incoming Friend Requests
        if (friendRequests.isNotEmpty()) {
            Text(
                text = "Pending Requests",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = OkBlack
            )
            Spacer(modifier = Modifier.height(8.dp))
            friendRequests.forEach { req ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = OkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OkBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = req.user.avatar_emoji, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = req.user.username,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = OkBlack
                            )
                        }

                        Button(
                            onClick = {
                                if (dataRepository.acceptFriendRequest(req.id)) {
                                    refreshLists()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OkBlack),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Accept", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Friends List
        Text(
            text = "Your Circle",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = OkBlack
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (friendsWithMoods.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OkSurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OkBorder)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🤝", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Build your support circle",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = OkBlack
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Search profiles and add friends to see how they're doing.",
                        fontSize = 12.sp,
                        color = OkMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            friendsWithMoods.forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = OkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OkBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(OkBeige),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = item.user.avatar_emoji, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = item.user.username,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = OkBlack
                              )
                        }

                        // Friend's mood indicator
                        if (item.lastCheckin != null) {
                            val (badgeLabel, badgeBg, badgeColor) = when (item.lastCheckin.mood) {
                                MoodState.good -> Triple("😎 Good", OkOrangeLight, OkOrangeShade)
                                MoodState.bad -> Triple("😔 Not Okay", OkTealLight, OkTeal)
                                else -> Triple("🤔 Unsure", OkBeige, OkMuted)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(badgeBg)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = badgeLabel,
                                    color = badgeColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = "no check-in",
                                color = OkMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 4: INSIGHTS
// -------------------------------------------------------------
@Composable
fun InsightsTab() {
    val dataRepository = LocalDataRepository.current
    var stats by remember { mutableStateOf(dataRepository.getUserStats()) }
    var weeklyMoods by remember { mutableStateOf(dataRepository.getWeeklyMoods()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Insights",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = OkBlack
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Streak & Support Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = OkSurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OkBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🔥", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${stats.streak} Days",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OkBlack
                    )
                    Text(
                        text = "Current Streak",
                        fontSize = 12.sp,
                        color = OkMuted
                    )
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = OkSurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OkBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🫂", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${stats.supportGiven} Times",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OkBlack
                    )
                    Text(
                        text = "Support Shared",
                        fontSize = 12.sp,
                        color = OkMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Weekly Mood History Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OkSurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, OkBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Weekly Reflection History",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = OkBlack
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weeklyMoods.forEach { item ->
                        val emoji = when (item.mood) {
                            MoodState.good -> "😎"
                            MoodState.bad -> "😔"
                            MoodState.unsure -> "🤔"
                            else -> "⚪"
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(OkBeige),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.day,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OkMuted
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Mood Distribution Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OkSurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, OkBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Mood Breakdown",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = OkBlack
                )
                Spacer(modifier = Modifier.height(16.dp))

                val total = (stats.moodCounts.good + stats.moodCounts.bad + stats.moodCounts.unsure).coerceAtLeast(1)
                
                // Good Row
                MoodDistributionRow(
                    label = "😎 Good",
                    count = stats.moodCounts.good,
                    percentage = stats.moodCounts.good.toFloat() / total.toFloat(),
                    color = OkOrange
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Bad Row
                MoodDistributionRow(
                    label = "😔 Not Okay",
                    count = stats.moodCounts.bad,
                    percentage = stats.moodCounts.bad.toFloat() / total.toFloat(),
                    color = OkTeal
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Unsure Row
                MoodDistributionRow(
                    label = "🤔 Unsure",
                    count = stats.moodCounts.unsure,
                    percentage = stats.moodCounts.unsure.toFloat() / total.toFloat(),
                    color = OkMuted
                )
            }
        }
    }
}

@Composable
fun MoodDistributionRow(
    label: String,
    count: Int,
    percentage: Float,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OkBlack)
            Text(text = "$count check-ins (${(percentage * 100).toInt()}%)", fontSize = 12.sp, color = OkMuted)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = percentage,
            color = color,
            trackColor = OkBeige,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

// -------------------------------------------------------------
// TAB 5: SETTINGS & PROFILE
// -------------------------------------------------------------
@Composable
fun SettingsTab(
    onSignOut: () -> Unit
) {
    val dataRepository = LocalDataRepository.current
    val currentUser by dataRepository.currentUser.collectAsState()

    var username by remember { mutableStateOf(currentUser?.username ?: "") }
    var selectedEmoji by remember { mutableStateOf(currentUser?.avatar_emoji ?: "🌙") }
    var updateSuccess by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val emojis = listOf("🌙", "😎", "😔", "🤔", "💻", "🎓", "⛵", "🌱", "🍕", "🐱", "🐶", "🦊")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = OkBlack
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Profile details card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OkSurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, OkBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Edit Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = OkBlack
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (updateSuccess) {
                    Text(
                        text = "Profile updated successfully!",
                        color = Color(0xFF2E7D32),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = Color.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        updateSuccess = false
                        errorMsg = null
                    },
                    label = { Text("Display Username") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OkOrange,
                        unfocusedBorderColor = OkBorder
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar emoji picker
                Text(
                    text = "Choose Your Avatar:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OkBlack
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
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
                                        .clickable {
                                            selectedEmoji = emoji
                                            updateSuccess = false
                                        }
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
                                        .clickable {
                                            selectedEmoji = emoji
                                            updateSuccess = false
                                        }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (username.isEmpty()) {
                            errorMsg = "Username cannot be empty."
                            return@Button
                        }
                        val result = dataRepository.updateProfile(username, selectedEmoji)
                        if (result.isSuccess) {
                            updateSuccess = true
                        } else {
                            errorMsg = result.exceptionOrNull()?.message ?: "Update failed."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OkBlack),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Settings", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sign Out Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OkSurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, OkBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Sign Out",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = OkBlack
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "To clear your session and sign in with a different profile, click below.",
                    fontSize = 13.sp,
                    color = OkMuted
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        dataRepository.signOut()
                        onSignOut()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sign Out", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
