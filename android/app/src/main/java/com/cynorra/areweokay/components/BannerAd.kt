package com.cynorra.areweokay.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.delay

private const val LIVE_BANNER_UNIT_ID = "ca-app-pub-4674211063760769/2064874796"

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val adWidthDp = configuration.screenWidthDp.coerceAtLeast(320)
    val sdkReady = AdGate.sdkReady

    var isLoaded by remember { mutableStateOf(false) }
    var retryTick by remember { mutableIntStateOf(0) }
    var failed by remember { mutableStateOf(false) }

    val adView = remember(adWidthDp) {
        AdView(context).apply {
            adUnitId = LIVE_BANNER_UNIT_ID
            setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    context,
                    adWidthDp
                )
            )
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    isLoaded = true
                    failed = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoaded = false
                    failed = true
                }
            }
        }
    }

    LaunchedEffect(sdkReady, retryTick, adView) {
        if (sdkReady) {
            adView.loadAd(AdRequest.Builder().build())
        }
    }

    LaunchedEffect(failed) {
        if (failed) {
            delay(25_000L)
            failed = false
            retryTick++
        }
    }

    DisposableEffect(adView) {
        onDispose {
            adView.destroy()
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isLoaded) 1f else 0f),
        factory = { adView }
    )
}
