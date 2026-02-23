package com.DefaultCompany.Tahmin11

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity

// -------------------- YANDEX --------------------
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.MobileAds as YMobileAds

import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.banner.BannerAdEventListener

import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader

import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader

// -------------------- ADMOB --------------------
import com.google.android.gms.ads.AdRequest as GAdRequest
import com.google.android.gms.ads.LoadAdError as GLoadAdError
import com.google.android.gms.ads.MobileAds as GMobileAds
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.AdError as GAdError
import com.google.android.gms.ads.interstitial.InterstitialAd as GInterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback as GInterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd as GRewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback as GRewardedAdLoadCallback
import com.google.android.gms.ads.AdView as GAdView
import com.google.android.gms.ads.AdSize as GAdSize

class MainActivity : AppCompatActivity() {

    private lateinit var myWebView: WebView

    // -------------------- YANDEX ADS --------------------
    private var yandexInterstitial: InterstitialAd? = null
    private var yandexRewarded: RewardedAd? = null

    private var yandexInterstitialLoader: InterstitialAdLoader? = null
    private var yandexRewardedLoader: RewardedAdLoader? = null

    private var bannerAdView: BannerAdView? = null

    // Updated Yandex Ad Unit IDs
    private val Y_INTERSTITIAL_ID = "R-M-18543851-10"
    private val Y_REWARDED_ID = "R-M-18543851-8"
    private val Y_BANNER_ID = "R-M-18543851-9"

    // -------------------- ADMOB ADS --------------------
    // Updated AdMob Ad Unit IDs
    private val ADMOB_INTERSTITIAL_ID = "ca-app-pub-5879474591831999/8282395523"
    private val ADMOB_REWARDED_ID = "ca-app-pub-5879474591831999/6350366758"
    private val ADMOB_BANNER_ID = "ca-app-pub-5879474591831999/2171448597"

    private var admobInterstitial: GInterstitialAd? = null
    private var admobRewarded: GRewardedAd? = null
    private var admobBannerView: GAdView? = null

    private val TAG = "AdsMix"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 0) Initialize Loaders
        setupYandexInterstitial()
        setupYandexRewarded()

        // 1) Initialize SDKs
        // Yandex
        YMobileAds.initialize(this) {
            Log.i(TAG, "Yandex SDK initialized")
            loadYandexInterstitialAd()
            loadYandexRewardedAd()
        }

        // AdMob
        GMobileAds.initialize(this) {
            Log.i(TAG, "AdMob SDK initialized")
            loadAdMobInterstitial()
            loadAdMobRewarded()
        }

        // 2) Configure WebView
        myWebView = findViewById(R.id.webview)

        // Prevent white screen flash
        myWebView.setBackgroundColor(Color.TRANSPARENT)

        // Performance optimization: Hardware Acceleration
        myWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        myWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = true
        }

        myWebView.addJavascriptInterface(WebAppInterface(this), "Android")

        myWebView.webChromeClient = WebChromeClient()
        myWebView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                Log.e(TAG, "WebView Error: ${error?.description}")
                // In case of error, ensure game doesn't get stuck if it was waiting for something
                super.onReceivedError(view, request, error)
            }
        }

        myWebView.loadUrl("file:///android_asset/index.html")

        // 3) Setup Banner
        setupYandexBanner()

        // Back Button Handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (myWebView.canGoBack()) myWebView.goBack() else finish()
            }
        })
    }

    // -------------------- HELPER: NOTIFY JS --------------------
    // Safely call window.onAdClosed() to resume game loop
    fun notifyAdClosed() {
        runOnUiThread {
            myWebView.evaluateJavascript("if(window.onAdClosed) window.onAdClosed();", null)
        }
    }

    fun notifyRewardGranted() {
        runOnUiThread {
            myWebView.evaluateJavascript(
                "if(window.AdManager && AdManager.onReward) AdManager.onReward(); if(window.grantReward) window.grantReward();",
                null
            )
        }
    }

    // -------------------- YANDEX BANNER --------------------
    private fun setupYandexBanner() {
        if (Y_BANNER_ID.isBlank()) return

        val container = findViewById<FrameLayout>(R.id.adContainer) ?: return

        // Cleanup old views
        container.removeAllViews()

        bannerAdView = BannerAdView(this).apply {
            setAdUnitId(Y_BANNER_ID)
            setAdSize(BannerAdSize.stickySize(this@MainActivity, 320))
            setBannerAdEventListener(object : BannerAdEventListener {
                override fun onAdLoaded() { Log.d(TAG, "Yandex Banner Loaded") }
                override fun onAdFailedToLoad(error: AdRequestError) {
                    Log.e(TAG, "Yandex Banner Failed: ${error.description}")
                    setupAdMobBanner() // Fallback to AdMob
                }
                override fun onAdClicked() {}
                override fun onLeftApplication() {}
                override fun onReturnedToApplication() {}
                override fun onImpression(data: ImpressionData?) {}
            })
        }

        container.addView(bannerAdView)

        val request = AdRequest.Builder().build()
        bannerAdView?.loadAd(request)
    }

    // -------------------- ADMOB BANNER (FALLBACK) --------------------
    private fun setupAdMobBanner() {
        val container = findViewById<FrameLayout>(R.id.adContainer) ?: return

        runOnUiThread {
            container.removeAllViews()
            if (admobBannerView != null) {
                admobBannerView?.destroy()
            }

            admobBannerView = GAdView(this).apply {
                setAdSize(GAdSize.BANNER)
                adUnitId = ADMOB_BANNER_ID
            }

            container.addView(admobBannerView)

            val adRequest = GAdRequest.Builder().build()
            admobBannerView?.loadAd(adRequest)
            Log.d(TAG, "Requesting AdMob Banner Fallback")
        }
    }

    // -------------------- YANDEX INTERSTITIAL --------------------
    private fun setupYandexInterstitial() {
        yandexInterstitialLoader = InterstitialAdLoader(this).apply {
            setAdLoadListener(object : InterstitialAdLoadListener {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    yandexInterstitial = interstitialAd
                    Log.d(TAG, "Yandex Interstitial Loaded")
                }

                override fun onAdFailedToLoad(adRequestError: AdRequestError) {
                    yandexInterstitial = null
                    Log.e(TAG, "Yandex Interstitial Failed: ${adRequestError.description}")
                    retryLoad(isInterstitial = true, delay = 30_000)
                }
            })
        }
    }

    private fun loadYandexInterstitialAd() {
        val config = AdRequestConfiguration.Builder(Y_INTERSTITIAL_ID).build()
        yandexInterstitialLoader?.loadAd(config)
    }

    private fun showYandexInterstitial(onDone: (() -> Unit)? = null) {
        yandexInterstitial?.let { ad ->
            ad.setAdEventListener(object : InterstitialAdEventListener {
                override fun onAdShown() {
                    Log.d(TAG, "Yandex Interstitial Shown")
                }

                override fun onAdFailedToShow(adError: AdError) {
                    Log.e(TAG, "Yandex Interstitial failed to show: ${adError.description}")
                    yandexInterstitial = null
                    loadYandexInterstitialAd()
                    // If failed to show, try next step (fallback)
                    onDone?.invoke()
                }

                override fun onAdDismissed() {
                    Log.d(TAG, "Yandex Interstitial dismissed")
                    yandexInterstitial = null
                    loadYandexInterstitialAd()
                    // Success path, but we still trigger 'done' to ensure game resumes if this was the final step
                    notifyAdClosed()
                }

                override fun onAdClicked() {}
                override fun onAdImpression(impressionData: ImpressionData?) {}
            })
            ad.show(this)
        } ?: run {
            Log.d(TAG, "Yandex Interstitial not ready")
            loadYandexInterstitialAd()
            onDone?.invoke()
        }
    }

    // -------------------- YANDEX REWARDED --------------------
    private fun setupYandexRewarded() {
        yandexRewardedLoader = RewardedAdLoader(this).apply {
            setAdLoadListener(object : RewardedAdLoadListener {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    yandexRewarded = rewardedAd
                    Log.d(TAG, "Yandex Rewarded Loaded")
                }

                override fun onAdFailedToLoad(adRequestError: AdRequestError) {
                    yandexRewarded = null
                    Log.e(TAG, "Yandex Rewarded Failed: ${adRequestError.description}")
                    retryLoad(isInterstitial = false, delay = 30_000)
                }
            })
        }
    }

    private fun loadYandexRewardedAd() {
        val config = AdRequestConfiguration.Builder(Y_REWARDED_ID).build()
        yandexRewardedLoader?.loadAd(config)
    }

    private fun showYandexRewarded(
        onReward: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null
    ) {
        yandexRewarded?.let { ad ->
            ad.setAdEventListener(object : RewardedAdEventListener {
                override fun onAdShown() {}

                override fun onAdFailedToShow(adError: AdError) {
                    Log.e(TAG, "Yandex Rewarded failed to show: ${adError.description}")
                    yandexRewarded = null
                    loadYandexRewardedAd()
                    onDone?.invoke()
                }

                override fun onAdDismissed() {
                    yandexRewarded = null
                    loadYandexRewardedAd()
                    notifyAdClosed()
                }

                override fun onAdClicked() {}
                override fun onAdImpression(impressionData: ImpressionData?) {}

                override fun onRewarded(reward: Reward) {
                    Log.d(TAG, "Yandex reward: ${reward.amount} ${reward.type}")
                    notifyRewardGranted()
                    onReward?.invoke()
                }
            })
            ad.show(this)
        } ?: run {
            Log.d(TAG, "Yandex Rewarded not ready")
            loadYandexRewardedAd()
            onDone?.invoke()
        }
    }

    // -------------------- ADMOB INTERSTITIAL --------------------
    private fun loadAdMobInterstitial() {
        val req = GAdRequest.Builder().build()
        GInterstitialAd.load(
            this,
            ADMOB_INTERSTITIAL_ID,
            req,
            object : GInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: GInterstitialAd) {
                    admobInterstitial = ad
                    Log.d(TAG, "AdMob Interstitial Loaded")

                    admobInterstitial?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            admobInterstitial = null
                            loadAdMobInterstitial()
                            notifyAdClosed()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: GAdError) {
                            admobInterstitial = null
                            loadAdMobInterstitial()
                            notifyAdClosed() // Fallback to ensure game doesn't hang
                        }
                    }
                }

                override fun onAdFailedToLoad(error: GLoadAdError) {
                    admobInterstitial = null
                    Log.e(TAG, "AdMob Interstitial Failed: ${error.message}")
                }
            }
        )
    }

    private fun showAdMobInterstitial(onDone: (() -> Unit)? = null) {
        val ad = admobInterstitial
        if (ad == null) {
            Log.d(TAG, "AdMob Interstitial not ready")
            loadAdMobInterstitial()
            onDone?.invoke() // Proceed to final fallback
            return
        }

        ad.show(this)
    }

    // -------------------- ADMOB REWARDED --------------------
    private fun loadAdMobRewarded() {
        val req = GAdRequest.Builder().build()
        GRewardedAd.load(
            this,
            ADMOB_REWARDED_ID,
            req,
            object : GRewardedAdLoadCallback() {
                override fun onAdLoaded(ad: GRewardedAd) {
                    admobRewarded = ad
                    Log.d(TAG, "AdMob Rewarded Loaded")

                    admobRewarded?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            admobRewarded = null
                            loadAdMobRewarded()
                            notifyAdClosed()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: GAdError) {
                            admobRewarded = null
                            loadAdMobRewarded()
                            notifyAdClosed()
                        }
                    }
                }

                override fun onAdFailedToLoad(error: GLoadAdError) {
                    admobRewarded = null
                    Log.e(TAG, "AdMob Rewarded Failed: ${error.message}")
                }
            }
        )
    }

    private fun showAdMobRewarded(
        onReward: ((RewardItem) -> Unit)? = null,
        onDone: (() -> Unit)? = null
    ) {
        val ad = admobRewarded
        if (ad == null) {
            Log.d(TAG, "AdMob Rewarded not ready")
            loadAdMobRewarded()
            onDone?.invoke()
            return
        }

        ad.show(this) { rewardItem ->
            Log.d(TAG, "AdMob reward: ${rewardItem.amount} ${rewardItem.type}")
            notifyRewardGranted()
            onReward?.invoke(rewardItem)
        }
    }

    // -------------------- MAIN LOGIC: CHAINED FALLBACK --------------------

    fun showInterstitialFromJS(adUnitId: String? = null) {
        runOnUiThread {
            // 1. Try Yandex
            showYandexInterstitial(
                onDone = {
                    // 2. If Yandex failed/not ready, Try AdMob
                    Log.d(TAG, "Yandex Interstitial failed/not ready, trying AdMob...")
                    showAdMobInterstitial(
                        onDone = {
                            // 3. If AdMob failed/not ready, just resume game
                            Log.d(TAG, "AdMob Interstitial also failed/not ready. Resuming game.")
                            notifyAdClosed()
                        }
                    )
                }
            )
        }
    }

    fun showRewardedFromJS(adUnitId: String? = null) {
        runOnUiThread {
            // 1. Try Yandex
            showYandexRewarded(
                onReward = {
                    // Yandex Reward granted
                },
                onDone = {
                    // 2. If Yandex failed/not ready, Try AdMob
                    Log.d(TAG, "Yandex Rewarded failed/not ready, trying AdMob...")
                    showAdMobRewarded(
                        onReward = {
                            // AdMob Reward granted
                        },
                        onDone = {
                            // 3. If AdMob failed/not ready, just resume game
                            Log.d(TAG, "AdMob Rewarded also failed/not ready. Resuming game.")
                            notifyAdClosed()
                        }
                    )
                }
            )
        }
    }

    private fun retryLoad(isInterstitial: Boolean, delay: Long) {
        Handler(Looper.getMainLooper()).postDelayed({
            if (isInterstitial) loadYandexInterstitialAd() else loadYandexRewardedAd()
        }, delay)
    }

    override fun onDestroy() {
        bannerAdView?.destroy()
        bannerAdView = null

        admobBannerView?.destroy()
        admobBannerView = null

        yandexInterstitial?.setAdEventListener(null)
        yandexRewarded?.setAdEventListener(null)

        admobInterstitial = null
        admobRewarded = null

        super.onDestroy()
    }
}

@Keep
class WebAppInterface(private val mActivity: MainActivity) {

    @JavascriptInterface
    fun showRewardedAd(adUnitId: String?) = mActivity.showRewardedFromJS(adUnitId)

    @JavascriptInterface
    fun showInterstitial(adUnitId: String?) = mActivity.showInterstitialFromJS(adUnitId)

    @JavascriptInterface
    fun showNativeAd(adUnitId: String?) {
        // Placeholder
    }

    @JavascriptInterface
    fun showAd(adUnitId: String?) = mActivity.showRewardedFromJS(adUnitId)
}
