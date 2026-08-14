package com.avfusionapps.game_2048.ui.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.avfusionapps.game_2048.utils.AdManager
import com.unity3d.mediation.LevelPlayAdError
import com.unity3d.mediation.LevelPlayAdInfo
import com.unity3d.mediation.LevelPlayAdSize
import com.unity3d.mediation.banner.LevelPlayBannerAdView
import com.unity3d.mediation.banner.LevelPlayBannerAdViewListener

@Composable
fun LevelPlayBanner(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    val isInitialized by AdManager.isInitialized.collectAsState()

    if (isInitialized) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
            val adSize = LevelPlayAdSize.BANNER
            val config = LevelPlayBannerAdView.Config.Builder()
                .setAdSize(adSize)
                .build()

            LevelPlayBannerAdView(context, adUnitId, config).apply {
                setBannerListener(object : LevelPlayBannerAdViewListener {
                    override fun onAdLoaded(adInfo: LevelPlayAdInfo) {
                        Log.d("LevelPlayBanner", "Banner loaded")
                        visibility = android.view.View.VISIBLE
                    }

                    override fun onAdLoadFailed(error: LevelPlayAdError) {
                        Log.e("LevelPlayBanner", "Banner load failed: ${error.errorMessage}")
                        visibility = android.view.View.GONE
                    }

                    override fun onAdClicked(adInfo: LevelPlayAdInfo) {
                        Log.d("LevelPlayBanner", "Banner clicked")
                    }

                    override fun onAdLeftApplication(adInfo: LevelPlayAdInfo) {
                        Log.d("LevelPlayBanner", "Banner left application")
                    }

                    override fun onAdExpanded(adInfo: LevelPlayAdInfo) {
                        Log.d("LevelPlayBanner", "Banner expanded")
                    }

                    override fun onAdCollapsed(adInfo: LevelPlayAdInfo) {
                        Log.d("LevelPlayBanner", "Banner collapsed")
                    }
                })
                
                loadAd()
            }
        },
        onRelease = { banner ->
            banner.destroy()
        }
    )
    }
}
