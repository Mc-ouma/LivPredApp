package com.soccertips.predictx.data.model

/**
 * Announcement model with localization support.
 *
 * For localized announcements in Firebase Remote Config, use this JSON structure:
 * ```json
 * {
 *   "id": "announcement_1",
 *   "title": "New Feature!",
 *   "message": "Check out our new predictions!",
 *   "titles": {
 *     "en": "New Feature!",
 *     "es": "¡Nueva Función!",
 *     "pt": "Nova Funcionalidade!",
 *     "fr": "Nouvelle Fonctionnalité!",
 *     "de": "Neue Funktion!",
 *     "it": "Nuova Funzionalità!",
 *     "ar": "ميزة جديدة!",
 *     "tr": "Yeni Özellik!",
 *     "sw": "Kipengele Kipya!"
 *   },
 *   "messages": {
 *     "en": "Check out our new predictions!",
 *     "es": "¡Mira nuestras nuevas predicciones!",
 *     "pt": "Confira nossas novas previsões!",
 *     "fr": "Découvrez nos nouvelles prédictions!",
 *     "de": "Entdecke unsere neuen Vorhersagen!",
 *     "it": "Scopri le nostre nuove previsioni!",
 *     "ar": "اطلع على توقعاتنا الجديدة!",
 *     "tr": "Yeni tahminlerimize göz atın!",
 *     "sw": "Angalia utabiri wetu mpya!"
 *   },
 *   "actionTexts": {
 *     "en": "Learn More",
 *     "es": "Más Información",
 *     ...
 *   },
 *   "type": "INFO",
 *   "actionUrl": "https://...",
 *   "isVisible": true,
 *   "priority": 10
 * }
 * ```
 */
data class Announcement(
    val id: String = "",
    // Default fallback values (used if localized version not found)
    val title: String = "",
    val message: String = "",
    val actionText: String? = null,
    // Localized versions (language code -> text)
    val titles: Map<String, String>? = null,
    val messages: Map<String, String>? = null,
    val actionTexts: Map<String, String>? = null,
    // Other fields
    val type: AnnouncementType = AnnouncementType.INFO,
    val actionUrl: String? = null,
    val isVisible: Boolean = false,
    val priority: Int = 0 // Higher priority shows first
) {
    /**
     * Get the localized title based on language code.
     * Falls back to default title if localized version not found.
     */
    fun getLocalizedTitle(languageCode: String): String {
        return titles?.get(languageCode)
            ?: titles?.get("en") // Fallback to English
            ?: title // Fallback to default
    }

    /**
     * Get the localized message based on language code.
     * Falls back to default message if localized version not found.
     */
    fun getLocalizedMessage(languageCode: String): String {
        return messages?.get(languageCode)
            ?: messages?.get("en") // Fallback to English
            ?: message // Fallback to default
    }

    /**
     * Get the localized action text based on language code.
     * Falls back to default action text if localized version not found.
     */
    fun getLocalizedActionText(languageCode: String): String? {
        return actionTexts?.get(languageCode)
            ?: actionTexts?.get("en") // Fallback to English
            ?: actionText // Fallback to default
    }
}

enum class AnnouncementType {
    INFO, // Blue info icon
    WARNING, // Yellow warning icon
    ERROR, // Red error icon
    SUCCESS, // Green success icon
    UPDATE // Purple update icon
}
