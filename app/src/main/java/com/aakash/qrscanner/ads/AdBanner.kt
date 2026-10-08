package com.aakash.qrscanner.ads

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import com.aakash.qrscanner.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val width = LocalConfiguration.current.screenWidthDp.coerceAtLeast(1)
    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                adUnitId = BuildConfig.BANNER_AD_UNIT_ID
                setAdSize(AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, width))
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

