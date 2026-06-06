package com.example.okayness.data

import kotlinx.serialization.Serializable

@Serializable
enum class MoodState {
    good, bad, unsure
}

@Serializable
enum class ReactionType {
    hug, feel_this, strength, you_got_this
}

@Serializable
data class UserProfile(
    val id: String,
    val email: String? = null,
    val username: String,
    val display_name: String? = null,
    val avatar_emoji: String = "🌙",
    val role: String = "user",
    val created_at: String
)

@Serializable
data class Checkin(
    val id: String,
    val user_id: String,
    val mood: MoodState,
    val note: String? = null,
    val is_public: Boolean = true,
    val created_at: String
)

@Serializable
data class Comment(
    val id: String,
    val post_id: String,
    val user_id: String,
    val username: String,
    val avatar_emoji: String,
    val content: String,
    val created_at: String
)

@Serializable
data class ReactionCounts(
    val hug: Int = 0,
    val feel_this: Int = 0,
    val strength: Int = 0,
    val you_got_this: Int = 0
)

@Serializable
data class Post(
    val id: String,
    val user_id: String,
    val username: String,
    val avatar_emoji: String,
    val checkin_id: String? = null,
    val content: String,
    val mood: MoodState? = null,
    val is_anonymous: Boolean = true,
    val created_at: String,
    val reactions: ReactionCounts = ReactionCounts(),
    val userReactions: List<String> = emptyList(), // Store strings representing ReactionType enum
    val comments: List<Comment> = emptyList()
)

@Serializable
data class Friendship(
    val id: String,
    val requester_id: String,
    val addressee_id: String,
    val status: String = "pending", // "pending", "accepted", "blocked"
    val created_at: String
)

@Serializable
data class FriendWithMood(
    val user: UserProfile,
    val lastCheckin: Checkin?
)

@Serializable
data class MoodCounts(
    val good: Int = 0,
    val bad: Int = 0,
    val unsure: Int = 0
)

@Serializable
data class UserStats(
    val streak: Int,
    val supportGiven: Int,
    val moodCounts: MoodCounts
)

@Serializable
data class WeeklyMood(
    val day: String,
    val mood: MoodState?
)

@Serializable
data class FriendRequestItem(
    val id: String,
    val user: UserProfile
)
