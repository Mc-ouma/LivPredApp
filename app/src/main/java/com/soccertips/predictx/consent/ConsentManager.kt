package com.soccertips.predictx.consent

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.soccertips.predictx.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages user consent for ads, analytics, and crashlytics in the app.
 * Handles the User Messaging Platform (UMP) SDK for GDPR compliance.
 */
@Singleton
class ConsentManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAnalytics: FirebaseAnalytics
) {
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private var consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)
    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance()


    private val _consentState = MutableStateFlow<ConsentState>(ConsentState.Unknown)
    val consentState: StateFlow<ConsentState> = _consentState.asStateFlow()

    // Keys for storing consent preferences
    private companion object {
        const val KEY_PERSONALIZED_ADS_CONSENT = "personalized_ads_consent"
        const val KEY_NON_PERSONALIZED_ADS_CONSENT = "non_personalized_ads_consent"
        const val KEY_ANALYTICS_CONSENT = "analytics_consent"
        const val KEY_CRASHLYTICS_CONSENT = "crashlytics_consent"
        const val KEY_CONSENT_FORM_SHOWN = "consent_form_shown"
    }

    init {
        // Initialize consent state from stored preferences
        val personalizedAdsConsent = preferences.getBoolean(KEY_PERSONALIZED_ADS_CONSENT, false)
        val nonPersonalizedAdsConsent = preferences.getBoolean(KEY_NON_PERSONALIZED_ADS_CONSENT, false)
        val analyticsConsent = preferences.getBoolean(KEY_ANALYTICS_CONSENT, false)
        val crashlyticsConsent = preferences.getBoolean(KEY_CRASHLYTICS_CONSENT, false)
        val formShown = preferences.getBoolean(KEY_CONSENT_FORM_SHOWN, false)

        _consentState.value = if (!formShown) {
            ConsentState.Unknown
        } else {
            ConsentState.Detailed(
                personalizedAdsConsent = personalizedAdsConsent,
                nonPersonalizedAdsConsent = nonPersonalizedAdsConsent,
                analyticsConsent = analyticsConsent,
                crashlyticsConsent = crashlyticsConsent
            )
        }

        // Apply stored consent settings immediately
        applyCurrentConsentSettings()
    }

    /**
     * Request user consent using the UMP SDK
     * Will show the consent form if required and not shown before
     */
    fun requestConsentForm(activity: Activity, onConsentGathered: () -> Unit = {}) {
        // Set tag for underage of consent
        val debugSettings = if (BuildConfig.DEBUG) {
            ConsentDebugSettings.Builder(context)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .build()
        } else null

        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .apply {
                if (debugSettings != null) setConsentDebugSettings(debugSettings)
            }
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                // Consent info updated successfully
                if (consentInformation.isConsentFormAvailable) {
                    loadAndShowConsentForm(activity, onConsentGathered)
                } else {
                    // Consent form not available, use defaults
                    setDefaultConsent()
                    onConsentGathered()
                }
            },
            { requestError ->
                // Consent info update failed
                Timber.e("Error updating consent info: ${requestError.errorCode} - ${requestError.message}")
                setDefaultConsent()
                onConsentGathered()
            }
        )
    }

    private fun loadAndShowConsentForm(activity: Activity, onConsentGathered: () -> Unit) {
        UserMessagingPlatform.loadConsentForm(
            context,
            { consentForm ->
                if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                    showConsentForm(activity, consentForm, onConsentGathered)
                } else {
                    updateConsentFromUMP()
                    onConsentGathered()
                }
            },
            { formError ->
                Timber.e("Error loading consent form: ${formError.errorCode} - ${formError.message}")
                setDefaultConsent()
                onConsentGathered()
            }
        )
    }

    private fun showConsentForm(activity: Activity, consentForm: ConsentForm, onConsentGathered: () -> Unit) {
        consentForm.show(
            activity
        ) { formError ->
            if (formError != null) {
                Timber.e("Error showing consent form: ${formError.errorCode} - ${formError.message}")
                setDefaultConsent()
            } else {
                updateConsentFromUMP()
                preferences.edit {
                    putBoolean(KEY_CONSENT_FORM_SHOWN, true)
                }
            }
            onConsentGathered()
        }
    }

    private fun updateConsentFromUMP() {
        val canShowPersonalizedAds = consentInformation.canRequestAds() &&
                consentInformation.canRequestAds()
        val canShowAds = consentInformation.canRequestAds()

        preferences.edit {
            putBoolean(KEY_PERSONALIZED_ADS_CONSENT, canShowPersonalizedAds)
            putBoolean(KEY_NON_PERSONALIZED_ADS_CONSENT, canShowAds && !canShowPersonalizedAds)
            putBoolean(KEY_ANALYTICS_CONSENT, canShowPersonalizedAds) // Analytics follows personalized ads consent
            putBoolean(KEY_CRASHLYTICS_CONSENT, canShowPersonalizedAds) // Crashlytics follows personalized ads consent
        }

        _consentState.value = ConsentState.Detailed(
            personalizedAdsConsent = canShowPersonalizedAds,
            nonPersonalizedAdsConsent = canShowAds && !canShowPersonalizedAds,
            analyticsConsent = canShowPersonalizedAds,
            crashlyticsConsent = canShowPersonalizedAds
        )

        applyCurrentConsentSettings()
    }

    private fun setDefaultConsent() {
        // By default, assume no consent if consent form cannot be shown
        preferences.edit {
            putBoolean(KEY_PERSONALIZED_ADS_CONSENT, false)
            putBoolean(KEY_NON_PERSONALIZED_ADS_CONSENT, true) // Default to non-personalized ads
            putBoolean(KEY_ANALYTICS_CONSENT, false)
            putBoolean(KEY_CRASHLYTICS_CONSENT, false)
        }

        _consentState.value = ConsentState.Detailed(
            personalizedAdsConsent = false,
            nonPersonalizedAdsConsent = true,
            analyticsConsent = false,
            crashlyticsConsent = false
        )

        applyCurrentConsentSettings()
    }

    /**
     * Manually update consent settings for a specific privacy category
     */
    fun updateConsent(
        personalizedAdsConsent: Boolean,
        nonPersonalizedAdsConsent: Boolean,
        analyticsConsent: Boolean,
        crashlyticsConsent: Boolean
    ) {
        preferences.edit {
            putBoolean(KEY_PERSONALIZED_ADS_CONSENT, personalizedAdsConsent)
            putBoolean(KEY_NON_PERSONALIZED_ADS_CONSENT, nonPersonalizedAdsConsent)
            putBoolean(KEY_ANALYTICS_CONSENT, analyticsConsent)
            putBoolean(KEY_CRASHLYTICS_CONSENT, crashlyticsConsent)
            putBoolean(KEY_CONSENT_FORM_SHOWN, true)
        }

        _consentState.value = ConsentState.Detailed(
            personalizedAdsConsent = personalizedAdsConsent,
            nonPersonalizedAdsConsent = nonPersonalizedAdsConsent,
            analyticsConsent = analyticsConsent,
            crashlyticsConsent = crashlyticsConsent
        )

        applyCurrentConsentSettings()
    }

    /**
     * Reset consent information - will trigger a new consent flow
     */
    fun resetConsent() {
        consentInformation.reset()
        preferences.edit {
            putBoolean(KEY_PERSONALIZED_ADS_CONSENT, false)
            putBoolean(KEY_NON_PERSONALIZED_ADS_CONSENT, false)
            putBoolean(KEY_ANALYTICS_CONSENT, false)
            putBoolean(KEY_CRASHLYTICS_CONSENT, false)
            putBoolean(KEY_CONSENT_FORM_SHOWN, false)
        }
        _consentState.value = ConsentState.Unknown

        // Apply settings immediately
        applyCurrentConsentSettings()
    }

    /**
     * Check if consent form needs to be shown
     */
    fun shouldShowConsentForm(): Boolean {
        return consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED ||
                !preferences.getBoolean(KEY_CONSENT_FORM_SHOWN, false)
    }

    /**
     * Apply the current consent settings to analytics and ad systems
     */
    private fun applyCurrentConsentSettings() {
        val personalizedAdsConsent = preferences.getBoolean(KEY_PERSONALIZED_ADS_CONSENT, false)
        val analyticsConsent = preferences.getBoolean(KEY_ANALYTICS_CONSENT, false)
        val crashlyticsConsent = preferences.getBoolean(KEY_CRASHLYTICS_CONSENT, false)

        // Configure Firebase Analytics consent
        firebaseAnalytics.setAnalyticsCollectionEnabled(analyticsConsent)

        // Configure Crashlytics consent
        crashlytics.setCrashlyticsCollectionEnabled(crashlyticsConsent)

        // Configure Ad personalization
        val requestConfiguration = MobileAds.getRequestConfiguration().toBuilder()
            .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
            .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)

        if (!personalizedAdsConsent) {
            // Set request to non-personalized ads
            requestConfiguration.setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        }

        MobileAds.setRequestConfiguration(requestConfiguration.build())

        // Log the current consent state
        Timber.d("Consent settings applied: PersonalizedAds=$personalizedAdsConsent, NonPersonalizedAds=${preferences.getBoolean(KEY_NON_PERSONALIZED_ADS_CONSENT, false)}, Analytics=$analyticsConsent, Crashlytics=$crashlyticsConsent")
    }

    /**
     * Get whether any ads can be shown based on user consent
     */
    fun canShowAds(): Boolean {
        return preferences.getBoolean(KEY_PERSONALIZED_ADS_CONSENT, false) ||
                preferences.getBoolean(KEY_NON_PERSONALIZED_ADS_CONSENT, false)
    }

    /**
     * Check if personalized ads can be shown
     */
    fun canShowPersonalizedAds(): Boolean {
        return preferences.getBoolean(KEY_PERSONALIZED_ADS_CONSENT, false)
    }

    /**
     * Opens the privacy options dialog (if available)
     */
    fun showPrivacyOptionsDialog(activity: Activity, onPrivacyOptionsDialogDismissed: () -> Unit = {}) {
        if (consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED) {

            UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
                if (formError != null) {
                    Timber.e("Privacy options error: ${formError.errorCode} - ${formError.message}")
                } else {
                    updateConsentFromUMP()
                }
                onPrivacyOptionsDialogDismissed()
            }
        } else {
            // If the privacy options form is not required, show the consent form if available
            requestConsentForm(activity, onPrivacyOptionsDialogDismissed)
        }
    }
}

/**
 * Represents the user's consent state
 */
sealed class ConsentState {
    object Unknown : ConsentState()
    object FullConsent : ConsentState()
    object NoConsent : ConsentState()
    data class PartialConsent(val adsConsent: Boolean, val analyticsConsent: Boolean) : ConsentState()
    data class Detailed(
        val personalizedAdsConsent: Boolean,
        val nonPersonalizedAdsConsent: Boolean,
        val analyticsConsent: Boolean,
        val crashlyticsConsent: Boolean
    ) : ConsentState()
}
