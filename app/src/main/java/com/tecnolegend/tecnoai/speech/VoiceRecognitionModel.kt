package com.tecnolegend.tecnoai.speech

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.abs
import kotlin.math.sqrt

private val Context.voiceProfileStore by preferencesDataStore(name = "tecno_voice_profile")

data class VoiceProfileData(
    val isEnrolled: Boolean = false,
    val speakerName: String = "User",
    val averagePitchHz: Float = 0f,
    val averageEnergy: Float = 0f,
    val spectralCentroid: Float = 0f,
    val totalSamplesCaptured: Int = 0,
    val enrollmentDateTimestamp: Long = 0L,
    val confidenceThreshold: Float = 0.65f
)

class VoiceRecognitionModel(private val context: Context) {

    companion object {
        val KEY_ENROLLED = booleanPreferencesKey("voice_is_enrolled")
        val KEY_SPEAKER_NAME = stringPreferencesKey("voice_speaker_name")
        val KEY_AVG_PITCH = floatPreferencesKey("voice_avg_pitch")
        val KEY_AVG_ENERGY = floatPreferencesKey("voice_avg_energy")
        val KEY_SPECTRAL_CENTROID = floatPreferencesKey("voice_spectral_centroid")
        val KEY_SAMPLES_COUNT = floatPreferencesKey("voice_samples_count")
        val KEY_TIMESTAMP = longPreferencesKey("voice_timestamp")
        val KEY_CONFIDENCE_THRESHOLD = floatPreferencesKey("voice_confidence_threshold")
    }

    val voiceProfileFlow: Flow<VoiceProfileData> = context.voiceProfileStore.data.map { pref ->
        VoiceProfileData(
            isEnrolled = pref[KEY_ENROLLED] ?: false,
            speakerName = pref[KEY_SPEAKER_NAME] ?: "User",
            averagePitchHz = pref[KEY_AVG_PITCH] ?: 0f,
            averageEnergy = pref[KEY_AVG_ENERGY] ?: 0f,
            spectralCentroid = pref[KEY_SPECTRAL_CENTROID] ?: 0f,
            totalSamplesCaptured = (pref[KEY_SAMPLES_COUNT] ?: 0f).toInt(),
            enrollmentDateTimestamp = pref[KEY_TIMESTAMP] ?: 0L,
            confidenceThreshold = pref[KEY_CONFIDENCE_THRESHOLD] ?: 0.65f
        )
    }

    /**
     * Compute acoustic features from raw 16-bit PCM audio buffer using Autocorrelation for accurate Pitch (F0)
     */
    fun extractAcousticFeatures(pcmBuffer: ShortArray, sampleRate: Int = 16000): Triple<Float, Float, Float> {
        if (pcmBuffer.isEmpty()) return Triple(0f, 0f, 0f)

        // 1. RMS Energy
        var sumSquares = 0.0
        for (sample in pcmBuffer) {
            sumSquares += sample.toDouble() * sample.toDouble()
        }
        val rms = sqrt(sumSquares / pcmBuffer.size).toFloat()

        // 2. Autocorrelation for Pitch (F0) Estimation within human vocal range [80Hz - 400Hz]
        val minPeriod = sampleRate / 400 // 40 samples at 16kHz
        val maxPeriod = sampleRate / 80  // 200 samples at 16kHz
        val windowSize = (pcmBuffer.size / 2).coerceAtMost(512)

        var bestCorr = 0.0
        var bestPeriod = 0

        if (pcmBuffer.size >= maxPeriod + windowSize) {
            for (lag in minPeriod..maxPeriod) {
                var corr = 0.0
                for (i in 0 until windowSize) {
                    corr += pcmBuffer[i].toDouble() * pcmBuffer[i + lag].toDouble()
                }
                if (corr > bestCorr) {
                    bestCorr = corr
                    bestPeriod = lag
                }
            }
        }

        val estimatedPitchHz = if (bestPeriod > 0 && bestCorr > 0) {
            (sampleRate.toFloat() / bestPeriod).coerceIn(75f, 380f)
        } else {
            // Fallback to Zero Crossing Rate
            var zcr = 0
            for (i in 1 until pcmBuffer.size) {
                if ((pcmBuffer[i] >= 0 && pcmBuffer[i - 1] < 0) || (pcmBuffer[i] < 0 && pcmBuffer[i - 1] >= 0)) {
                    zcr++
                }
            }
            ((zcr.toFloat() * sampleRate) / (2f * pcmBuffer.size)).coerceIn(80f, 350f)
        }

        // 3. Spectral Centroid approximation via discrete magnitude weighting
        var weightedSum = 0.0
        var totalMag = 0.0
        val step = 4
        for (i in 0 until pcmBuffer.size step step) {
            val mag = abs(pcmBuffer[i].toDouble())
            val freqBin = (i.toDouble() * sampleRate) / pcmBuffer.size
            weightedSum += freqBin * mag
            totalMag += mag
        }
        val centroid = if (totalMag > 0) (weightedSum / totalMag).toFloat() else 1200f

        return Triple(estimatedPitchHz, rms, centroid.coerceIn(200f, 3800f))
    }

    suspend fun saveEnrolledVoiceProfile(
        speakerName: String,
        pitches: List<Float>,
        energies: List<Float>,
        centroids: List<Float>
    ) {
        val avgPitch = if (pitches.isNotEmpty()) pitches.average().toFloat() else 165f
        val avgEnergy = if (energies.isNotEmpty()) energies.average().toFloat() else 1500f
        val avgCentroid = if (centroids.isNotEmpty()) centroids.average().toFloat() else 1200f

        context.voiceProfileStore.edit { pref ->
            pref[KEY_ENROLLED] = true
            pref[KEY_SPEAKER_NAME] = speakerName
            pref[KEY_AVG_PITCH] = avgPitch
            pref[KEY_AVG_ENERGY] = avgEnergy
            pref[KEY_SPECTRAL_CENTROID] = avgCentroid
            pref[KEY_SAMPLES_COUNT] = pitches.size.toFloat()
            pref[KEY_TIMESTAMP] = System.currentTimeMillis()
            pref[KEY_CONFIDENCE_THRESHOLD] = 0.65f
        }
    }

    suspend fun resetVoiceProfile() {
        context.voiceProfileStore.edit { pref ->
            pref[KEY_ENROLLED] = false
            pref[KEY_AVG_PITCH] = 0f
            pref[KEY_AVG_ENERGY] = 0f
            pref[KEY_SPECTRAL_CENTROID] = 0f
            pref[KEY_SAMPLES_COUNT] = 0f
            pref[KEY_TIMESTAMP] = 0L
        }
    }

    fun verifySpeakerMatch(
        samplePitch: Float,
        sampleCentroid: Float,
        profile: VoiceProfileData
    ): Pair<Boolean, Float> {
        if (!profile.isEnrolled || profile.averagePitchHz <= 0f) {
            return Pair(true, 1.0f) // Not enrolled -> accept all
        }

        val pitchDiff = abs(samplePitch - profile.averagePitchHz) / profile.averagePitchHz
        val centroidDiff = abs(sampleCentroid - profile.spectralCentroid) / profile.spectralCentroid

        val pitchScore = (1f - pitchDiff.coerceIn(0f, 1f))
        val centroidScore = (1f - centroidDiff.coerceIn(0f, 1f))

        val matchConfidence = (pitchScore * 0.6f + centroidScore * 0.4f).coerceIn(0f, 1f)
        val isMatch = matchConfidence >= profile.confidenceThreshold

        return Pair(isMatch, matchConfidence)
    }
}
