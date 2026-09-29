package org.itantra.transceiver.protocol

/**
 * iTantra Tactical & Disaster Phrase Codebook (I-14).
 * Maps tactical emergency phrases to a 2-byte ID for cross-language translation and 12-byte micro-packets.
 */
object PhraseCodebook {

    data class PhraseEntry(
        val id: Int,
        val category: String,
        val translations: Map<String, String> // langCode -> translated text
    )

    private val phrases = listOf(
        PhraseEntry(
            id = 1,
            category = "SOS",
            translations = mapOf(
                "hi" to "आपातकालीन स्थिति! हमें तुरंत सहायता चाहिए।",
                "en" to "Emergency SOS! Immediate assistance required.",
                "ta" to "அவசர உதவி தேவை! உடனே வாருங்கள்.",
                "te" to "అత్యవసర సహాయం కావాలి! వెంటనే రండి.",
                "gu" to "કટોકટી! અમને તાત્કાલિક મદદની જરૂર છે.",
                "mr" to "आणीबाणी! आम्हाला त्वरित मदतीची आवश्यकता आहे.",
                "kn" to "ತುರ್ತು ಪರಿಸ್ಥಿತಿ! ನಮಗೆ ತಕ್ಷಣ ಸಹಾಯ ಬೇಕು.",
                "ml" to "അടിയന്തര സാഹചര്യം! ഉടൻ സഹായം വേണം.",
                "bn" to "জরুরি অবস্থা! আমাদের অবিলম্বে সাহায্য প্রয়োজন।",
                "or" to "ଜରୁରୀକାଳୀନ ପରିସ୍ଥିତି! ତୁରନ୍ତ ସାହାଯ୍ୟ ଦରକାର।"
            )
        ),
        PhraseEntry(
            id = 2,
            category = "ALERT",
            translations = mapOf(
                "hi" to "खतरा! बाढ़ का पानी तेजी से बढ़ रहा है।",
                "en" to "Warning! Flood water is rising rapidly.",
                "ta" to "எச்சரிக்கை! வெள்ள நீர் வேகமாக உயர்கிறது.",
                "te" to "హెచ్చరిక! వరద నీరు వేగంగా పెరుగుతోంది.",
                "gu" to "ચેતવણી! પૂરનું પાણી ઝડપથી વધી રહ્યું છે.",
                "mr" to "धोका! पुराचे पाणी वेगाने वाढत आहे.",
                "kn" to "ಎಚ್ಚರಿಕೆ! ಪ್ರವಾಹದ ನೀರು ವೇಗವಾಗಿ ಏರುತ್ತಿದೆ.",
                "ml" to "മുന്നറിയിപ്പ്! വെള്ളപ്പൊക്കം വേഗത്തിൽ ഉയരുന്നു.",
                "bn" to "সতর্কতা! বন্যার জল দ্রুত বাড়ছে।",
                "or" to "ଚେତାବନୀ! ବନ୍ୟା ଜଳ ଦ୍ରୁତ ଗତିରେ ବଢୁଛି।"
            )
        ),
        PhraseEntry(
            id = 3,
            category = "TACTICAL",
            translations = mapOf(
                "hi" to "रास्ता बंद है, वैकल्पिक मार्ग लें।",
                "en" to "Road blocked, proceed via alternate route.",
                "ta" to "பாதை அடைக்கப்பட்டுள்ளது, மாற்று வழியில் செல்லவும்.",
                "te" to "రహదారి మూసివేయబడింది, ప్రత్యామ్నాయ మార్గంలో వెళ్ళండి.",
                "gu" to "રસ્તો બંધ છે, વૈકલ્પિક માર્ગ લો.",
                "mr" to "रस्ता बंद आहे, पर्यायी मार्गाने जा.",
                "kn" to "ರಸ್ತೆ ಬಂದ್ ಆಗಿದೆ, ಪರ್ಯಾಯ ಮಾರ್ಗ ಬಳಸಿ.",
                "ml" to "വഴി തടസ്സപ്പെട്ടു, മറ്റൊരു വഴി ഉപയോഗിക്കുക.",
                "bn" to "রাস্তা বন্ধ, বিকল্প পথ ব্যবহার করুন।",
                "or" to "ରାସ୍ତା ବନ୍ଦ ଅଛି, ବିକଳ୍ପ ରାସ୍ତା ଦେଇ ଯାଆନ୍ତୁ।"
            )
        ),
        PhraseEntry(
            id = 4,
            category = "MEDICAL",
            translations = mapOf(
                "hi" to "घायल व्यक्ति मिला है, मेडिकल टीम भेजें।",
                "en" to "Casualty located, dispatch medical team immediately.",
                "ta" to "காயமடைந்தவர் கிடைத்தார், மருத்துவக் குழுவை அனுப்பவும்.",
                "te" to "గాయపడిన వ్యక్తి ఉన్నారు, వైద్య బృందాన్ని పంపండి.",
                "gu" to "ઈજાગ્રસ્ત વ્યક્તિ મળ્યો છે, તબીબી ટીમ મોકલો.",
                "mr" to "जखमी व्यक्ती सापडली आहे, वैद्यकीय पथक पाठवा.",
                "kn" to "ಗಾಯಾಳು ಸಿಕ್ಕಿದ್ದಾನೆ, ವೈದ್ಯಕೀಯ ತಂಡ ಕಳುಹಿಸಿ.",
                "ml" to "പരിക്കേറ്റയാളെ കണ്ടെത്തി, മെഡിക്കൽ ടീമിനെ അയക്കുക.",
                "bn" to "আহত ব্যক্তি পাওয়া গেছে, মেডিকেল টিম পাঠান।",
                "or" to "ଆହତ ବ୍ୟକ୍ତି ମିଳିଛନ୍ତି, ଡାକ୍ତରୀ ଦଳ ପଠାନ୍ତୁ।"
            )
        ),
        PhraseEntry(
            id = 5,
            category = "STATUS",
            translations = mapOf(
                "hi" to "हम सुरक्षित स्थान पर पहुंच गए हैं।",
                "en" to "We have reached the safe shelter location.",
                "ta" to "நாங்கள் பாதுகாப்பான இடத்திற்கு வந்துவிட்டோம்.",
                "te" to "మేము సురక్షిత ప్రాంతానికి చేరుకున్నాము.",
                "gu" to "અમે સુરક્ષિત સ્થળે પહોંચી ગયા છીએ.",
                "mr" to "आम्ही सुरक्षित ठिकाणी पोहोचलो आहोत.",
                "kn" to "ನಾವು ಸುರಕ್ಷಿತ ಸ್ಥಳಕ್ಕೆ ತಲುಪಿದ್ದೇವೆ.",
                "ml" to "ഞങ്ങൾ സുരക്ഷിത സ്ഥാനത്ത് എത്തിച്ചേർന്നു.",
                "bn" to "আমরা নিরাপদ স্থানে পৌঁছেছি।",
                "or" to "ଆମେ ସୁରକ୍ଷିତ ସ୍ଥାନରେ ପହଞ୍ଚିଛୁ।"
            )
        ),
        PhraseEntry(
            id = 6,
            category = "LOGISTICS",
            translations = mapOf(
                "hi" to "पीने का पानी और खाद्य सामग्री की आवश्यकता है।",
                "en" to "Drinking water and food supplies urgently needed.",
                "ta" to "குடிநீரும் உணவும் அவசரமாக தேவைப்படுகிறது.",
                "te" to "త్రాగునీరు మరియు ఆహార సామాగ్రి అత్యవసరం.",
                "gu" to "પીવાનું પાણી અને ખોરાકની તાકીદે જરૂર છે.",
                "mr" to "पिण्याचे पाणी आणि अन्नाची तातडीने गरज आहे.",
                "kn" to "ಕುಡಿಯುವ ನೀರು ಮತ್ತು ಆಹಾರ ತುರ್ತಾಗಿ ಬೇಕಾಗಿದೆ.",
                "ml" to "കുടിവെള്ളവും ഭക്ഷണവും അടിയന്തരമായി ആവശ്യമുണ്ട്.",
                "bn" to "পানীয় জল ও খাবার অবিলম্বে প্রয়োজন।",
                "or" to "ପିଇବା ପାଣି ଏବଂ ଖାଦ୍ୟ ସାମଗ୍ରୀ ତୁରନ୍ତ ଆବଶ୍ୟକ।"
            )
        )
    )

    private val phraseMapById = phrases.associateBy { it.id }

    /**
     * Looks up translated phrase for the recipient language given a phrase ID.
     */
    fun getTranslation(phraseId: Int, targetLangCode: String): String? {
        val entry = phraseMapById[phraseId] ?: return null
        return entry.translations[targetLangCode] ?: entry.translations["en"] ?: entry.translations["hi"]
    }

    /**
     * Finds phrase ID if the spoken text closely matches any known codebook phrase.
     */
    fun findPhraseId(text: String): Int? {
        val normalized = text.trim().lowercase()
        for (phrase in phrases) {
            for ((_, translation) in phrase.translations) {
                val candidate = translation.trim().lowercase()
                if (normalized == candidate || normalized.contains(candidate) || candidate.contains(normalized)) {
                    if (normalized.length > 5) return phrase.id
                }
            }
        }
        return null
    }

    fun getAllPhrases(): List<PhraseEntry> = phrases
}
