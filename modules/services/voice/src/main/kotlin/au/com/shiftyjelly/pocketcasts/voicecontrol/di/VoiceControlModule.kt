package au.com.shiftyjelly.pocketcasts.voicecontrol.di

import android.content.Context
import android.media.AudioManager
import android.os.PowerManager
import android.os.SystemClock
import android.telephony.TelephonyManager
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.CastManager
import au.com.shiftyjelly.pocketcasts.voicecontrol.feedback.AudioFeedbackRenderer
import au.com.shiftyjelly.pocketcasts.voicecontrol.feedback.EarconPlayer
import au.com.shiftyjelly.pocketcasts.voicecontrol.foreground.ForegroundStateMonitor
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.EnabledByUserCondition
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlGate
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRule
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions.AppInForegroundCondition
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions.BatteryOkCondition
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions.DeviceSupportedCondition
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions.GracePeriodActiveCondition
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions.ModelsReadyCondition
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions.NotCastingCondition
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions.NotOnCallCondition
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions.OtherAppPlayingCondition
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.signals.GracePeriodSignal
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.EmbeddingIntentMatcher
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.EntityExtractor
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.FunctionGemmaIntentRouter
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.FunctionGemmaMetrics
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.TimberFunctionGemmaMetrics
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.embedding.BpeTokenizer
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.embedding.EmbeddingEngine
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.embedding.JniEmbeddingEngine
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.embedding.TextTokenizer
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.entity.GrammarEntityExtractor
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.runtime.FunctionGemmaRuntimeFactory
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.runtime.LiteRtFunctionGemmaRuntimeFactory
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.runtime.MonotonicClock
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.ModelManager
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.VoiceRecognizer
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.AudioManagerVolumeSink
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.NoOpCloudRouteSink
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.PlaybackContextActiveCondition
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.PlaybackContextMonitor
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.PlaybackContextProvider
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.PlaybackManagerBookmarkSink
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.PlaybackManagerChapterSink
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.PlaybackManagerEffectsSink
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.PlaybackManagerPlaybackContextProvider
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.PlaybackManagerPlaybackSink
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.PlaybackQuerySink
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.SleepTimerSink
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.StatsQuerySink
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.UpNextQueueSink
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.VoiceBookmarkSink
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.VoiceChapterSink
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.VoiceCloudRouteSink
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.VoiceEffectsSink
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.VoicePlaybackQuerySink
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.VoicePlaybackSink
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.VoiceQueueSink
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.VoiceSleepSink
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.VoiceStatsQuerySink
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.VoiceVolumeSink
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.AndroidAudioRouteMonitor
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.AudioRouteMonitor
import au.com.shiftyjelly.pocketcasts.voicecontrol.tts.AndroidPlatformTtsEngine
import au.com.shiftyjelly.pocketcasts.voicecontrol.tts.TtsEngine
import au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword.OpenWakeWordDetector
import au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword.WakeWordDetector
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceControlModule {

    @Binds abstract fun bindVoiceRecognizer(impl: FunctionGemmaIntentRouter): VoiceRecognizer

    @Binds abstract fun bindFunctionGemmaRuntimeFactory(
        impl: LiteRtFunctionGemmaRuntimeFactory,
    ): FunctionGemmaRuntimeFactory

    @Binds abstract fun bindFunctionGemmaMetrics(
        impl: TimberFunctionGemmaMetrics,
    ): FunctionGemmaMetrics

    @Binds abstract fun bindVoicePlaybackSink(impl: PlaybackManagerPlaybackSink): VoicePlaybackSink

    @Binds abstract fun bindVoiceEffectsSink(impl: PlaybackManagerEffectsSink): VoiceEffectsSink

    @Binds abstract fun bindVoiceVolumeSink(impl: AudioManagerVolumeSink): VoiceVolumeSink

    @Binds abstract fun bindVoiceSleepSink(impl: SleepTimerSink): VoiceSleepSink

    @Binds abstract fun bindVoiceChapterSink(impl: PlaybackManagerChapterSink): VoiceChapterSink

    @Binds abstract fun bindVoiceBookmarkSink(impl: PlaybackManagerBookmarkSink): VoiceBookmarkSink

    @Binds abstract fun bindVoiceCloudRouteSink(impl: NoOpCloudRouteSink): VoiceCloudRouteSink

    @Binds abstract fun bindPlaybackContextProvider(impl: PlaybackManagerPlaybackContextProvider): PlaybackContextProvider

    @Binds abstract fun bindVoiceQueueSink(impl: UpNextQueueSink): VoiceQueueSink

    @Binds abstract fun bindVoicePlaybackQuerySink(impl: PlaybackQuerySink): VoicePlaybackQuerySink

    @Binds abstract fun bindVoiceStatsQuerySink(impl: StatsQuerySink): VoiceStatsQuerySink

    @Binds abstract fun bindAudioRouteMonitor(impl: AndroidAudioRouteMonitor): AudioRouteMonitor

    @Binds abstract fun bindTextTokenizer(impl: BpeTokenizer): TextTokenizer

    @Binds abstract fun bindEmbeddingEngine(impl: JniEmbeddingEngine): EmbeddingEngine

    @Binds abstract fun bindEntityExtractor(impl: GrammarEntityExtractor): EntityExtractor

    @Binds abstract fun bindWakeWordDetector(impl: OpenWakeWordDetector): WakeWordDetector

    @Binds abstract fun bindTtsEngine(impl: AndroidPlatformTtsEngine): TtsEngine

    companion object {
        @Provides
        @Singleton
        fun provideMonotonicClock(): MonotonicClock = MonotonicClock(SystemClock::elapsedRealtime)

        @Provides @Singleton
        @Named("embeddingModel")
        fun provideEmbeddingModelFile(manager: ModelManager): File = manager.embeddingModelFile

        @Provides @Singleton
        @Named("embeddingTokenizer")
        fun provideEmbeddingTokenizerFile(manager: ModelManager): File = manager.tokenizerModelFile

        @Provides @Singleton
        fun provideDeviceProbe(): au.com.shiftyjelly.pocketcasts.voicecontrol.asr.DeviceProbe = au.com.shiftyjelly.pocketcasts.voicecontrol.asr.DeviceProbe()

        @Provides @Singleton
        fun provideDeviceSupportedCondition(): DeviceSupportedCondition = DeviceSupportedCondition()

        @Provides @Singleton
        fun provideModelsReadyCondition(): ModelsReadyCondition = ModelsReadyCondition()

        @Provides @Singleton
        @Suppress("DEPRECATION") // TelephonyManager.callState deprecated in API 31; CallStateCallback requires CALL_STATE permission
        fun provideNotOnCallCondition(
            @ApplicationContext context: Context,
        ): NotOnCallCondition {
            val inCall = try {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                tm?.callState == TelephonyManager.CALL_STATE_OFFHOOK
            } catch (e: SecurityException) {
                false
            }
            return NotOnCallCondition(isInCall = inCall)
        }

        @Provides @Singleton
        fun provideNotCastingCondition(castManager: CastManager): NotCastingCondition = NotCastingCondition()

        @Provides @Singleton
        fun provideBatteryOkCondition(
            @ApplicationContext context: Context,
        ): BatteryOkCondition = BatteryOkCondition(
            isPowerSaveMode = (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)
                ?.isPowerSaveMode == true,
        )

        @Provides @Singleton
        fun provideAppInForegroundCondition(
            foregroundState: ForegroundStateMonitor,
            @ApplicationScope scope: CoroutineScope,
        ): AppInForegroundCondition = AppInForegroundCondition(foregroundState, scope)

        @Provides @Singleton
        fun provideGracePeriodActiveCondition(
            gracePeriodSignal: GracePeriodSignal,
            @ApplicationScope scope: CoroutineScope,
        ): GracePeriodActiveCondition = GracePeriodActiveCondition(gracePeriodSignal, scope)

        @Provides @Singleton
        fun provideOtherAppPlayingCondition(
            @ApplicationContext context: Context,
            playbackContextMonitor: PlaybackContextMonitor,
            @ApplicationScope scope: CoroutineScope,
        ): OtherAppPlayingCondition = OtherAppPlayingCondition(
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager,
            hostIsPlaying = playbackContextMonitor.isHostAudioActive,
            scope = scope,
        )

        @Provides
        @Singleton
        fun provideVoiceControlGate(
            playbackContextMonitor: PlaybackContextMonitor,
            settings: Settings,
            deviceSupported: DeviceSupportedCondition,
            modelsReady: ModelsReadyCondition,
            notOnCall: NotOnCallCondition,
            notCasting: NotCastingCondition,
            batteryOk: BatteryOkCondition,
            appInForeground: AppInForegroundCondition,
            otherAppPlaying: OtherAppPlayingCondition,
            gracePeriodActive: GracePeriodActiveCondition,
            @ApplicationScope scope: CoroutineScope,
        ): VoiceControlGate {
            val rules: List<VoiceControlRule> = listOf(
                // Setup group
                EnabledByUserCondition(settings, scope),
                deviceSupported,
                modelsReady,
                // Conflicts group — audio route does not gate voice control; MicExposure
                // only blocks capture for NoMic (spec: MicExposure does not select mode)
                notOnCall,
                notCasting,
                batteryOk,
                otherAppPlaying,
                // Context group
                appInForeground,
                PlaybackContextActiveCondition(playbackContextMonitor.context, scope),
                gracePeriodActive,
            )
            return VoiceControlGate(rules = rules, scope = scope)
        }

        @Provides
        @Singleton
        fun provideEarconPlayer(@ApplicationContext context: Context): EarconPlayer = EarconPlayer(context)

        @Provides
        @Singleton
        fun provideAudioFeedbackRenderer(
            earconPlayer: EarconPlayer,
            ttsEngine: TtsEngine,
        ): AudioFeedbackRenderer = AudioFeedbackRenderer(earconPlayer, ttsEngine)
    }
}
