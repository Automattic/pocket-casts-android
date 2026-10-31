package au.com.shiftyjelly.pocketcasts.profile.cloud

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.content.IntentCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingActivity
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.deeplink.CloudFilesDeepLink
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.playback.EpisodeFileMetadata
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import coil3.Image
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.target.Target
import coil3.toBitmap
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.UpgradeBannerDismissedEvent
import com.automattic.eventhorizon.UploadedFilesInvalidFileErrorEvent
import com.automattic.eventhorizon.UploadedFilesUploadFailedEvent
import com.automattic.eventhorizon.UserFileEditSaveEvent
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val ACTION_PICK_IMAGE = 1
private const val ACTION_PICK_FILE = 2
private const val EXTRA_EXISTING_EPISODE_UUID = "fileUUID"
private const val EXTRA_FILE_CHOOSER = "filechooser"
private const val STATE_LAUNCHED_FILE_CHOOSER = "LAUNCHED_FILE_CHOOSER"
private const val STATE_DATAURI = "DATAURI"
private const val STATE_NAME = "NAME"

@AndroidEntryPoint
class AddFileActivity :
    AppCompatActivity(),
    CoroutineScope,
    OnboardingLauncher {
    companion object {

        fun newFileChooser(context: Context): Intent {
            val intent = Intent(context, AddFileActivity::class.java)
            intent.putExtra(EXTRA_FILE_CHOOSER, true)
            return intent
        }

        fun newEditInstance(context: Context, fileUuid: String): Intent {
            val intent = Intent(context, AddFileActivity::class.java)
            intent.putExtra(EXTRA_EXISTING_EPISODE_UUID, fileUuid)
            return intent
        }

        fun darkThemeColors() = listOf(
            AddFileColour(1, ThemeColor.primaryText02Dark, false),
            AddFileColour(2, ThemeColor.filter01Dark, true),
            AddFileColour(3, ThemeColor.filter05Dark, true),
            AddFileColour(4, ThemeColor.filter04Dark, true),
            AddFileColour(5, ThemeColor.filter03Dark, true),
            AddFileColour(6, ThemeColor.filter02Dark, true),
            AddFileColour(7, ThemeColor.filter06Dark, true),
            AddFileColour(8, ThemeColor.filter07Dark, true),
        )

        private fun lightThemeColors() = listOf(

            AddFileColour(1, ThemeColor.primaryText02Light, false),
            AddFileColour(2, ThemeColor.filter01Light, true),
            AddFileColour(3, ThemeColor.filter05Light, true),
            AddFileColour(4, ThemeColor.filter04Light, true),
            AddFileColour(5, ThemeColor.filter03Light, true),
            AddFileColour(6, ThemeColor.filter02Light, true),
            AddFileColour(7, ThemeColor.filter06Light, true),
            AddFileColour(8, ThemeColor.filter07Light, true),
        )
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    @Inject lateinit var fileStorage: FileStorage

    @Inject lateinit var userEpisodeManager: UserEpisodeManager

    @Inject lateinit var theme: Theme

    @Inject lateinit var playbackManager: PlaybackManager

    @Inject lateinit var settings: Settings

    @Inject lateinit var eventHorizon: EventHorizon

    private val viewModel: AddFileViewModel by viewModels()

    val uuid = UUID.randomUUID().toString()
    var bitmap: Bitmap? by mutableStateOf(null)

    var player: ExoPlayer? = null
    var length: Long? = null
    var sizeInBytes: Long? = null

    var freeSubscription = true

    var tintColor: Int = 0
        set(value) {
            field = value
            fileTint = value

            if (value == 0) {
                showImageView()
            } else {
                clearImageView()
            }
        }

    private var launchedFileChooser = false

    private var toolbarTitle by mutableStateOf<Int?>(null)
    private var name by mutableStateOf("")
    private var fileName by mutableStateOf("")
    private var fileSize by mutableStateOf("")
    private var fileTint by mutableStateOf<Int?>(null)
    private var isArtworkVisible by mutableStateOf(true)
    private var artworkImage by mutableStateOf<Bitmap?>(null)
    private var imageButtonRes by mutableIntStateOf(LR.string.profile_files_add_custom_image)
    private var isLoading by mutableStateOf(false)
    private var isReadOnly by mutableStateOf(false)
    private var isUpgradeVisible by mutableStateOf(false)
    private var isImageClickable by mutableStateOf(false)
    private var selectedIndex by mutableIntStateOf(1)
    private var colours = emptyList<AddFileColour>()

    val existingUuid: String?
        get() = intent.getStringExtra(EXTRA_EXISTING_EPISODE_UUID)

    val isFileChooserMode: Boolean
        get() = intent.getBooleanExtra(EXTRA_FILE_CHOOSER, false)

    var dataUri: Uri? = null

    fun updateForm(readOnly: Boolean, loading: Boolean) {
        isLoading = loading
        isReadOnly = readOnly
        isUpgradeVisible = readOnly && !settings.getUpgradeClosedAddFile() && !loading
    }

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        theme.setupThemeForConfig(this, resources.configuration)
        enableEdgeToEdge(navigationBarStyle = theme.getNavigationBarStyle(this))
        theme.updateWindowStatusBarIcons(window = window)

        colours = if (Theme.isDark(this)) darkThemeColors() else lightThemeColors()
        setContent {
            AppTheme(theme.activeTheme) {
                AddFilePage(
                    state = AddFilePageState(
                        titleRes = toolbarTitle,
                        name = name,
                        fileName = fileName,
                        fileSize = fileSize,
                        fileTint = fileTint,
                        isArtworkVisible = isArtworkVisible,
                        artwork = artworkImage,
                        imageButtonRes = imageButtonRes,
                        isLoading = isLoading,
                        isReadOnly = isReadOnly,
                        isUpgradeVisible = isUpgradeVisible,
                        imageItem = bitmap,
                        colours = colours,
                        selectedIndex = selectedIndex,
                    ),
                    onNavigationClick = { onBackPressedDispatcher.onBackPressed() },
                    onSaveClick = {
                        eventHorizon.track(UserFileEditSaveEvent)
                        saveFile()
                    },
                    onNameChange = { name = it },
                    onImageClick = if (isImageClickable) ::onImageClick else null,
                    onImageButtonClick = ::onImageClick,
                    onColourClick = ::selectColour,
                    onLockedColourClick = ::openOnboardingFlow,
                    onUpgradeClick = ::openOnboardingFlow,
                    onUpgradeCloseClick = {
                        eventHorizon.track(
                            UpgradeBannerDismissedEvent(
                                source = SourceView.FILES.analyticsValue,
                            ),
                        )
                        settings.setUpgradeClosedAddFile(true)
                        isUpgradeVisible = false
                    },
                    toolbarModifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(onLongPress = { theme.toggleDarkLightThemeActivity(this@AddFileActivity) })
                    },
                )
            }
        }

        updateForm(readOnly = true, loading = true)
        viewModel.signInState.observe(this) { signInState ->
            freeSubscription = signInState.isNoAccountOrFree
            updateForm(freeSubscription, false)
            isImageClickable = true
        }

        val existingUuid = this.existingUuid
        launchedFileChooser = savedInstanceState?.getBoolean(STATE_LAUNCHED_FILE_CHOOSER, false) ?: false
        if (launchedFileChooser) {
            // do nothing as we are returning from the file chooser
        } else if (existingUuid == null) {
            setupToolbar(title = LR.string.profile_cloud_add_file)

            if (isFileChooserMode) {
                launchFileChooser()
                return
            }

            dataUri = when (intent?.action) {
                Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                Intent.ACTION_VIEW -> intent.data
                else -> null
            }
            setupForNewFile(dataUri)
        } else {
            setupToolbar(title = LR.string.profile_cloud_edit_file)

            launch {
                val userEpisode = getUserEpisode(existingUuid)
                if (userEpisode == null) {
                    finish()
                    return@launch
                }

                fileName = ""
                fileSize = Util.formattedBytes(bytes = userEpisode.sizeInBytes, context = this@AddFileActivity)

                name = userEpisode.title
                selectColour(max(userEpisode.tintColorIndex, 0))
                isArtworkVisible = userEpisode.tintColorIndex == 0

                val artworkUrl = userEpisode.artworkUrl
                if (artworkUrl != null) {
                    if (artworkUrl.startsWith("file") || artworkUrl.startsWith("/")) {
                        loadImageFromUri(Uri.parse(artworkUrl), true)
                    } else {
                        loadImageFromUri(Uri.parse(artworkUrl), false)
                    }
                }
            }
        }

        @Suppress("DEPRECATION")
        if (savedInstanceState != null) {
            dataUri = savedInstanceState.getParcelable(STATE_DATAURI)
            setupForNewFile(dataUri)
            savedInstanceState.getString(STATE_NAME)?.let { name = it }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_LAUNCHED_FILE_CHOOSER, launchedFileChooser)
        outState.putParcelable(STATE_DATAURI, dataUri)
        outState.putString(STATE_NAME, name)
    }

    private fun onImageClick() {
        if (!freeSubscription) {
            startImagePicker()
        } else {
            openOnboardingFlow()
        }
    }

    private fun selectColour(index: Int) {
        selectedIndex = index
        if (index <= colours.size) {
            tintColor = colours.getOrNull(index - 1)?.color ?: 0
        }
    }

    private fun openOnboardingFlow() {
        openOnboardingFlow(OnboardingFlow.Upsell(OnboardingUpgradeSource.FILES))
    }

    override fun launchIntent(onboardingFlow: OnboardingFlow): Intent {
        return OnboardingActivity.newInstance(this, onboardingFlow)
    }

    override fun openOnboardingFlow(onboardingFlow: OnboardingFlow) {
        // Just starting the activity without registering for a result because
        // we don't need the result since we don't want to break the user's flow
        // by sending them back to the Discover screen with an
        // OnboardingFinish.DoneGoToDiscover result.
        startActivity(launchIntent(onboardingFlow))
    }

    @UnstableApi
    @Suppress("NAME_SHADOWING")
    private fun setupForNewFile(fileUri: Uri?) {
        val fileUri = fileUri ?: return
        preparePlayer(fileUri)

        setupToolbar(title = LR.string.profile_cloud_add_file)

        val filename = getFilenameOfContent(fileUri)
        name = filename?.substringBeforeLast(".").orEmpty()
        fileName = filename.orEmpty()
        val bytes = getFilesizeOfContent(fileUri)
        fileSize = Util.formattedBytes(bytes, context = this)
        sizeInBytes = bytes

        selectColour(1)
    }

    private fun setupToolbar(@StringRes title: Int) {
        toolbarTitle = title
    }

    private fun showImageView() {
        val bitmap = bitmap
        if (bitmap != null) {
            isArtworkVisible = true
            artworkImage = bitmap
            imageButtonRes = LR.string.profile_files_remove_image
        } else {
            clearImageView()
        }
    }

    private fun clearImageView() {
        isArtworkVisible = false
        fileTint = tintColor
        imageButtonRes = LR.string.profile_files_add_custom_image
    }

    @UnstableApi
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val uri = data?.data
        if (requestCode == ACTION_PICK_IMAGE && resultCode == Activity.RESULT_OK && uri != null) {
            tintColor = 0
            selectColour(0)
            loadImageFromUri(uri)
        } else if (requestCode == ACTION_PICK_FILE) {
            if (resultCode == Activity.RESULT_OK && uri != null) {
                Timber.d("$uri")
                dataUri = uri
                setupForNewFile(uri)
            } else {
                finish()
            }
        }
    }

    private fun loadImageFromUri(uri: Uri, isFile: Boolean = false) {
        val target = object : Target {
            override fun onError(error: Image?) {
                clearImageView()
            }

            override fun onSuccess(result: Image) {
                imageButtonRes = LR.string.profile_files_remove_image
                val loaded = result.toBitmap()
                bitmap = loaded
                artworkImage = loaded
                if (tintColor == 0) {
                    isArtworkVisible = true
                }
            }
        }

        artworkImage = null

        if (isFile) {
            val path = uri.path
            if (path != null) {
                val file = File(path)
                val request = ImageRequest.Builder(this)
                    .data(file)
                    .target(target)
                    .build()
                this.imageLoader.enqueue(request)
            }
        } else {
            val request = ImageRequest.Builder(this)
                .data(uri)
                .target(target)
                .build()
            this.imageLoader.enqueue(request)
        }
    }

    override fun onPause() {
        super.onPause()
        UiUtil.hideKeyboard(window.decorView)
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
    }

    private fun saveFile() {
        if (name.isEmpty()) {
            return
        }

        if (existingUuid != null) {
            updateFile()
            return
        }

        val uri = dataUri ?: return
        val intentType = intent.type ?: getMimetypeOfContent(uri) ?: uriToFileType(fileName)
        if (!(intentType.startsWith("audio/") || intentType.startsWith("video/"))) {
            return
        }

        if (isUriInvalid(uri)) {
            eventHorizon.track(
                UploadedFilesInvalidFileErrorEvent(
                    uri = uri.toString(),
                ),
            )

            Timber.e("Could not upload invalid file")

            val message = getString(LR.string.profile_cloud_add_invalid_file)
            handleErrorWhenLoadingFile(errorMessage = message)
        } else {
            launch(Dispatchers.IO) {
                try {
                    val userEpisode = UserEpisode(uuid = uuid, publishedDate = Date(), fileType = intent.type)

                    val outFile = fileStorage.getOrCreatePodcastEpisodeFile(userEpisode) ?: throw Exception("File path empty")

                    contentResolver.openInputStream(uri).use { inputStream ->
                        saveInputStreamToFile(outFile, inputStream)
                    }
                    val imageFile = saveBitmapToFile()

                    userEpisode.downloadedFilePath = outFile.absolutePath
                    userEpisode.downloadStatus = EpisodeDownloadStatus.Downloaded
                    userEpisode.title = name
                    userEpisode.durationMs = length?.toInt() ?: 0
                    userEpisode.fileType = intentType
                    userEpisode.sizeInBytes = sizeInBytes ?: 0

                    if (imageFile != null && isCustomImageSelected()) {
                        userEpisode.artworkUrl = imageFile.absolutePath
                        userEpisode.hasCustomImage = true
                    } else {
                        userEpisode.tintColorIndex = selectedIndex
                        userEpisode.hasCustomImage = false
                    }

                    userEpisodeManager.add(userEpisode, playbackManager)
                    userEpisodeManager.autoUploadToCloudIfReq(userEpisode)

                    launch(Dispatchers.Main) {
                        if (isFileChooserMode) {
                            finish()
                        } else {
                            startActivity(CloudFilesDeepLink.toIntent(this@AddFileActivity))
                            finish()
                        }
                    }
                } catch (e: Exception) {
                    eventHorizon.track(
                        UploadedFilesUploadFailedEvent(
                            uri = uri.toString(),
                        ),
                    )

                    Timber.e(e, "Could not load file")

                    launch(Dispatchers.Main) {
                        handleErrorWhenLoadingFile(errorMessage = getString(LR.string.profile_cloud_add_file_error, e.message))
                    }
                }
            }
        }
    }

    private fun handleErrorWhenLoadingFile(errorMessage: String) {
        AlertDialog.Builder(this)
            .setTitle(LR.string.error)
            .setMessage(errorMessage)
            .setPositiveButton(LR.string.ok, null)
            .show()
    }

    private fun isUriInvalid(uri: Uri?): Boolean = uri == null || contentResolver.getType(uri) == null

    private fun uriToFileType(filename: String): String {
        if (!filename.contains(".")) return "audio/mp3"
        val fileExtension = filename.split(".").lastOrNull() ?: return "audio/mp3"
        val videoExtentions = listOf("mp4", "m4v", "mov")
        return if (videoExtentions.contains(fileExtension)) {
            "video/$fileExtension"
        } else {
            "audio/$fileExtension"
        }
    }

    private fun isCustomImageSelected(): Boolean {
        return tintColor == 0
    }

    private fun updateFile() {
        launch {
            try {
                val existingUuid = existingUuid ?: return@launch
                val userEpisode = getUserEpisode(existingUuid) ?: return@launch
                if (bitmap != null && isCustomImageSelected()) {
                    userEpisode.hasCustomImage = true
                    val imageFile = saveBitmapToFile()
                    if (imageFile != null) {
                        userEpisode.artworkUrl = imageFile.absolutePath
                        if (userEpisode.serverStatus == UserEpisodeServerStatus.UPLOADED) {
                            viewModel.updateImageOnServer(userEpisode, imageFile)
                        }
                    }
                } else {
                    userEpisode.hasCustomImage = false
                    if (userEpisode.artworkUrl?.isNotEmpty() == true) {
                        viewModel.deleteImageFromServer(userEpisode)
                    }
                    userEpisode.artworkUrl = null
                }

                userEpisode.title = name
                userEpisode.tintColorIndex = selectedIndex
                if (userEpisode.serverStatus == UserEpisodeServerStatus.UPLOADED) {
                    viewModel.updateFileMetadataOnServer(userEpisode)
                }
                userEpisodeManager.update(userEpisode)

                finish()
            } catch (e: Exception) {
                AlertDialog.Builder(this@AddFileActivity)
                    .setTitle(LR.string.error)
                    .setMessage(getString(LR.string.profile_cloud_update_file_error) + "\n\n" + e.message)
                    .setPositiveButton(LR.string.ok, null)
                    .show()
                return@launch
            }
        }
    }

    private suspend fun getUserEpisode(existingUuid: String) = withContext(Dispatchers.Default) { userEpisodeManager.findEpisodeByUuid(existingUuid) }

    private fun saveBitmapToFile(): File? {
        val bitmap = this.bitmap ?: return null
        try {
            val outImageFile = fileStorage.getOrCreateCloudFileImage(uuid)
            FileOutputStream(outImageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 80, outputStream)
                return outImageFile
            }
        } catch (t: Throwable) {
            Timber.e(t, "Could not save bitmap to file")
        }
        return null
    }

    private fun saveInputStreamToFile(outFile: File, inputStream: InputStream?) {
        FileOutputStream(outFile).use { fileOutputStream ->
            inputStream?.use {
                val buffer = ByteArray(1024)
                var length = it.read(buffer)

                // Transferring data
                while (length != -1) {
                    fileOutputStream.write(buffer, 0, length)
                    length = it.read(buffer)
                }
            }
        }
    }

    private fun getFilenameOfContent(uri: Uri?): String? {
        try {
            val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
            val uriFinal = uri ?: return null
            contentResolver.query(uriFinal, projection, null, null, null).use { metaCursor ->
                if (metaCursor == null) {
                    return null
                }
                if (metaCursor.moveToFirst()) {
                    return metaCursor.getString(0)
                }
            }
        } catch (t: Throwable) {
            Timber.e(t, "Failed to get file name.")
        }
        return null
    }

    private fun getMimetypeOfContent(uri: Uri?): String? {
        try {
            val uriFinal = uri ?: return null
            val projection = arrayOf(MediaStore.MediaColumns.MIME_TYPE)
            contentResolver.query(uriFinal, projection, null, null, null).use { metaCursor ->
                if (metaCursor == null) {
                    return null
                }
                if (metaCursor.moveToFirst()) {
                    return metaCursor.getString(0)
                }
            }
        } catch (t: Throwable) {
            Timber.e(t, "Failed to get file mime type.")
        }
        return null
    }

    private fun getFilesizeOfContent(uri: Uri?): Long? {
        try {
            val uriFinal = uri ?: return null
            contentResolver.query(uriFinal, null, null, null, null).use { cursor ->
                if (cursor == null) {
                    return null
                }
                cursor.moveToFirst()
                val columnIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (columnIndex < 0) {
                    throw Exception("Column not found")
                }
                return cursor.getLong(columnIndex)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get file size.")
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun startImagePicker() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), ACTION_PICK_IMAGE)
    }

    @UnstableApi
    private fun preparePlayer(uri: Uri) {
        val loadControl = DefaultLoadControl.Builder().setBufferDurationsMs(0, 0, 0, 0).build()
        val player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setReleaseTimeoutMs(settings.getPlayerReleaseTimeOutMs())
            .build()
        player.addListener(object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                val episodeMetadata = EpisodeFileMetadata()
                episodeMetadata.read(tracks, useEpisodeArtwork = true, this@AddFileActivity)
                episodeMetadata.embeddedArtworkPath?.let {
                    val artworkUri = Uri.parse(it)
                    loadImageFromUri(artworkUri, isFile = true)
                    tintColor = 0
                    selectColour(0)
                }
                episodeMetadata.embeddedTitle?.let {
                    name = it
                }
                episodeMetadata.embeddedLength?.let {
                    length = it
                }
            }

            override fun onPlaybackStateChanged(@Player.State playbackState: Int) {
                if (playbackState == ExoPlayer.STATE_READY) {
                    length = player.duration
                }
            }
        })

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Pocket Casts")
            .setAllowCrossProtocolRedirects(true)
        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)
        val extractorsFactory = DefaultExtractorsFactory().setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING)
        val source = ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory).createMediaSource(MediaItem.fromUri(uri))
        player.setMediaSource(source)
        player.prepare()

        this.player = player
    }

    @Suppress("DEPRECATION")
    private fun launchFileChooser() {
        try {
            this.launchedFileChooser = true
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "audio/*,video/*"
            val mimeTypes = arrayOf("audio/*", "video/*")
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            startActivityForResult(intent, ACTION_PICK_FILE)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Please install a file explorer app", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
