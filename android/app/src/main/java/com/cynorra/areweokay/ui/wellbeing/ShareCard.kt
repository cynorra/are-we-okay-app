package com.cynorra.areweokay.ui.wellbeing

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.cynorra.areweokay.R
import com.cynorra.areweokay.data.MoodScale
import java.io.File

object ShareCard {
    private const val W = 1080
    private const val H = 1920 // 9:16 (stories / reels / status)

    fun inviteLink(ctx: Context, inviterId: String?): String {
        val host = ctx.getString(R.string.invite_host)
        return if (inviterId != null) "https://$host/invite?ref=$inviterId" else "https://$host/"
    }

    /** Renders a 1080x1920 mood card. Contains no note text or identity, only mood, emotions and streak. */
    fun render(ctx: Context, level: Int, emotionLabels: List<String>, streak: Int): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val bg = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, H.toFloat(), 0xFFFDF1ED.toInt(), 0xFFE0EBF5.toInt(), Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), bg)

        fun paint(size: Float, color: Int, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size; this.color = color; textAlign = Paint.Align.CENTER
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        val cx = W / 2f
        val idx = (level - 1).coerceIn(0, 4)
        c.drawText(ctx.getString(R.string.card_today), cx, 360f, paint(54f, 0xFF8A7A75.toInt()))
        c.drawText(MoodScale.emojis[idx], cx, 820f, paint(420f, 0xFF000000.toInt()))
        val moodNames = listOf(R.string.mood_1, R.string.mood_2, R.string.mood_3, R.string.mood_4, R.string.mood_5)
        c.drawText(ctx.getString(moodNames[idx]), cx, 1010f, paint(96f, 0xFF402E2A.toInt(), true))
        if (emotionLabels.isNotEmpty()) {
            c.drawText(emotionLabels.take(3).joinToString("  ·  "), cx, 1110f, paint(52f, 0xFFC45F45.toInt()))
        }
        if (streak > 1) {
            c.drawText("🔥 " + ctx.getString(R.string.card_streak, streak), cx, 1290f, paint(60f, 0xFF402E2A.toInt(), true))
        }
        c.drawText(ctx.getString(R.string.card_cta), cx, 1600f, paint(56f, 0xFF402E2A.toInt(), true))
        c.drawText(ctx.getString(R.string.app_name), cx, 1700f, paint(48f, 0xFFE07A5F.toInt(), true))
        return bmp
    }

    fun share(ctx: Context, level: Int, emotionLabels: List<String>, streak: Int, inviterId: String?) {
        val bmp = render(ctx, level, emotionLabels, streak)
        val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
        val f = File(dir, "mood_card.png")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, ctx.getString(R.string.share_text, inviteLink(ctx, inviterId)))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, ctx.getString(R.string.share_chooser)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Text-only invite (no card). */
    fun invite(ctx: Context, inviterId: String?) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, ctx.getString(R.string.invite_text, inviteLink(ctx, inviterId)))
        }
        ctx.startActivity(Intent.createChooser(send, ctx.getString(R.string.share_chooser)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
