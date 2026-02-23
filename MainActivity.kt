package com.DefaultCompany.Tahmin11

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
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

class MainActivity : AppCompatActivity() {

    private lateinit var myWebView: WebView

    // -------------------- YANDEX ADS --------------------
    private var yandexInterstitial: InterstitialAd? = null
    private var yandexRewarded: RewardedAd? = null

    private var yandexInterstitialLoader: InterstitialAdLoader? = null
    private var yandexRewardedLoader: RewardedAdLoader? = null

    private var bannerAdView: BannerAdView? = null

    // Yandex Reklam Kimlikleri
    private val Y_INTERSTITIAL_ID = "R-M-18543851-2"
    private val Y_REWARDED_ID = "R-M-18543851-1"
    private val Y_BANNER_ID = "R-M-18543851-3" // varsa doldur

    // -------------------- ADMOB ADS --------------------
    private val ADMOB_INTERSTITIAL_ID = "ca-app-pub-5879474591831999/9656400021"
    private val ADMOB_REWARDED_ID = "ca-app-pub-5879474591831999/4862883127"

    private var admobInterstitial: GInterstitialAd? = null
    private var admobRewarded: GRewardedAd? = null

    private val TAG = "AdsMix"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 0) Loader kurulumları
        setupYandexInterstitial()
        setupYandexRewarded()

        // 1) SDK init
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

        // 2) WebView
        myWebView = findViewById(R.id.webview)
        myWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
        }
        myWebView.addJavascriptInterface(WebAppInterface(this), "Android")
        myWebView.webViewClient = WebViewClient()
        myWebView.webChromeClient = WebChromeClient()
        myWebView.loadUrl("file:///android_asset/index.html")

        // 3) Banner (Yandex - opsiyonel)
        setupYandexBanner()

        // Back
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (myWebView.canGoBack()) myWebView.goBack() else finish()
            }
        })
    }

    // -------------------- YANDEX BANNER --------------------
    private fun setupYandexBanner() {
        if (Y_BANNER_ID.isBlank() || Y_BANNER_ID == "R-M-18543851-3") return

        val container = findViewById<FrameLayout>(R.id.adContainer) ?: return
        bannerAdView = BannerAdView(this).apply {
            setAdUnitId(Y_BANNER_ID)
            setAdSize(BannerAdSize.stickySize(this@MainActivity, 320))
        }

        container.removeAllViews()
        container.addView(bannerAdView)

        val request = AdRequest.Builder().build()
        bannerAdView?.loadAd(request)
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

    private fun loadYandexInterstitialAd(adUnitId: String = Y_INTERSTITIAL_ID) {
        val config = AdRequestConfiguration.Builder(adUnitId).build()
        yandexInterstitialLoader?.loadAd(config)
    }

    private fun showYandexInterstitial(onDone: (() -> Unit)? = null, adUnitId: String? = null) {
        val unitId = adUnitId?.takeIf { it.isNotBlank() } ?: Y_INTERSTITIAL_ID

        yandexInterstitial?.let { ad ->
            ad.setAdEventListener(object : InterstitialAdEventListener {
                override fun onAdShown() {
                    Log.d(TAG, "Yandex Interstitial Shown")
                }

                override fun onAdFailedToShow(adError: AdError) {
                    Log.e(TAG, "Yandex Interstitial failed to show: ${adError.description}")
                    yandexInterstitial = null
                    loadYandexInterstitialAd(unitId)
                    onDone?.invoke()
                }

                override fun onAdDismissed() {
                    Log.d(TAG, "Yandex Interstitial dismissed")
                    yandexInterstitial = null
                    loadYandexInterstitialAd(unitId)
                    onDone?.invoke()
                }

                override fun onAdClicked() {}
                override fun onAdImpression(impressionData: ImpressionData?) {}
            })
            ad.show(this)
        } ?: run {
            Log.d(TAG, "Yandex Interstitial not ready")
            loadYandexInterstitialAd(unitId)
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

    private fun loadYandexRewardedAd(adUnitId: String = Y_REWARDED_ID) {
        val config = AdRequestConfiguration.Builder(adUnitId).build()
        yandexRewardedLoader?.loadAd(config)
    }

    private fun showYandexRewarded(
        onReward: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null,
        adUnitId: String? = null
    ) {
        val unitId = adUnitId?.takeIf { it.isNotBlank() } ?: Y_REWARDED_ID

        yandexRewarded?.let { ad ->
            ad.setAdEventListener(object : RewardedAdEventListener {
                override fun onAdShown() {}

                override fun onAdFailedToShow(adError: AdError) {
                    Log.e(TAG, "Yandex Rewarded failed to show: ${adError.description}")
                    yandexRewarded = null
                    loadYandexRewardedAd(unitId)
                    onDone?.invoke()
                }

                override fun onAdDismissed() {
                    yandexRewarded = null
                    myWebView.evaluateJavascript("if(window.onAdClosed) window.onAdClosed();", null)
                    loadYandexRewardedAd(unitId)
                    onDone?.invoke()
                }

                override fun onAdClicked() {}
                override fun onAdImpression(impressionData: ImpressionData?) {}

                override fun onRewarded(reward: Reward) {
                    myWebView.evaluateJavascript(
                        "if(window.AdManager && AdManager.onReward) AdManager.onReward(); if(window.grantReward) window.grantReward();",
                        null
                    )
                    Log.d(TAG, "Yandex reward: ${reward.amount} ${reward.type}")
                    onReward?.invoke()
                }
            })
            ad.show(this)
        } ?: run {
            Log.d(TAG, "Yandex Rewarded not ready")
            loadYandexRewardedAd(unitId)
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
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: GAdError) {
                            admobInterstitial = null
                            loadAdMobInterstitial()
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
            onDone?.invoke()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                admobInterstitial = null
                loadAdMobInterstitial()
                onDone?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(adError: GAdError) {
                admobInterstitial = null
                loadAdMobInterstitial()
                onDone?.invoke()
            }
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
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: GAdError) {
                            admobRewarded = null
                            loadAdMobRewarded()
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

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                admobRewarded = null
                myWebView.evaluateJavascript("if(window.onAdClosed) window.onAdClosed();", null)
                loadAdMobRewarded()
                onDone?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(adError: GAdError) {
                admobRewarded = null
                myWebView.evaluateJavascript("if(window.onAdClosed) window.onAdClosed();", null)
                loadAdMobRewarded()
                onDone?.invoke()
            }
        }

        ad.show(this) { rewardItem ->
            // JS reward callback
            myWebView.evaluateJavascript(
                "if(window.AdManager && AdManager.onReward) AdManager.onReward(); if(window.grantReward) window.grantReward();",
                null
            )
            Log.d(TAG, "AdMob reward: ${rewardItem.amount} ${rewardItem.type}")
            onReward?.invoke(rewardItem)
        }
    }

    // -------------------- JS'DEN ÇAĞIRILAN (FALLBACK'Lİ) --------------------
    fun showInterstitialFromJS(adUnitId: String? = null) {
        runOnUiThread {
            // Önce Yandex dene, yoksa AdMob
            showYandexInterstitial(
                onDone = {
                    // Yandex gösteremediyse AdMob dene
                    showAdMobInterstitial(onDone = null)
                },
                adUnitId = adUnitId
            )
        }
    }

    fun showRewardedFromJS(adUnitId: String? = null) {
        runOnUiThread {
            // Önce Yandex dene, yoksa AdMob
            showYandexRewarded(
                onReward = { /* Yandex reward event zaten JS'e gönderiliyor */ },
                onDone = {
                    // Yandex gösteremediyse AdMob dene
                    showAdMobRewarded(
                        onReward = { /* AdMob reward event de JS'e gönderiliyor */ },
                        onDone = null
                    )
                },
                adUnitId = adUnitId
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

        yandexInterstitial?.setAdEventListener(null)
        yandexRewarded?.setAdEventListener(null)

        admobInterstitial = null
        admobRewarded = null

        super.onDestroy()
    }
}

@Keep
class WebAppInterface(private val mActivity: MainActivity) {

    /**
     * JS: Android.showRewardedAd("R-M-XXXX-1")
     * (Yandex hazır değilse AdMob'a düşer)
     */
    @JavascriptInterface
    fun showRewardedAd(adUnitId: String?) = mActivity.showRewardedFromJS(adUnitId)

    /**
     * JS: Android.showInterstitial("R-M-XXXX-2")
     * (Yandex hazır değilse AdMob'a düşer)
     */
    @JavascriptInterface
    fun showInterstitial(adUnitId: String?) = mActivity.showInterstitialFromJS(adUnitId)

    @JavascriptInterface
    fun showNativeAd(adUnitId: String?) {
        // Native yok, şimdilik stub
    }

    /**
     * Eski çağrı desteği
     */
    @JavascriptInterface
    fun showAd(adUnitId: String?) = mActivity.showRewardedFromJS(adUnitId)
}
