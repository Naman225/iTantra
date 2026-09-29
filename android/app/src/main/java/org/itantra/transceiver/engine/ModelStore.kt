package org.itantra.transceiver.engine

import android.content.Context
import android.util.Log
import org.vosk.Model
import java.io.File

enum class ModelStatus {
    READY,
    NOT_INSTALLED,
    LOADING
}

/**
 * iTantra ModelStore (I-01 & I-02).
 * Manages per-language acoustic model packs (models/stt/hi, models/stt/en, models/stt/ta).
 * Keeps only one active model in memory at a time.
 */
object ModelStore {
    private const val TAG = "iTantra-ModelStore"

    private var activeLangCode: String? = null
    private var activeModel: Model? = null

    val MODEL_URLS = mapOf(
        "hi" to "https://alphacephei.com/vosk/models/vosk-model-small-hi-0.22.zip",
        "en" to "https://alphacephei.com/vosk/models/vosk-model-small-en-in-0.4.zip",
        "gu" to "https://alphacephei.com/vosk/models/vosk-model-small-gu-0.42.zip"
    )

    fun findModelRoot(dir: File): File {
        if (File(dir, "am").exists() || File(dir, "conf").exists() || File(dir, "model.conf").exists()) {
            return dir
        }
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory && (File(child, "am").exists() || File(child, "conf").exists())) {
                return child
            }
        }
        return dir
    }

    /**
     * Checks if a model exists on-device for the given language code.
     */
    fun isModelInstalled(context: Context, langCode: String): Boolean {
        val modelDir = getModelDir(context, langCode)
        val legacyDir = File(context.filesDir, "models/vosk-small")
        if (modelDir.exists() && modelDir.isDirectory) {
            val root = findModelRoot(modelDir)
            if (File(root, "am").exists() || File(root, "conf").exists() || (modelDir.list()?.isNotEmpty() == true)) {
                return true
            }
        }
        return (langCode == "hi" && legacyDir.exists() && legacyDir.isDirectory)
    }

    /**
     * Returns the model status for the requested language.
     */
    fun getModelStatus(context: Context, langCode: String): ModelStatus {
        return if (activeLangCode == langCode && activeModel != null) {
            ModelStatus.READY
        } else if (isModelInstalled(context, langCode)) {
            ModelStatus.READY
        } else {
            ModelStatus.NOT_INSTALLED
        }
    }

    /**
     * Unzips and imports an acoustic model package (.zip) into models/stt/[langCode].
     */
    fun importModelZip(context: Context, inputStream: java.io.InputStream, langCode: String): Boolean {
        val targetDir = getModelDir(context, langCode)
        if (!targetDir.exists()) targetDir.mkdirs()

        return try {
            val zis = java.util.zip.ZipInputStream(inputStream)
            var entry = zis.nextEntry
            val buffer = ByteArray(8192)

            while (entry != null) {
                val newFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    val fos = java.io.FileOutputStream(newFile)
                    var len: Int
                    while (zis.read(buffer).also { len = it } > 0) {
                        fos.write(buffer, 0, len)
                    }
                    fos.close()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
            zis.close()
            Log.i(TAG, "Successfully imported model pack for '$langCode' into ${targetDir.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import model pack for '$langCode': ${e.message}")
            false
        }
    }

    /**
     * Returns the dedicated directory for a language pack.
     */
    fun getModelDir(context: Context, langCode: String): File {
        return File(context.filesDir, "models/stt/$langCode")
    }

    /**
     * Loads the model for the requested language. Drops previous model to conserve RAM (< 30 MB).
     */
    @Synchronized
    fun getOrLoadModel(context: Context, langCode: String): Model? {
        if (activeLangCode == langCode && activeModel != null) {
            return activeModel
        }

        // Close previous model
        activeModel = null
        activeLangCode = null

        val dir = getModelDir(context, langCode)
        // Also check fallback legacy directory for backward compatibility
        val fallbackDir = File(context.filesDir, "models/vosk-small")

        val targetDir = when {
            dir.exists() && dir.isDirectory -> findModelRoot(dir)
            langCode == "hi" && fallbackDir.exists() -> findModelRoot(fallbackDir)
            else -> null
        }

        if (targetDir != null) {
            return try {
                Log.i(TAG, "Loading Vosk model for '$langCode' from ${targetDir.absolutePath}")
                val model = Model(targetDir.absolutePath)
                activeModel = model
                activeLangCode = langCode
                model
            } catch (e: Exception) {
                Log.e(TAG, "Error loading model for $langCode: ${e.message}")
                null
            }
        } else {
            Log.w(TAG, "No offline model pack installed for '$langCode' at ${dir.absolutePath}")
            return null
        }
    }

    @Synchronized
    fun unload() {
        activeModel = null
        activeLangCode = null
    }
}
