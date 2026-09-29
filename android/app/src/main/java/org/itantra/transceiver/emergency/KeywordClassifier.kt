package org.itantra.transceiver.emergency

/**
 * Intelligent 3-Tier Keyword Classifier (I-06).
 * Separates high-confidence distress triggers (Red SOS) from tactical warning words (Yellow Alert).
 * Uses whole-word boundary matching after normalization to prevent false triggers (e.g., 'helpful' matching 'help').
 */
object KeywordClassifier {

    // High-confidence SOS triggers
    private val HIGH_CONFIDENCE_SOS = setOf(
        "sos", "s.o.s", "mayday", "distress", "evacuate",
        "bachao", "bachao bachao", "madad karo", "sahayata karo",
        "एसओएस", "एस ओ एस", "बचाओ", "बचाओ बचाओ", "आपातकाल", "आपातकालीन",
        "காப்பாற்றுங்கள்", "காப்பாது",
        "కాపాడండి", "రక్షించండి",
        "বাঁচাও", "বাঁচাও বাঁচাও",
        "બચાવો", "બચાવો બચાવો",
        "ಉಳಿಸಿ", "ಕಾಪಾಡಿ",
        "രക്ഷിക്കൂ",
        "ବଞ୍ଚାଅ", "ରକ୍ଷାକର"
    )

    // Tactical Alert / Hazard warnings (Yellow)
    private val TACTICAL_ALERT_KEYWORDS = setOf(
        "alert", "alerts", "warning", "warnings", "danger", "dangerous", "caution", "hazard", "threat", "rescue", "help",
        "khatra", "khatre", "khatron", "khatarnak", "chetawani", "savdhan", "dhoka", "hoshra", "bipod", "jokham", "apaya",
        "खतरा", "खतरे", "खतरों", "खतरनाक", "अलर्ट", "चेतावनी", "सावधान", "सावधानी", "सतर्क", "मदद", "सहायता",
        "எச்சரிக்கை", "கவனம்", "அபாயம்", "உதவி",
        "ప్రమాదం", "హెచ్చరిక", "జాగ్రత్త", "సహాయం",
        "বিপদ", "সতর্কতা", "হুঁশিয়ার", "সাহায্য",
        "ખતરો", "ચેતવણી", "સાવધાન", "જોખમ", "મદદ",
        "ಅಪಾಯ", "ಎಚ್ಚರಿಕೆ", "ಸಹಾಯ",
        "അപകടം", "ജാഗ്രത", "മുന്നറിയിപ്പ്", "സഹായം",
        "ବିପଦ", "ଚେତାବନୀ", "ସତର୍କତା", "ସାହାଯ୍ୟ"
    )

    data class ClassificationResult(
        val isEmergencySos: Boolean,
        val isTacticalAlert: Boolean,
        val matchedKeyword: String? = null
    )

    fun classify(text: String): ClassificationResult {
        val normalized = text.lowercase()
            .replace("।", " ")
            .replace(".", " ")
            .replace(",", " ")
            .replace("!", " ")
            .replace("?", " ")
            .replace("-", " ")
            .replace("_", " ")
            .replace(":", " ")
            .replace(";", " ")

        val words = normalized.split("\\s+".toRegex()).filter { it.isNotBlank() }

        // 1. Check multi-word high confidence triggers
        for (sos in HIGH_CONFIDENCE_SOS) {
            if (sos.contains(" ")) {
                if (normalized.contains(sos)) {
                    return ClassificationResult(isEmergencySos = true, isTacticalAlert = false, matchedKeyword = sos)
                }
            }
        }

        // 2. Check single-word SOS triggers with exact word matching
        for (word in words) {
            if (HIGH_CONFIDENCE_SOS.contains(word)) {
                return ClassificationResult(isEmergencySos = true, isTacticalAlert = false, matchedKeyword = word)
            }
        }

        // 3. Check tactical alert keywords
        for (word in words) {
            if (TACTICAL_ALERT_KEYWORDS.contains(word)) {
                return ClassificationResult(isEmergencySos = false, isTacticalAlert = true, matchedKeyword = word)
            }
        }

        return ClassificationResult(isEmergencySos = false, isTacticalAlert = false)
    }
}
