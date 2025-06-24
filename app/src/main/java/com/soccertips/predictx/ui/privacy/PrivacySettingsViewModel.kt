package com.soccertips.predictx.ui.privacy

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soccertips.predictx.consent.ConsentManager
import com.soccertips.predictx.consent.ConsentState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val consentManager: ConsentManager
) : ViewModel() {

    val consentState = consentManager.consentState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ConsentState.Unknown
    )

    fun updatePersonalizedAdsConsent(enabled: Boolean) {
        val current = (consentState.value as? ConsentState.Detailed) ?: return

        // If enabling personalized ads, disable non-personalized
        val nonPersonalizedAds = if (enabled) false else current.nonPersonalizedAdsConsent

        consentManager.updateConsent(
            personalizedAdsConsent = enabled,
            nonPersonalizedAdsConsent = nonPersonalizedAds,
            analyticsConsent = current.analyticsConsent,
            crashlyticsConsent = current.crashlyticsConsent
        )
    }

    fun updateNonPersonalizedAdsConsent(enabled: Boolean) {
        val current = (consentState.value as? ConsentState.Detailed) ?: return

        // If enabling non-personalized ads, disable personalized
        val personalizedAds = if (enabled) false else current.personalizedAdsConsent

        consentManager.updateConsent(
            personalizedAdsConsent = personalizedAds,
            nonPersonalizedAdsConsent = enabled,
            analyticsConsent = current.analyticsConsent,
            crashlyticsConsent = current.crashlyticsConsent
        )
    }

    fun updateAnalyticsConsent(enabled: Boolean) {
        val current = (consentState.value as? ConsentState.Detailed) ?: return
        consentManager.updateConsent(
            personalizedAdsConsent = current.personalizedAdsConsent,
            nonPersonalizedAdsConsent = current.nonPersonalizedAdsConsent,
            analyticsConsent = enabled,
            crashlyticsConsent = current.crashlyticsConsent
        )
    }

    fun updateCrashlyticsConsent(enabled: Boolean) {
        val current = (consentState.value as? ConsentState.Detailed) ?: return
        consentManager.updateConsent(
            personalizedAdsConsent = current.personalizedAdsConsent,
            nonPersonalizedAdsConsent = current.nonPersonalizedAdsConsent,
            analyticsConsent = current.analyticsConsent,
            crashlyticsConsent = enabled
        )
    }

    fun resetConsent(activity: Activity) {
        viewModelScope.launch {
            consentManager.resetConsent()
            consentManager.requestConsentForm(activity)
        }
    }

    fun showConsentForm(activity: Activity) {
        consentManager.showPrivacyOptionsDialog(activity)
    }
}
