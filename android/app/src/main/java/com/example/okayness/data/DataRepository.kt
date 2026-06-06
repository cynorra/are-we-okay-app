package com.example.okayness.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

val LocalDataRepository = staticCompositionLocalOf<DataRepository> {
    error("No DataRepository provided")
}

interface DataRepository {
    val currentUser: StateFlow<UserProfile?>
    fun getCurrentUserSync(): UserProfile?
    fun signUp(email: String, username: String, avatarEmoji: String): Result<UserProfile>
    fun signIn(email: String): Result<UserProfile>
    fun signOut()
    fun updateProfile(username: String, avatarEmoji: String): Result<UserProfile>
    
    fun createCheckin(mood: MoodState, note: String, isPublic: Boolean): Result<Checkin>
    fun getFeedPosts(): List<Post>
    fun addReaction(postId: String, reactionType: String): Boolean
    fun removeReaction(postId: String, reactionType: String): Boolean
    fun addComment(postId: String, content: String): Comment?
    
    fun searchUsers(query: String): List<UserProfile>
    fun sendFriendRequest(targetUserId: String): Boolean
    fun getFriendRequests(): List<FriendRequestItem>
    fun acceptFriendRequest(requestId: String): Boolean
    fun getFriendsWithMoods(): List<FriendWithMood>
    
    fun getUserStats(): UserStats
    fun getWeeklyMoods(): List<WeeklyMood>
}

class DefaultDataRepository(context: Context) : DataRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("OkaynessPrefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    init {
        // Load initial session if exists
        val sessionStr = prefs.getString("ok_session", null)
        if (sessionStr != null) {
            try {
                _currentUser.value = json.decodeFromString<UserProfile>(sessionStr)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Seed data if empty
        seedDataIfEmpty()
    }

    private fun getIsoString(date: Date = Date()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(date)
    }

    private fun parseIsoString(iso: String): Date? {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return try {
            sdf.parse(iso)
        } catch (e: Exception) {
            try {
                // Try fallback for sub-seconds
                val fallbackSdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                fallbackSdf.timeZone = TimeZone.getTimeZone("UTC")
                fallbackSdf.parse(iso)
            } catch (ex: Exception) {
                null
            }
        }
    }

    private fun seedDataIfEmpty() {
        val usersStr = prefs.getString("ok_users", null)
        if (usersStr.isNullOrEmpty()) {
            val initialUsers = listOf(
                UserProfile(id = "usr-1", username = "stressed_coder", avatar_emoji = "💻", role = "user", created_at = getIsoString()),
                UserProfile(id = "usr-2", username = "exam_winner", avatar_emoji = "🎓", role = "user", created_at = getIsoString()),
                UserProfile(id = "usr-3", username = "wanderer", avatar_emoji = "⛵", role = "user", created_at = getIsoString())
            )
            prefs.edit().putString("ok_users", json.encodeToString(initialUsers)).apply()
        }

        val postsStr = prefs.getString("ok_posts", null)
        if (postsStr.isNullOrEmpty()) {
            val nowMs = System.currentTimeMillis()
            val initialPosts = listOf(
                Post(
                    id = "post-1",
                    user_id = "usr-1",
                    username = "stressed_coder",
                    avatar_emoji = "💻",
                    content = "Just feeling really overwhelmed with work lately. Doesn't seem to end.",
                    mood = MoodState.bad,
                    is_anonymous = true,
                    created_at = getIsoString(Date(nowMs - 2 * 60 * 60 * 1000)),
                    reactions = ReactionCounts(hug = 12, feel_this = 8, strength = 3, you_got_this = 1),
                    userReactions = emptyList(),
                    comments = listOf(
                        Comment(
                            id = "c-1",
                            post_id = "post-1",
                            user_id = "usr-2",
                            username = "exam_winner",
                            avatar_emoji = "🎓",
                            content = "Hang in there, take short breaks! 🫂",
                            created_at = getIsoString(Date(nowMs - 90 * 60 * 1000))
                        )
                    )
                ),
                Post(
                    id = "post-2",
                    user_id = "usr-2",
                    username = "exam_winner",
                    avatar_emoji = "🎓",
                    content = "I passed my final exam! So relieved.",
                    mood = MoodState.good,
                    is_anonymous = false,
                    created_at = getIsoString(Date(nowMs - 4 * 60 * 60 * 1000)),
                    reactions = ReactionCounts(hug = 85, feel_this = 5, strength = 42, you_got_this = 10),
                    userReactions = emptyList(),
                    comments = emptyList()
                ),
                Post(
                    id = "post-3",
                    user_id = "usr-3",
                    username = "wanderer",
                    avatar_emoji = "⛵",
                    content = "Not sure where I'm going in life right now, but taking it one day at a time.",
                    mood = MoodState.unsure,
                    is_anonymous = true,
                    created_at = getIsoString(Date(nowMs - 5 * 60 * 60 * 1000)),
                    reactions = ReactionCounts(hug = 45, feel_this = 32, strength = 8, you_got_this = 4),
                    userReactions = emptyList(),
                    comments = emptyList()
                )
            )
            prefs.edit().putString("ok_posts", json.encodeToString(initialPosts)).apply()
        }
    }

    private fun getUsersList(): List<UserProfile> {
        val s = prefs.getString("ok_users", null) ?: return emptyList()
        return try { json.decodeFromString(s) } catch (e: Exception) { emptyList() }
    }

    private fun saveUsersList(list: List<UserProfile>) {
        prefs.edit().putString("ok_users", json.encodeToString(list)).apply()
    }

    private fun getPostsList(): List<Post> {
        val s = prefs.getString("ok_posts", null) ?: return emptyList()
        return try { json.decodeFromString(s) } catch (e: Exception) { emptyList() }
    }

    private fun savePostsList(list: List<Post>) {
        prefs.edit().putString("ok_posts", json.encodeToString(list)).apply()
    }

    private fun getCheckinsList(): List<Checkin> {
        val s = prefs.getString("ok_checkins", null) ?: return emptyList()
        return try { json.decodeFromString(s) } catch (e: Exception) { emptyList() }
    }

    private fun saveCheckinsList(list: List<Checkin>) {
        prefs.edit().putString("ok_checkins", json.encodeToString(list)).apply()
    }

    private fun getFriendshipsList(): List<Friendship> {
        val s = prefs.getString("ok_friendships", null) ?: return emptyList()
        return try { json.decodeFromString(s) } catch (e: Exception) { emptyList() }
    }

    private fun saveFriendshipsList(list: List<Friendship>) {
        prefs.edit().putString("ok_friendships", json.encodeToString(list)).apply()
    }

    override fun getCurrentUserSync(): UserProfile? {
        return _currentUser.value
    }

    override fun signUp(email: String, username: String, avatarEmoji: String): Result<UserProfile> {
        val users = getUsersList().toMutableList()
        if (users.any { it.email.equals(email, ignoreCase = true) }) {
            return Result.failure(Exception("Email already registered."))
        }
        if (users.any { it.username.equals(username, ignoreCase = true) }) {
            return Result.failure(Exception("Username already taken."))
        }

        val newUser = UserProfile(
            id = "usr-" + UUID.randomUUID().toString().substring(0, 9),
            email = email,
            username = username,
            avatar_emoji = avatarEmoji,
            created_at = getIsoString()
        )

        users.add(newUser)
        saveUsersList(users)

        // Save session
        prefs.edit().putString("ok_session", json.encodeToString(newUser)).apply()
        _currentUser.value = newUser

        return Result.success(newUser)
    }

    override fun signIn(email: String): Result<UserProfile> {
        val users = getUsersList()
        val user = users.find { it.email.equals(email, ignoreCase = true) || it.username.equals(email, ignoreCase = true) }
            ?: return Result.failure(Exception("User not found. Try signing up!"))

        // Save session
        prefs.edit().putString("ok_session", json.encodeToString(user)).apply()
        _currentUser.value = user

        return Result.success(user)
    }

    override fun signOut() {
        prefs.edit().remove("ok_session").apply()
        _currentUser.value = null
    }

    override fun updateProfile(username: String, avatarEmoji: String): Result<UserProfile> {
        val current = _currentUser.value ?: return Result.failure(Exception("You must be logged in"))
        val users = getUsersList().toMutableList()
        val index = users.indexOfFirst { it.id == current.id }
        if (index == -1) return Result.failure(Exception("User session not found"))

        if (users.any { it.id != current.id && it.username.equals(username, ignoreCase = true) }) {
            return Result.failure(Exception("Username already taken."))
        }

        val updated = users[index].copy(username = username, avatar_emoji = avatarEmoji)
        users[index] = updated
        saveUsersList(users)

        // Update session
        prefs.edit().putString("ok_session", json.encodeToString(updated)).apply()
        _currentUser.value = updated

        return Result.success(updated)
    }

    override fun createCheckin(mood: MoodState, note: String, isPublic: Boolean): Result<Checkin> {
        val current = _currentUser.value ?: return Result.failure(Exception("You must be logged in to check in"))

        val checkins = getCheckinsList().toMutableList()
        val newCheckin = Checkin(
            id = "chk-" + UUID.randomUUID().toString().substring(0, 9),
            user_id = current.id,
            mood = mood,
            note = note.takeIf { it.isNotEmpty() },
            is_public = isPublic,
            created_at = getIsoString()
        )

        checkins.add(newCheckin)
        saveCheckinsList(checkins)

        if (isPublic && note.isNotEmpty()) {
            val posts = getPostsList().toMutableList()
            val newPost = Post(
                id = "post-" + UUID.randomUUID().toString().substring(0, 9),
                user_id = current.id,
                username = current.username,
                avatar_emoji = current.avatar_emoji,
                checkin_id = newCheckin.id,
                content = note,
                mood = mood,
                is_anonymous = true, // Default to anonymous
                created_at = getIsoString()
            )
            posts.add(0, newPost) // Add at start of list
            savePostsList(posts)
        }

        return Result.success(newCheckin)
    }

    override fun getFeedPosts(): List<Post> {
        val current = _currentUser.value
        val posts = getPostsList()
        val users = getUsersList()

        return posts.map { p ->
            val author = users.find { it.id == p.user_id }
            val resolvedUsername = if (p.is_anonymous) "Anonymous" else (author?.username ?: p.username)
            val resolvedAvatar = if (p.is_anonymous) "🌙" else (author?.avatar_emoji ?: p.avatar_emoji)

            p.copy(
                username = resolvedUsername,
                avatar_emoji = resolvedAvatar
            )
        }
    }

    override fun addReaction(postId: String, reactionType: String): Boolean {
        val current = _currentUser.value ?: return false
        val posts = getPostsList().toMutableList()
        val index = posts.indexOfFirst { it.id == postId }
        if (index == -1) return false

        val post = posts[index]
        val userReactions = post.userReactions.toMutableList()
        if (!userReactions.contains(reactionType)) {
            userReactions.add(reactionType)
            val rc = post.reactions
            val updatedRc = when (reactionType) {
                "hug" -> rc.copy(hug = rc.hug + 1)
                "feel_this" -> rc.copy(feel_this = rc.feel_this + 1)
                "strength" -> rc.copy(strength = rc.strength + 1)
                "you_got_this" -> rc.copy(you_got_this = rc.you_got_this + 1)
                else -> rc
            }
            posts[index] = post.copy(userReactions = userReactions, reactions = updatedRc)
            savePostsList(posts)
            return true
        }
        return false
    }

    override fun removeReaction(postId: String, reactionType: String): Boolean {
        val current = _currentUser.value ?: return false
        val posts = getPostsList().toMutableList()
        val index = posts.indexOfFirst { it.id == postId }
        if (index == -1) return false

        val post = posts[index]
        val userReactions = post.userReactions.toMutableList()
        if (userReactions.contains(reactionType)) {
            userReactions.remove(reactionType)
            val rc = post.reactions
            val updatedRc = when (reactionType) {
                "hug" -> rc.copy(hug = (rc.hug - 1).coerceAtLeast(0))
                "feel_this" -> rc.copy(feel_this = (rc.feel_this - 1).coerceAtLeast(0))
                "strength" -> rc.copy(strength = (rc.strength - 1).coerceAtLeast(0))
                "you_got_this" -> rc.copy(you_got_this = (rc.you_got_this - 1).coerceAtLeast(0))
                else -> rc
            }
            posts[index] = post.copy(userReactions = userReactions, reactions = updatedRc)
            savePostsList(posts)
            return true
        }
        return false
    }

    override fun addComment(postId: String, content: String): Comment? {
        val current = _currentUser.value ?: return null
        val posts = getPostsList().toMutableList()
        val index = posts.indexOfFirst { it.id == postId }
        if (index == -1) return null

        val post = posts[index]
        val newComment = Comment(
            id = "comm-" + UUID.randomUUID().toString().substring(0, 9),
            post_id = postId,
            user_id = current.id,
            username = current.username,
            avatar_emoji = current.avatar_emoji,
            content = content,
            created_at = getIsoString()
        )

        val comments = post.comments.toMutableList()
        comments.add(newComment)
        posts[index] = post.copy(comments = comments)
        savePostsList(posts)

        return newComment
    }

    override fun searchUsers(query: String): List<UserProfile> {
        val current = _currentUser.value ?: return emptyList()
        val users = getUsersList()
        return users.filter {
            it.id != current.id && it.username.contains(query, ignoreCase = true)
        }
    }

    override fun sendFriendRequest(targetUserId: String): Boolean {
        val current = _currentUser.value ?: return false
        val friendships = getFriendshipsList().toMutableList()

        val exists = friendships.any {
            (it.requester_id == current.id && it.addressee_id == targetUserId) ||
            (it.requester_id == targetUserId && it.addressee_id == current.id)
        }
        if (exists) return false

        val newFriendship = Friendship(
            id = "frnd-" + UUID.randomUUID().toString().substring(0, 9),
            requester_id = current.id,
            addressee_id = targetUserId,
            status = "pending",
            created_at = getIsoString()
        )
        friendships.add(newFriendship)
        saveFriendshipsList(friendships)
        return true
    }

    override fun getFriendRequests(): List<FriendRequestItem> {
        val current = _currentUser.value ?: return emptyList()
        val friendships = getFriendshipsList()
        val users = getUsersList()

        val incomingRequests = friendships.filter {
            it.addressee_id == current.id && it.status == "pending"
        }

        return incomingRequests.mapNotNull { f ->
            val reqUser = users.find { it.id == f.requester_id } ?: return@mapNotNull null
            FriendRequestItem(id = f.id, user = reqUser)
        }
    }

    override fun acceptFriendRequest(requestId: String): Boolean {
        val friendships = getFriendshipsList().toMutableList()
        val index = friendships.indexOfFirst { it.id == requestId }
        if (index == -1) return false

        friendships[index] = friendships[index].copy(status = "accepted")
        saveFriendshipsList(friendships)
        return true
    }

    override fun getFriendsWithMoods(): List<FriendWithMood> {
        val current = _currentUser.value ?: return emptyList()
        val friendships = getFriendshipsList()
        val users = getUsersList()
        val checkins = getCheckinsList()

        val accepted = friendships.filter {
            (it.requester_id == current.id || it.addressee_id == current.id) && it.status == "accepted"
        }

        val friendIds = accepted.map {
            if (it.requester_id == current.id) it.addressee_id else it.requester_id
        }

        val oneDayAgoMs = System.currentTimeMillis() - 24 * 60 * 60 * 1000

        return friendIds.map { fId ->
            val friendProfile = users.find { it.id == fId } ?: UserProfile(id = fId, username = "Friend", avatar_emoji = "👤", created_at = "")
            // Find latest checkin from last 24h
            val latestCheckin = checkins
                .filter { it.user_id == fId }
                .mapNotNull { c ->
                    val date = parseIsoString(c.created_at) ?: return@mapNotNull null
                    Pair(c, date.time)
                }
                .filter { it.second > oneDayAgoMs }
                .maxByOrNull { it.second }?.first

            FriendWithMood(user = friendProfile, lastCheckin = latestCheckin)
        }
    }

    override fun getUserStats(): UserStats {
        val current = _currentUser.value ?: return UserStats(0, 0, MoodCounts())
        val checkins = getCheckinsList().filter { it.user_id == current.id }
        val posts = getPostsList()

        // 1. Calculate streak
        val streak = calculateStreak(checkins)

        // 2. Count reactions given by current user to other users' posts
        var supportGiven = 0
        posts.forEach { p ->
            if (p.user_id != current.id) {
                // If userReactions is not empty, it means current user reacted to it
                supportGiven += p.userReactions.size
            }
        }

        // 3. Mood counts
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

        // Parse dates and get unique dates in YYYY-MM-DD
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")

        val uniqueDates = checkins.mapNotNull {
            val d = parseIsoString(it.created_at) ?: return@mapNotNull null
            format.format(d)
        }.toSet()

        val todayCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val todayStr = format.format(todayCal.time)

        val yesterdayCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        yesterdayCal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = format.format(yesterdayCal.time)

        // Streak continues if they checked in today or yesterday
        if (!uniqueDates.contains(todayStr) && !uniqueDates.contains(yesterdayStr)) {
            return 0
        }

        var currentCal = if (uniqueDates.contains(todayStr)) todayCal else yesterdayCal
        var streak = 0

        while (true) {
            val dateStr = format.format(currentCal.time)
            if (uniqueDates.contains(dateStr)) {
                streak++
                currentCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        return streak
    }

    override fun getWeeklyMoods(): List<WeeklyMood> {
        val current = _currentUser.value ?: return emptyList()
        val checkins = getCheckinsList().filter { it.user_id == current.id }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val result = mutableListOf<WeeklyMood>()

        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = sdf.format(cal.time)

            // Find checkin on this date
            val dayCheckin = checkins.find {
                val d = parseIsoString(it.created_at)
                d != null && sdf.format(d) == dateStr
            }

            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sun, 2 = Mon, etc.
            val dayName = dayNames[dayOfWeek - 1]

            result.add(WeeklyMood(day = dayName, mood = dayCheckin?.mood))
        }

        return result
    }
}
