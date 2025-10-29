# GitHub Copilot Instructions for Pocket Casts Android

## Technology Preferences

### Always Use for New Code
- **Jetpack Compose** for all new UI (never XML layouts)
- **Kotlin Coroutines and Flow** for async operations (never RxJava)
- **StateFlow** for ViewModel state management
- **Hilt** (@HiltViewModel, @Inject) for dependency injection
- **Kotlin** only (never Java)

### Architecture Pattern: MVVM
- ViewModels with `@HiltViewModel` annotation
- UI state as sealed interfaces/classes (Idle, Loading, Success, Error patterns)
- Expose state via `StateFlow`, accept actions via functions
- Use `MutableStateFlow` privately, expose as `StateFlow` publicly

## Code Style Rules

- **TODO only**: Use `TODO` for temporary notes, NEVER use `FIXME` (not allowed)
- **Nullable handling**: Prefer safe calls (`?.`) and Elvis operator (`?:`) over `!!`

## String Resources

- Add strings to `modules/services/localization` only

## Image Resources

- Add images to `modules/services/images` only

### Vector Graphics Preferred

- **Prefer vector graphics (.svg)** over rasterized formats when possible
  - Scalable to any screen density without quality loss
  - Smaller file size for simple graphics
  - Use for icons, logos, and simple illustrations

### WebP Format Required

- **New images**: Always use `.webp` format for better compression and smaller APK
- **PNG exceptions only for**:
  - App icons and launcher icons (when required by Android)
  - Images requiring transparency that are very small
  - Third-party assets that cannot be modified

## Testing

- Write unit tests for ViewModels and business logic
- Use `MainCoroutineRule` for coroutine testing
- Use **Turbine** library for Flow testing
- Mock with Mockito: `@Mock`, `whenever()`, `verify()`
