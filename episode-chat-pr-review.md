# Episode Chat Branch Review

## Review scope

Reviewed branch `obsantos/episodechat` against `origin/main`.

- 27 commits reviewed: `bc7dac869` → `8464d8bf2`
- 36 changed files in the committed branch
- Also noticed 2 **uncommitted local changes** in the working tree:
  - `modules/services/servers/src/main/java/au/com/shiftyjelly/pocketcasts/servers/podcast/PodcastCacheService.kt`
  - `modules/services/servers/src/main/java/au/com/shiftyjelly/pocketcasts/servers/podcast/PodcastCacheServiceManagerImpl.kt`

Overall: the feature is structured in the right direction — new chat module, Compose UI, Hilt ViewModels, StateFlow, Room schema/migration — but I would **not consider this PR ready yet**. There are several functional blockers.

---

## Blockers / major PR findings

### 1. Uncommitted hard-coded ngrok endpoint must not ship

Current working tree changes route chat to a developer tunnel:

```kotlin
return service.episodeChat(
    url = "https://c6d4-70-29-50-8.ngrok-free.app/mobile/episode/chat",
    request = request,
)
```

Files:

- `PodcastCacheServiceManagerImpl.kt:91-93`
- `PodcastCacheService.kt:127-134`

This is not in the committed branch diff, but it is in the current working tree I tested. It must be reverted or replaced with a real `BuildConfig`/environment-configured endpoint before committing.

---

### 2. Plus/paywall gating is currently commented out, so free users can open chat

In `EpisodeFragment.kt`, the `isPlusUser` logic and `ChatPaywallFragment` path are commented out:

- `EpisodeFragment.kt:615`
- `EpisodeFragment.kt:624-640`

That means:

- `isPlusUser` is collected but not actually used.
- `ChatPaywallFragment` exists but is dead code.
- Free users can open chat when the feature flag is enabled.

This is especially risky because the UI/paywall text clearly implies chat is a Plus feature.

---

### 3. `EPISODE_CHAT` cannot be remotely enabled in release

`Feature.EPISODE_CHAT` is:

```kotlin
defaultValue = isDebugOrPrototypeBuild,
hasFirebaseRemoteFlag = false,
hasDevToggle = true,
```

File:

- `Feature.kt:236-243`

In release builds, `isDebugOrPrototypeBuild` is false, and because `hasFirebaseRemoteFlag = false`, this feature cannot be remotely rolled out. If this is intended to ever be enabled in production, this should be `hasFirebaseRemoteFlag = true`.

---

### 4. Chat request history includes quote messages and duplicates retry messages

`ChatManager.sendMessage()` builds history from `allMessages`:

- `ChatManager.kt:53-56`

But `ChatMessage.Quote` maps to `apiRole = "assistant"` and `textOrNull()` returns quote text. That means quote cards are sent back to the model as assistant conversation messages. This can pollute the conversation.

Also, on retry, `allMessages` already contains the failed last user message, and `message` is sent separately again. So retry can send the current user prompt twice: once in `conversationHistory`, once as `message`.

I’d fix this before backend testing because it can cause confusing or duplicated AI responses.

---

### 5. Cancellation is caught as `ServerError`

In `ChatViewModel.performSend()`:

```kotlin
} catch (e: Exception) {
    _uiState.update { it.copy(error = ChatError.ServerError) }
}
```

File:

- `ChatViewModel.kt:253-256`

`CancellationException` is an `Exception`. So if `clearChat()` cancels an in-flight send, the cancelled coroutine can still set a server error afterward.

This should either rethrow `CancellationException` or catch it separately.

---

### 6. Quote playback restoration has edge cases

In `ChatViewModel.playQuote()`:

- `quotePlaybackJob?.cancel()` cancels an existing quote playback without restoring the original playback snapshot.
- Starting a second quote while the first is in flight can overwrite `pendingPlaybackSnapshot`, losing the user’s original playback position.
- `capturePlaybackSnapshot()` uses `blockingFirst()` synchronously from the UI event path.

Files:

- `ChatViewModel.kt:134-139`
- `ChatViewModel.kt:174-183`

This needs tightening before shipping playable quotes. Manual QA should cover: play quote, stop quote, switch quotes, close sheet mid-quote, original episode playing vs paused, and original episode different from chat episode.

---

### 7. Chat availability is tied only to `Transcript.Text`

The banner is added inside:

```kotlin
AnimatedNonNullVisibility(
    item = transcript as? Transcript.Text,
)
```

File:

- `EpisodeFragment.kt:578-645`

So chat appears only for text transcripts. But `ChatManager` has logic to use external transcript URLs for non-generated transcripts:

- `ChatManager.kt:59-70`
- `ChatManager.kt:101-105`

If chat is meant to work with external/web transcripts, this UI gate prevents that. If it is intentionally text-transcript-only, the server request path can probably be simplified.

---

### 8. Data-layer architecture is leaking into the feature module

`ChatManager` lives in `modules/features/chat`, but directly uses:

- `EpisodeChatDao`
- `TranscriptDao`
- `PodcastCacheServiceManager`
- Moshi persistence mapping

That is effectively repository/data-layer behavior inside a feature module. This repo generally keeps data access in `modules/services/repositories`, with features depending on repository APIs.

Dependency analysis also caught this indirectly: `chat` uses transitive `model` and `servers` APIs without declaring them directly.

I’d consider moving `ChatManager` or the persistence/network parts into `modules/services/repositories`.

---

## Tests / checks run

These passed:

```bash
./gradlew :modules:features:chat:testDebugUnitTest \
  :modules:features:podcasts:testDebugUnitTest \
  :modules:services:servers:testDebugUnitTest \
  :modules:services:model:testDebugUnitTest
```

Note: `:modules:features:chat:testDebugUnitTest` is currently `NO-SOURCE`, so the new chat logic has no unit tests.

Passed:

```bash
./gradlew :modules:features:chat:lintDebug
./gradlew :modules:features:podcasts:lintDebug
./gradlew :app:assembleDebug
```

Dependency analysis:

```bash
./gradlew :modules:features:chat:projectHealth
```

Passed but reported cleanup needed, including:

- remove unused:
  - `implementation(libs.coroutines.reactive)`
  - `implementation(libs.coroutines.rx2)`
- declare/move direct deps for model/server usage
- `api(libs.moshi)` recommended instead of `implementation`

```bash
./gradlew :modules:features:podcasts:projectHealth
```

Passed but reported:

- `api(projects.modules.features.chat)` should be `implementation(projects.modules.features.chat)`
- some pre-existing dependency advice in podcasts

These did not cleanly complete:

```bash
./gradlew spotlessCheck
```

Failed due widespread existing formatting violations across the repo, not specifically isolated to this branch.

```bash
./gradlew buildHealth
```

Timed out after 240s, so I ran targeted `projectHealth` instead.

---

## Recommended next steps before PR

1. Revert/remove the ngrok endpoint.
2. Restore Plus gating or remove the paywall path if chat is intentionally free.
3. Make `EPISODE_CHAT` remotely configurable if it needs production rollout.
4. Fix conversation history:
   - exclude quote messages, unless backend explicitly wants them
   - avoid duplicating the retry prompt
5. Fix cancellation handling in `ChatViewModel`.
6. Harden quote playback restoration.
7. Move data/persistence/network orchestration out of the feature module or declare dependencies explicitly.
8. Add unit tests for:
   - `parseTimestampMs`
   - `ChatMessage` entity mapping
   - `ChatManager.sendMessage()` request construction
   - retry behavior
   - cancellation behavior
9. Manual QA on device/emulator:
   - feature flag off/on
   - Plus user vs free user
   - generated transcript vs author transcript vs web transcript
   - offline send
   - server error retry
   - clear chat during in-flight request
   - persisted chat after closing/reopening
   - playable quote start/stop/switch/restore playback
