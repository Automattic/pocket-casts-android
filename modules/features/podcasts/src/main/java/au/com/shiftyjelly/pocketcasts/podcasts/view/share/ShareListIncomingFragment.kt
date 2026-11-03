package au.com.shiftyjelly.pocketcasts.podcasts.view.share

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.fragment.app.viewModels
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastFragment
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class ShareListIncomingFragment :
    BaseFragment(),
    ShareListIncomingListener {

    @Inject lateinit var podcastManager: PodcastManager

    @Inject lateinit var settings: Settings

    private val viewModel: ShareListIncomingViewModel by viewModels()

    companion object {
        const val EXTRA_URL = "EXTRA_URL"
        const val EXTRA_SOURCE = "EXTRA_SOURCE"

        fun newInstance(
            listPath: String,
            sourceView: SourceView = SourceView.UNKNOWN,
        ): ShareListIncomingFragment {
            return ShareListIncomingFragment().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_URL, listPath)
                    putString(EXTRA_SOURCE, sourceView.key)
                }
            }
        }
    }

    val source get() = SourceView.fromString(arguments?.getString(EXTRA_SOURCE))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.getString(EXTRA_URL)?.let { url ->
            viewModel.loadShareUrl(url)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = contentWithoutConsumedInsets {
        val share by viewModel.share.asFlow().collectAsStateWithLifecycle(viewModel.share.value)
        val subscribedUuids by viewModel.subscribedUuids.asFlow().collectAsStateWithLifecycle(viewModel.subscribedUuids.value)
        val bottomInsetPx by settings.bottomInset.collectAsStateWithLifecycle(0)
        val bottomInset = with(LocalDensity.current) { bottomInsetPx.toDp() }
        AppTheme(theme.activeTheme) {
            ShareListIncomingPage(
                share = share,
                subscribedUuids = subscribedUuids?.toHashSet().orEmpty(),
                bottomInset = PaddingValues(bottom = bottomInset),
                listener = this,
                toolbarModifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onLongPress = { theme.toggleDarkLightThemeActivity(requireActivity() as AppCompatActivity) })
                },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!viewModel.isFragmentChangingConfigurations) {
            viewModel.trackIncomingShareListShown(source)
        }
    }

    override fun onCloseClick() {
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    override fun onPodcastClick(podcast: Podcast) {
        val fragment = PodcastFragment.newInstance(podcast.uuid, sourceView = SourceView.SHARE_LIST)
        (activity as FragmentHostListener).addFragment(fragment)
    }

    override fun onSubscribeClick(podcast: Podcast) {
        viewModel.subscribeToPodcast(podcast.uuid)
        viewModel.trackPodcastSubscribed(podcast.uuid)
    }

    override fun onUnsubscribeClick(podcast: Podcast) {
        val uuid = podcast.uuid
        val dialog = OptionsDialog()
            .setTitle(getString(LR.string.podcasts_unsubscribe_are_you_sure))
            .addTextOption(
                titleId = LR.string.unsubscribe,
                click = {
                    viewModel.unsubscribeFromPodcast(uuid)
                    viewModel.trackPodcastUnsubscribed(uuid)
                },
            )
        activity?.supportFragmentManager?.let {
            dialog.show(it, "unsubscribe")
        }
    }

    override fun onSubscribeToAllClick(podcasts: List<Podcast>) {
        viewModel.trackIncomingShareListSubscribeAll(podcasts.size)
        for (podcastHeader in podcasts) {
            val uuid = podcastHeader.uuid
            viewModel.subscribeToPodcast(uuid)
            viewModel.trackPodcastSubscribed(uuid)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onFragmentPause(activity?.isChangingConfigurations)
    }
}
