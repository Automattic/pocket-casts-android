package au.com.shiftyjelly.pocketcasts.profile.cloud

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.IntentCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingActivity
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.profile.R
import au.com.shiftyjelly.pocketcasts.profile.databinding.ActivityAddFileBinding
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadHelper
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
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import coil.imageLoader
import coil.request.ImageRequest
import coil.target.Target
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.math.max
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val ACTION_PICK_IMAGE = 1
private const val ACTION_PICK_FILE = 2
private const val EXTRA_EXISTING_EPISODE_UUID = "fileUUID"
private const val EXTRA_FILE_CHOOSER = "filechooser"
private const val STATE_LAUNCHED_FILE_CHOOSER = "LAUNCHED_FILE_CHOOSER"

@AndroidEntryPoint
class AddFileActivity :
    AppCompatActivity(),
    CoroutineScope,
    OnboardingLauncher,
    Toolbar.OnMenuItemClickListener {
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
            AddFileColourAdapter.Item.Colour(1, ThemeColor.primaryText02Dark, false),
            AddFileColourAdapter.Item.Colour(2, ThemeColor.filter01Dark, true),
            AddFileColourAdapter.Item.Colour(3, ThemeColor.filter05Dark, true),
            AddFileColourAdapter.Item.Colour(4, ThemeColor.filter04Dark, true),
            AddFileColourAdapter.Item.Colour(5, ThemeColor.filter03Dark, true),
            AddFileColourAdapter.Item.Colour(6, ThemeColor.filter02Dark, true),
            AddFileColourAdapter.Item.Colour(7, ThemeColor.filter06Dark, true),
            AddFileColourAdapter.Item.Colour(8, ThemeColor.filter07Dark, true),
        )

        private fun lightThemeColors() = listOf(

            AddFileColourAdapter.Item.Colour(1, ThemeColor.primaryText02Light, false),
            AddFileColourAdapter.Item.Colour(2, ThemeColor.filter01Light, true),
            AddFileColourAdapter.Item.Colour(3, ThemeColor.filter05Light, true),
            AddFileColourAdapter.Item.Colour(4, ThemeColor.filter04Light, true),
            AddFileColourAdapter.Item.Colour(5, ThemeColor.filter03Light, true),
            AddFileColourAdapter.Item.Colour(6, ThemeColor.filter02Light, true),
            AddFileColourAdapter.Item.Colour(7, ThemeColor.filter06Light, true),
            AddFileColourAdapter.Item.Colour(8, ThemeColor.filter07Light, true),
        )
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    @Inject lateinit var fileStorage: FileStorage
    @Inject lateinit var userEpisodeManager: UserEpisodeManager
    @Inject lateinit var theme: Theme
    @Inject lateinit var playbackManager: PlaybackManager
    @Inject lateinit var settings: Settings

    private val viewModel: AddFileViewModel by viewModels()

    val uuid = UUID.randomUUID().toString()
    var bitmap: Bitmap? = null
        set(value) {
            field = value
            updateColorItems()
        }

    var player: ExoPlayer? = null
    var length: Long? = null
    var sizeInBytes: Long? = null

    var freeSubscription = true

    var tintColor: Int = 0
        set(value) {
            field = value
            binding.imgFile.imageTintList = ColorStateList.valueOf(value)
            binding.mainConstraintView.requestFocus() // Without this the name field will grab focus when changing colour

            if (value == 0) {
                showImageView()
            } else {
                clearImageView()
            }
        }

    private var launchedFileChooser = false

    private lateinit var colorAdapter: AddFileColourAdapter
    private lateinit var binding: ActivityAddFileBinding

    val existingUuid: String?
        get() = intent.getStringExtra(EXTRA_EXISTING_EPISODE_UUID)

    val isFileChooserMode: Boolean
        get() = intent.getBooleanExtra(EXTRA_FILE_CHOOSER, false)

    var dataUri: Uri? = null

    fun updateForm(readOnly: Boolean, loading: Boolean) {
        binding.progress.isVisible = loading
        binding.imgLock.isVisible = readOnly
        colorAdapter.locked = readOnly
        binding.upgradeLayout.root.isVisible = readOnly && !settings.getUpgradeClosedAddFile() && !loading
    }

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        theme.setupThemeForConfig(this, resources.configuration)

        binding = ActivityAddFileBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        colorAdapter = AddFileColourAdapter(
            onSelectedChange = {
                tintColor = if (it is AddFileColourAdapter.Item.Colour) {
                    it.color
                } else {
                    0
                }
            },
            onLockedItemTapped = ::openOnboardingFlow
        )

        updateForm(readOnly = true, loading = true)
        viewModel.signInState.observe(this) { signInState ->
            freeSubscription = (signInState is SignInState.SignedOut) ||
                (signInState is SignInState.SignedIn && signInState.subscriptionStatus is SubscriptionStatus.Free)
            updateForm(freeSubscription, false)

            if (!freeSubscription) {
                binding.imgFile.setOnClickListener {
                    startImagePicker()
                }
            } else {
                binding.imgFile.setOnClickListener {
                    openOnboardingFlow()
                }
            }
        }

        val upgradeLayout = binding.upgradeLayout.root
        upgradeLayout.findViewById<View>(R.id.btnClose).setOnClickListener {
            settings.setUpgradeClosedAddFile(true)
            upgradeLayout.isVisible = false
        }
        upgradeLayout.setOnClickListener {
            openOnboardingFlow()
        }

        val colorLayoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        binding.recyclerColor.layoutManager = colorLayoutManager
        binding.recyclerColor.adapter = colorAdapter

        updateColorItems()

        binding.btnImage.setOnClickListener {
            if (!freeSubscription) {
                startImagePicker()
            } else {
                openOnboardingFlow()
            }
        }

        val existingUuid = this.existingUuid
        launchedFileChooser = savedInstanceState?.getBoolean(STATE_LAUNCHED_FILE_CHOOSER, false) ?: false
        if (launchedFileChooser) {
            // do nothing as we are returning from the file chooser
        } else if (existingUuid == null) {
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
            launch {
                val userEpisode = getUserEpisode(existingUuid)
                if (userEpisode == null) {
                    finish()
                    return@launch
                }

                setupToolbar(title = LR.string.profile_cloud_edit_file)

                binding.lblFilename.text = ""
                binding.lblFilesize.text = Util.formattedBytes(bytes = userEpisode.sizeInBytes, context = binding.lblFilesize.context)

                binding.txtFilename.setText(userEpisode.title)
                colorAdapter.selectedIndex = max(userEpisode.tintColorIndex, 0)
                binding.imgFileArtwork.isVisible = userEpisode.tintColorIndex == 0

                val artworkUrl = userEpisode.artworkUrl
                if (artworkUrl != null) {
                    if (artworkUrl.startsWith("file") || artworkUrl.startsWith("/")) {
                        loadImageFromUri(Uri.parse(artworkUrl), true)
                    } else {
                        loadImageFromUri(Uri.parse(artworkUrl), false)
                    }
                }

                updateColorItems()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_LAUNCHED_FILE_CHOOSER, launchedFileChooser)
    }

    private fun openOnboardingFlow() {
        openOnboardingFlow(OnboardingFlow.PlusUpsell(OnboardingUpgradeSource.FILES))
    }

    override fun openOnboardingFlow(onboardingFlow: OnboardingFlow) {
        // Just starting the activity without registering for a result because
        // we don't need the result since we don't want to break the user's flow
        // by sending them back to the Discover screen with an
        // OnboardingFinish.DoneGoToDiscover result.
        startActivity(OnboardingActivity.newInstance(this, onboardingFlow))
    }

    @UnstableApi
    @Suppress("NAME_SHADOWING")
    private fun setupForNewFile(fileUri: Uri?) {
        val fileUri = fileUri ?: return
        preparePlayer(fileUri)

        setupToolbar(title = LR.string.profile_cloud_add_file)

        val filename = getFilenameOfContent(fileUri)
        binding.txtFilename.setText(filename?.substringBeforeLast("."))
        binding.lblFilename.text = filename
        val bytes = getFilesizeOfContent(fileUri)
        binding.lblFilesize.text = Util.formattedBytes(bytes, context = binding.lblFilesize.context)
        sizeInBytes = bytes

        colorAdapter.selectedIndex = 1
    }

    private fun setupToolbar(@StringRes title: Int) {
        val toolbar = binding.toolbar
        toolbar.setup(
            title = getString(title),
            navigationIcon = NavigationIcon.BackArrow,
            activity = this,
            theme = theme,
            menu = R.menu.menu_addfile
        )
        toolbar.setOnMenuItemClickListener(this)
    }

    private fun updateColorItems() {
        val listItems = mutableListOf<AddFileColourAdapter.Item>()
        listItems.add(AddFileColourAdapter.Item.Image(bitmap))
        listItems.addAll(
            if (Theme.isDark(this)) {
                darkThemeColors()
            } else {
                lightThemeColors()
            }
        )
        colorAdapter.submitList(listItems)
    }

    private fun showImageView() {
        if (bitmap != null) {
            val imgFileArtwork = binding.imgFileArtwork
            imgFileArtwork.isVisible = true
            imgFileArtwork.imageTintList = null
            imgFileArtwork.setImageBitmap(bitmap)
            binding.btnImage.text = getString(LR.string.profile_files_remove_image)
        } else {
            clearImageView()
        }
    }

    private fun clearImageView() {
        binding.imgFileArtwork.isVisible = false
        binding.imgFile.setImageResource(IR.drawable.ic_uploadedfile)
        binding.imgFile.imageTintList = ColorStateList.valueOf(tintColor)
        binding.btnImage.text = getString(LR.string.profile_files_add_custom_image)
    }

    @UnstableApi
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val uri = data?.data
        if (requestCode == ACTION_PICK_IMAGE && resultCode == Activity.RESULT_OK && uri != null) {
            tintColor = 0
            colorAdapter.selectedIndex = 0
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
            override fun onError(error: Drawable?) {
                clearImageView()
            }

            override fun onSuccess(result: Drawable) {
                binding.btnImage.text = getString(LR.string.profile_files_remove_image)
                bitmap = result.toBitmap()
                binding.imgFileArtwork.setImageBitmap(bitmap)
                if (tintColor == 0) {
                    binding.imgFileArtwork.isVisible = true
                }
            }
        }

        binding.imgFileArtwork.setImageResource(IR.drawable.defaultartwork)

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
        UiUtil.hideKeyboard(binding.txtFilename)
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_save -> {
                saveFile()
                true
            }
            else -> false
        }
    }

    private fun saveFile() {
        if (binding.txtFilename.text.isNullOrEmpty()) {
            return
        }

        if (existingUuid != null) {
            updateFile()
            return
        }

        try {
            binding.layoutLoading.isVisible = true
            val uri = dataUri ?: return
            val intentType = intent.type ?: getMimetypeOfContent(uri) ?: uriToFileType(binding.lblFilename.text.toString())
            if (!(intentType.startsWith("audio/") || intentType.startsWith("video/"))) {
                return
            }
            launch(Dispatchers.IO) {
                val userEpisode = UserEpisode(uuid = uuid, publishedDate = Date(), fileType = intent.type)

                val savePath = DownloadHelper.pathForEpisode(userEpisode, fileStorage) ?: throw Exception("File path empty")
                val outFile = File(savePath)

                contentResolver.openInputStream(uri).use { inputStream ->
                    saveInputStreamToFile(outFile, inputStream)
                }
                val imageFile = saveBitmapToFile()

                userEpisode.downloadedFilePath = outFile.absolutePath
                userEpisode.episodeStatus = EpisodeStatusEnum.DOWNLOADED
                userEpisode.title = binding.txtFilename.text.toString()
                userEpisode.durationMs = length?.toInt() ?: 0
                userEpisode.fileType = intentType
                userEpisode.sizeInBytes = sizeInBytes ?: 0

                if (imageFile != null && isCustomImageSelected()) {
                    userEpisode.artworkUrl = imageFile.absolutePath
                    userEpisode.hasCustomImage = true
                } else {
                    userEpisode.tintColorIndex = colorAdapter.selectedIndex
                    userEpisode.hasCustomImage = false
                }

                userEpisodeManager.add(userEpisode, playbackManager)
                userEpisodeManager.autoUploadToCloudIfReq(userEpisode)

                launch(Dispatchers.Main) {
                    if (isFileChooserMode) {
                        finish()
                    } else {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Settings.INTENT_LINK_CLOUD_FILES))
                        startActivity(intent)
                        finish()
                    }
                }
            }
        } catch (e: Exception) {
            binding.layoutLoading.isVisible = false
            Timber.e(e, "Could not load file")
            AlertDialog.Builder(this)
                .setTitle(LR.string.error)
                .setMessage(getString(LR.string.profile_cloud_add_file_error, e.message))
                .setPositiveButton(LR.string.ok, null)
                .show()
        }
    }

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
                binding.layoutLoading.isVisible = true
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

                userEpisode.title = binding.txtFilename.text.toString()
                userEpisode.tintColorIndex = colorAdapter.selectedIndex
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
                binding.layoutLoading.isVisible = false
                return@launch
            }
        }
    }

    private suspend fun getUserEpisode(existingUuid: String) =
        withContext(Dispatchers.Default) { userEpisodeManager.findEpisodeByUuid(existingUuid) }

    private fun saveBitmapToFile(): File? {
        val bitmap = this.bitmap ?: return null
        try {
            val outImageFile = fileStorage.getCloudFileImage(uuid)
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
        val player = ExoPlayer.Builder(this).setLoadControl(loadControl).build()
        player.addListener(object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                val episodeMetadata = EpisodeFileMetadata()
                episodeMetadata.read(tracks, true, this@AddFileActivity)
                episodeMetadata.embeddedArtworkPath?.let {
                    val artworkUri = Uri.parse(it)
                    loadImageFromUri(artworkUri, isFile = true)
                    tintColor = 0
                    colorAdapter.selectedIndex = 0
                }
                episodeMetadata.embeddedTitle?.let {
                    binding.txtFilename.setText(it)
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
