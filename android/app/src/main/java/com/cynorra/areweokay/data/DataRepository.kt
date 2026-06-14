package com.cynorra.areweokay.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException

val LocalDataRepository = staticCompositionLocalOf<DataRepository> {
    error("No DataRepository provided")
}

interface DataRepository {
    val currentUser: StateFlow<UserProfile?>
    val feedPosts: StateFlow<List<Post>>
    fun getCurrentUserSync(): UserProfile?
    suspend fun signUp(email: String, username: String, avatarEmoji: String): Result<UserProfile>
    suspend fun signIn(email: String): Result<UserProfile>
    suspend fun signInWithPassword(email: String, password: String): Result<UserProfile>
    fun signOut()
    suspend fun updateProfile(username: String, avatarEmoji: String): Result<UserProfile>
    
    suspend fun createCheckin(mood: MoodState, note: String, isPublic: Boolean): Result<Checkin>
    fun getFeedPosts(): List<Post>
    suspend fun addReaction(postId: String, reactionType: String): Boolean
    suspend fun removeReaction(postId: String, reactionType: String): Boolean
    fun addComment(postId: String, content: String): Comment?
    
    suspend fun searchUsers(query: String): List<UserProfile>
    suspend fun sendFriendRequest(targetUserId: String): Boolean
    suspend fun getFriendRequests(): List<FriendRequestItem>
    suspend fun acceptFriendRequest(requestId: String): Boolean
    suspend fun getFriendsWithMoods(): List<FriendWithMood>
    
    suspend fun getUserStats(): UserStats
    fun getWeeklyMoods(): List<WeeklyMood>
}

class DefaultDataRepository(context: Context) : DataRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("AreWeOkayPrefs", Context.MODE_PRIVATE)
    private val json = SupabaseApi.json

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _feedPosts = MutableStateFlow<List<Post>>(emptyList())
    override val feedPosts: StateFlow<List<Post>> = _feedPosts.asStateFlow()

    private val _userCheckins = MutableStateFlow<List<Checkin>>(emptyList())
    private val _friendships = MutableStateFlow<List<Friendship>>(emptyList())

    private val repositoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        // Load initial session if exists
        val sessionStr = prefs.getString("ok_session", null)
        if (sessionStr != null) {
            try {
                val user = json.decodeFromString<UserProfile>(sessionStr)
                _currentUser.value = user
                repositoryScope.launch {
                    fetchUserCheckins(user.id)
                    fetchFriendships(user.id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fetch feed posts from Supabase
        refreshFeed()
    }

    private fun getIsoString(date: java.util.Date = java.util.Date()): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(date)
    }

    private fun parseIsoString(iso: String): java.util.Date? {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return try {
            sdf.parse(iso)
        } catch (e: Exception) {
            try {
                val fallbackSdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                fallbackSdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                fallbackSdf.parse(iso)
            } catch (ex: Exception) {
                null
            }
        }
    }

    private fun refreshFeed() {
        repositoryScope.launch {
            try {
                // Fetch approved posts and relations
                val rawJson = SupabaseApi.get("/posts?select=*,users:user_id(username,avatar_emoji),reactions(type,user_id)&deleted_at=is.null&status=eq.approved&order=created_at.desc")
                val supabasePosts = json.decodeFromString<List<SupabasePostResponse>>(rawJson)
                
                val mappedPosts = supabasePosts.map { sp ->
                    val reactionGroups = sp.reactions.groupBy { it.type }
                    val rc = ReactionCounts(
                        hug = reactionGroups["hug"]?.size ?: 0,
                        feel_this = reactionGroups["feel_this"]?.size ?: 0,
                        strength = reactionGroups["strength"]?.size ?: 0,
                        you_got_this = reactionGroups["you_got_this"]?.size ?: 0
                    )
                    val myReactions = _currentUser.value?.let { user ->
                        sp.reactions.filter { it.user_id == user.id }.map { it.type }
                    } ?: emptyList()

                    Post(
                        id = sp.id,
                        user_id = sp.user_id,
                        username = if (sp.is_anonymous) "Anonymous" else (sp.users?.username ?: "User"),
                        avatar_emoji = if (sp.is_anonymous) "🌙" else (sp.users?.avatar_emoji ?: "👤"),
                        checkin_id = sp.checkin_id,
                        content = sp.content,
                        mood = sp.mood,
                        is_anonymous = sp.is_anonymous,
                        created_at = sp.created_at,
                        reactions = rc,
                        userReactions = myReactions,
                        comments = emptyList() // Comments are mock and not stored in schema
                    )
                }
                _feedPosts.value = mappedPosts
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun fetchUserCheckins(userId: String) {
        try {
            val rawJson = SupabaseApi.get("/checkins?user_id=eq.$userId&order=created_at.desc")
            val checkins = json.decodeFromString<List<Checkin>>(rawJson)
            _userCheckins.value = checkins
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun fetchFriendships(userId: String) {
        try {
            val rawJson = SupabaseApi.get("/friendships?or=(requester_id.eq.$userId,addressee_id.eq.$userId)")
            val friendships = json.decodeFromString<List<Friendship>>(rawJson)
            _friendships.value = friendships
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getCurrentUserSync(): UserProfile? {
        return _currentUser.value
    }

    override suspend fun signUp(email: String, username: String, avatarEmoji: String): Result<UserProfile> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if user already exists
                val checkRaw = SupabaseApi.get("/users?email=eq.$email")
                val existingUsers = json.decodeFromString<List<UserProfile>>(checkRaw)
                if (existingUsers.isNotEmpty()) {
                    return@withContext Result.failure(Exception("Email already registered."))
                }

                // Check username
                val checkUsernameRaw = SupabaseApi.get("/users?username=eq.$username")
                val existingUsernames = json.decodeFromString<List<UserProfile>>(checkUsernameRaw)
                if (existingUsernames.isNotEmpty()) {
                    return@withContext Result.failure(Exception("Username already taken."))
                }

                // Create user in Supabase Auth first using the Admin API to get a valid UUID
                val authRaw = SupabaseApi.createAuthUser(email)
                val authUser = json.decodeFromString<AuthUserResponse>(authRaw)

                val newUser = UserProfile(
                    id = authUser.id,
                    email = email,
                    username = username,
                    avatar_emoji = avatarEmoji,
                    role = "user",
                    created_at = getIsoString()
                )

                // Insert into Supabase
                val userJson = json.encodeToString(newUser)
                SupabaseApi.post("/users", userJson)

                // Save session
                prefs.edit().putString("ok_session", json.encodeToString(newUser)).apply()
                _currentUser.value = newUser
                fetchUserCheckins(newUser.id)
                fetchFriendships(newUser.id)
                refreshFeed()

                Result.success(newUser)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun signIn(email: String): Result<UserProfile> {
        return withContext(Dispatchers.IO) {
            try {
                // Query by email or username
                val query = if (email.contains("@")) "email=eq.$email" else "username=eq.$email"
                val rawJson = SupabaseApi.get("/users?$query")
                val users = json.decodeFromString<List<UserProfile>>(rawJson)
                
                if (users.isEmpty()) {
                    return@withContext Result.failure(Exception("User not found. Try signing up!"))
                }
                
                val user = users.first()
                prefs.edit().putString("ok_session", json.encodeToString(user)).apply()
                _currentUser.value = user
                fetchUserCheckins(user.id)
                fetchFriendships(user.id)
                refreshFeed()

                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun signInWithPassword(email: String, password: String): Result<UserProfile> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseApi.signInWithPassword(email, password)
                val query = "email=eq.$email"
                val rawJson = SupabaseApi.get("/users?$query")
                val users = json.decodeFromString<List<UserProfile>>(rawJson)
                if (users.isEmpty()) {
                    return@withContext Result.failure(Exception("User profile not found."))
                }
                val user = users.first()
                prefs.edit().putString("ok_session", json.encodeToString(user)).apply()
                _currentUser.value = user
                fetchUserCheckins(user.id)
                fetchFriendships(user.id)
                refreshFeed()
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun signOut() {
        prefs.edit().remove("ok_session").apply()
        _currentUser.value = null
        _userCheckins.value = emptyList()
        _friendships.value = emptyList()
        refreshFeed()
    }

    override suspend fun updateProfile(username: String, avatarEmoji: String): Result<UserProfile> {
        val current = _currentUser.value ?: return Result.failure(Exception("You must be logged in"))
        return withContext(Dispatchers.IO) {
            try {
                // Check if username is taken
                val checkUsernameRaw = SupabaseApi.get("/users?username=eq.$username&id=neq.${current.id}")
                val existingUsernames = json.decodeFromString<List<UserProfile>>(checkUsernameRaw)
                if (existingUsernames.isNotEmpty()) {
                    return@withContext Result.failure(Exception("Username already taken."))
                }

                val updated = current.copy(username = username, avatar_emoji = avatarEmoji)
                val body = "{\"username\":\"$username\", \"avatar_emoji\":\"$avatarEmoji\"}"
                SupabaseApi.patch("/users?id=eq.${current.id}", body)

                prefs.edit().putString("ok_session", json.encodeToString(updated)).apply()
                _currentUser.value = updated
                refreshFeed()

                Result.success(updated)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun createCheckin(mood: MoodState, note: String, isPublic: Boolean): Result<Checkin> {
        val current = _currentUser.value ?: return Result.failure(Exception("You must be logged in to check in"))
        return withContext(Dispatchers.IO) {
            try {
                val checkinId = UUID.randomUUID().toString()
                val newCheckin = Checkin(
                    id = checkinId,
                    user_id = current.id,
                    mood = mood,
                    note = note.takeIf { it.isNotEmpty() },
                    is_public = isPublic,
                    created_at = getIsoString()
                )

                // Insert checkin into Supabase
                val checkinJson = json.encodeToString(newCheckin)
                SupabaseApi.post("/checkins", checkinJson)

                if (isPublic && note.isNotEmpty()) {
                    val newPost = Post(
                        id = UUID.randomUUID().toString(),
                        user_id = current.id,
                        username = current.username,
                        avatar_emoji = current.avatar_emoji,
                        checkin_id = checkinId,
                        content = note,
                        mood = mood,
                        is_anonymous = true,
                        created_at = getIsoString()
                    )
                    val postJson = buildJsonObject {
                        put("id", newPost.id)
                        put("user_id", newPost.user_id)
                        put("checkin_id", newPost.checkin_id)
                        put("content", newPost.content)
                        put("mood", newPost.mood?.toString())
                        put("is_anonymous", true)
                        put("status", "approved")
                    }.toString()
                    SupabaseApi.post("/posts", postJson)
                }

                fetchUserCheckins(current.id)
                refreshFeed()

                Result.success(newCheckin)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun getFeedPosts(): List<Post> {
        return _feedPosts.value
    }

    override suspend fun addReaction(postId: String, reactionType: String): Boolean {
        val current = _currentUser.value ?: return false
        return withContext(Dispatchers.IO) {
            try {
                val body = mapOf(
                    "post_id" to postId,
                    "user_id" to current.id,
                    "type" to reactionType
                )
                SupabaseApi.post("/reactions", json.encodeToString(body))
                refreshFeed()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    override suspend fun removeReaction(postId: String, reactionType: String): Boolean {
        val current = _currentUser.value ?: return false
        return withContext(Dispatchers.IO) {
            try {
                SupabaseApi.delete("/reactions?post_id=eq.$postId&user_id=eq.${current.id}&type=eq.$reactionType")
                refreshFeed()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    override fun addComment(postId: String, content: String): Comment? {
        val current = _currentUser.value ?: return null
        val newComment = Comment(
            id = "comm-" + UUID.randomUUID().toString().substring(0, 9),
            post_id = postId,
            user_id = current.id,
            username = current.username,
            avatar_emoji = current.avatar_emoji,
            content = content,
            created_at = getIsoString()
        )
        val currentPosts = _feedPosts.value.toMutableList()
        val index = currentPosts.indexOfFirst { it.id == postId }
        if (index != -1) {
            val post = currentPosts[index]
            val updatedComments = post.comments.toMutableList()
            updatedComments.add(newComment)
            currentPosts[index] = post.copy(comments = updatedComments)
            _feedPosts.value = currentPosts
        }
        return newComment
    }

    override suspend fun searchUsers(query: String): List<UserProfile> {
        val current = _currentUser.value ?: return emptyList()
        return withContext(Dispatchers.IO) {
            try {
                val rawJson = SupabaseApi.get("/users?username=ilike.*$query*&id=neq.${current.id}")
                json.decodeFromString<List<UserProfile>>(rawJson)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun sendFriendRequest(targetUserId: String): Boolean {
        val current = _currentUser.value ?: return false
        return withContext(Dispatchers.IO) {
            try {
                val body = mapOf(
                    "requester_id" to current.id,
                    "addressee_id" to targetUserId,
                    "status" to "pending"
                )
                SupabaseApi.post("/friendships", json.encodeToString(body))
                fetchFriendships(current.id)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun getFriendRequests(): List<FriendRequestItem> {
        val current = _currentUser.value ?: return emptyList()
        return withContext(Dispatchers.IO) {
            try {
                val rawJson = SupabaseApi.get("/friendships?addressee_id=eq.${current.id}&status=eq.pending&select=*,requester:requester_id(*)")
                val friendships = json.decodeFromString<List<FriendshipWithRequester>>(rawJson)
                friendships.map { FriendRequestItem(id = it.id, user = it.requester) }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun acceptFriendRequest(requestId: String): Boolean {
        val current = _currentUser.value ?: return false
        return withContext(Dispatchers.IO) {
            try {
                SupabaseApi.patch("/friendships?id=eq.$requestId", "{\"status\":\"accepted\"}")
                fetchFriendships(current.id)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun getFriendsWithMoods(): List<FriendWithMood> {
        val current = _currentUser.value ?: return emptyList()
        val acceptedFriendships = _friendships.value.filter { it.status == "accepted" }
        if (acceptedFriendships.isEmpty()) return emptyList()

        val friendIds = acceptedFriendships.map {
            if (it.requester_id == current.id) it.addressee_id else it.requester_id
        }

        return withContext(Dispatchers.IO) {
            try {
                val idsString = friendIds.joinToString(",") { it }
                val rawUsersJson = SupabaseApi.get("/users?id=in.($idsString)")
                val friendsProfiles = json.decodeFromString<List<UserProfile>>(rawUsersJson)

                val oneDayAgo = getIsoString(java.util.Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))
                val rawCheckinsJson = SupabaseApi.get("/checkins?user_id=in.($idsString)&created_at=gt.$oneDayAgo&order=created_at.desc")
                val checkins = json.decodeFromString<List<Checkin>>(rawCheckinsJson)

                friendsProfiles.map { friend ->
                    val latestCheckin = checkins.find { it.user_id == friend.id }
                    FriendWithMood(user = friend, lastCheckin = latestCheckin)
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun getUserStats(): UserStats {
        val current = _currentUser.value ?: return UserStats(0, 0, MoodCounts())
        val checkins = _userCheckins.value
        
        val streak = calculateStreak(checkins)

        val supportGiven = withContext(Dispatchers.IO) {
            try {
                val rawJson = SupabaseApi.get("/reactions?user_id=eq.${current.id}&select=count")
                val countList = json.decodeFromString<List<CountResponse>>(rawJson)
                countList.firstOrNull()?.count ?: 0
            } catch (e: Exception) {
                0
            }
        }

        var good = 0
        var bad = 0
        var unsure = 0
        checkins.forEach { c ->
            when (c.mood) {
                MoodState.good -> good++
                MoodState.bad -> bad++
                MoodState.unsure -> unsure++
            }
        }

        return UserStats(
            streak = streak,
            supportGiven = supportGiven,
            moodCounts = MoodCounts(good, bad, unsure)
        )
    }

    private fun calculateStreak(checkins: List<Checkin>): Int {
        if (checkins.isEmpty()) return 0

        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        format.timeZone = java.util.TimeZone.getTimeZone("UTC")

        val uniqueDates = checkins.mapNotNull {
            val d = parseIsoString(it.created_at) ?: return@mapNotNull null
            format.format(d)
        }.toSet()

        val todayCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        val todayStr = format.format(todayCal.time)

        val yesterdayCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        yesterdayCal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = format.format(yesterdayCal.time)

        if (!uniqueDates.contains(todayStr) && !uniqueDates.contains(yesterdayStr)) {
            return 0
        }

        var currentCal = if (uniqueDates.contains(todayStr)) todayCal else yesterdayCal
        var streak = 0

        while (true) {
            val dateStr = format.format(currentCal.time)
            if (uniqueDates.contains(dateStr)) {
                streak++
                currentCal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        return streak
    }

    override fun getWeeklyMoods(): List<WeeklyMood> {
        val current = _currentUser.value ?: return emptyList()
        val checkins = _userCheckins.value

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")

        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val result = mutableListOf<WeeklyMood>()

        for (i in 6 downTo 0) {
            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
            val dateStr = sdf.format(cal.time)

            val dayCheckin = checkins.find {
                val d = parseIsoString(it.created_at)
                d != null && sdf.format(d) == dateStr
            }

            val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
            val dayName = dayNames[dayOfWeek - 1]

            result.add(WeeklyMood(day = dayName, mood = dayCheckin?.mood))
        }

        return result
    }
}
