package au.com.shiftyjelly.pocketcasts.settings

import android.graphics.Shader
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPricingPhase
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.settings.databinding.FragmentPlusSettingsBinding
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.views.activity.WebViewActivity
import au.com.shiftyjelly.pocketcasts.views.component.GradientIcon
import au.com.shiftyjelly.pocketcasts.views.component.TileDrawable
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class PlusSettingsFragment : BaseFragment() {

    @Inject lateinit var subscriptionManager: SubscriptionManager
    @Inject lateinit var settings: Settings

    private var binding: FragmentPlusSettingsBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater.inflate(R.layout.fragment_plus_settings, container, false)

        val binding = FragmentPlusSettingsBinding.inflate(inflater, container, false)
        this.binding = binding

        val recyclerView = binding.recyclerView

        LiveDataReactiveStreams.fromPublisher(subscriptionManager.observeProductDetails()).observe(viewLifecycleOwner) { productDetailsState ->
            val subscriptions = when (productDetailsState) {
                is ProductDetailsState.Error -> null
                is ProductDetailsState.Loaded -> productDetailsState.productDetails.mapNotNull {
                    Subscription.fromProductDetails(
                        productDetails = it,
                        isFreeTrialEligible = subscriptionManager.isFreeTrialEligible()
                    )
                }
            }

            val headerText = PlusSection.TextBlock(LR.string.plus_description_title, LR.string.plus_description_body)
            val feature1 = PlusSection.Feature(R.drawable.ic_desktop_apps, LR.string.plus_desktop_apps, LR.string.plus_desktop_apps_body)
            val feature2 = PlusSection.Feature(R.drawable.ic_cloud_storage, LR.string.plus_cloud_storage, LR.string.plus_cloud_storage_body)
            val feature3 = PlusSection.Feature(R.drawable.ic_themes_icons, LR.string.plus_themes_icons, LR.string.plus_themes_icons_body)
            val feature4 = PlusSection.Feature(R.drawable.plus_folder, LR.string.plus_folder, LR.string.plus_folder_body)
            val upgrade = PlusSection.UpgradeButton(subscriptions)
            val link = PlusSection.LinkBlock(theme.verticalPlusLogoRes(), LR.string.plus_description_body, LR.string.plus_learn_more_about_plus, Settings.INFO_LEARN_MORE_URL)

            val sections = listOf(
                PlusSection.Header,
                headerText,
                upgrade,
                PlusSection.Divider,
                feature1,
                feature2,
                feature3,
                feature4,
                PlusSection.Divider,
                link,
                upgrade
            )

            val onAccountUpgradeClick: (() -> Unit) = {
                val flow = if (settings.isLoggedIn()) {
                    OnboardingFlow.PlusAccountUpgrade(OnboardingUpgradeSource.PLUS_DETAILS)
                } else {
                    OnboardingFlow.PlusAccountUpgradeNeedsLogin
                }
                OnboardingLauncher.openOnboardingFlow(activity, flow)
            }

            val adapter = PlusAdapter(onAccountUpgradeClick)
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(rootView.context, LinearLayoutManager.VERTICAL, false)

            adapter.submitList(sections)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.toolbar?.setup(title = getString(LR.string.pocket_casts_plus), navigationIcon = BackArrow, activity = activity, theme = theme)
    }
}

private sealed class PlusSection {
    data class UpgradeButton(val subscriptions: List<Subscription>?) : PlusSection()
    data class Feature(@DrawableRes val icon: Int, @StringRes val title: Int, @StringRes val body: Int) : PlusSection()
    object Header : PlusSection()
    data class TextBlock(@StringRes val title: Int, @StringRes val body: Int) : PlusSection()
    data class LinkBlock(@DrawableRes val icon: Int, @StringRes val body: Int, @StringRes val linkText: Int, val link: String) : PlusSection()
    object Divider : PlusSection()
}

private val SECTION_DIFF = object : DiffUtil.ItemCallback<PlusSection>() {
    override fun areItemsTheSame(oldItem: PlusSection, newItem: PlusSection): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: PlusSection, newItem: PlusSection): Boolean {
        return oldItem == newItem
    }
}

private class PlusAdapter(val onAccountUpgradeClick: () -> Unit) : ListAdapter<PlusSection, RecyclerView.ViewHolder>(SECTION_DIFF) {
    class FeatureViewHolder(val root: View) : RecyclerView.ViewHolder(root) {
        private val imgIcon = root.findViewById<GradientIcon>(R.id.imgIcon)
        private val lblTitle = root.findViewById<TextView>(R.id.lblTitle)
        private val lblBody = root.findViewById<TextView>(R.id.lblBody)

        fun bind(feature: PlusSection.Feature) {
            imgIcon.setup(AppCompatResources.getDrawable(root.context, feature.icon))
            lblTitle.setText(feature.title)
            lblBody.setText(feature.body)
        }
    }

    class UpgradeViewHolder(val root: View, val onAccountUpgradeClick: () -> Unit) : RecyclerView.ViewHolder(root) {
        private val lblSubtitle = root.findViewById<TextView>(R.id.lblSubtitle)
        private val btnUpgrade = root.findViewById<TextView>(R.id.btnUpgrade)

        fun bind(upgrade: PlusSection.UpgradeButton) {
            btnUpgrade.setOnClickListener {
                onAccountUpgradeClick()
            }

            upgrade.subscriptions?.let { subscriptions ->
                val trials = subscriptions.filterIsInstance<Subscription.WithTrial>()
                btnUpgrade.text = root.resources.getString(
                    if (trials.isEmpty()) {
                        LR.string.profile_upgrade_to_plus
                    } else {
                        LR.string.profile_start_free_trial
                    }
                )

                lblSubtitle.text = when (trials.size) {
                    0 -> {
                        val monthlySub = subscriptions
                            .find { it.recurringPricingPhase is SubscriptionPricingPhase.Months }
                        val yearlySub = subscriptions
                            .find { it.recurringPricingPhase is SubscriptionPricingPhase.Years }

                        if (monthlySub != null && yearlySub != null) {
                            root.resources.getString(
                                LR.string.plus_month_year_price,
                                monthlySub.recurringPricingPhase.formattedPrice,
                                yearlySub.recurringPricingPhase.formattedPrice
                            )
                        } else {
                            null
                        }
                    }
                    1 -> trials.first().numFreeThenPricePerPeriod(root.resources)
                    else ->
                        trials
                            .filter { it.recurringPricingPhase is SubscriptionPricingPhase.Months }
                            .ifEmpty { trials }
                            .first()
                            .numFreeThenPricePerPeriod(root.resources)
                }
            }
        }
    }

    class HeaderViewHolder(val root: View) : RecyclerView.ViewHolder(root) {
        private val tileOverlay = root.findViewById<View>(R.id.tileOverlay)

        fun bind() {
            val drawable = AppCompatResources.getDrawable(root.context, R.drawable.ic_plus_tile) ?: return
            val tile = TileDrawable(drawable, Shader.TileMode.REPEAT)
            tileOverlay.background = tile
        }
    }

    class TextBlockHolder(root: View) : RecyclerView.ViewHolder(root) {
        private val lblTitle = root.findViewById<TextView>(R.id.lblTitle)
        private val lblBody = root.findViewById<TextView>(R.id.lblBody)

        fun bind(textBlock: PlusSection.TextBlock) {
            lblTitle.setText(textBlock.title)
            lblBody.setText(textBlock.body)
        }
    }

    class LinkBlockHolder(root: View) : RecyclerView.ViewHolder(root) {
        private val imgLogo = root.findViewById<ImageView>(R.id.imgLogo)
        private val lblBody = root.findViewById<TextView>(R.id.lblBody)
        private val lblLink = root.findViewById<TextView>(R.id.lblLink)

        fun bind(linkBlock: PlusSection.LinkBlock) {
            lblBody.setText(linkBlock.body)
            lblLink.setText(linkBlock.linkText)
            lblLink.setOnClickListener {
                val intent = WebViewActivity.newInstance(lblLink.context, "Learn More", linkBlock.link)
                lblLink.context?.startActivity(intent)
            }
            imgLogo.setImageResource(linkBlock.icon)
        }
    }

    class DividerHolder(root: View) : RecyclerView.ViewHolder(root)

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is PlusSection.Feature -> R.layout.adapter_plus_feature_row
            is PlusSection.UpgradeButton -> R.layout.adapter_plus_upgrade_button
            is PlusSection.Header -> R.layout.adapter_plus_header
            is PlusSection.TextBlock -> R.layout.adapter_plus_text_block
            is PlusSection.LinkBlock -> R.layout.adapter_plus_link_block
            is PlusSection.Divider -> R.layout.adapter_plus_divider
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return when (viewType) {
            R.layout.adapter_plus_feature_row -> FeatureViewHolder(itemView)
            R.layout.adapter_plus_upgrade_button -> UpgradeViewHolder(itemView, onAccountUpgradeClick)
            R.layout.adapter_plus_header -> HeaderViewHolder(itemView)
            R.layout.adapter_plus_text_block -> TextBlockHolder(itemView)
            R.layout.adapter_plus_link_block -> LinkBlockHolder(itemView)
            R.layout.adapter_plus_divider -> DividerHolder(itemView)
            else -> throw IllegalStateException("Unknown view type in plus settings")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is FeatureViewHolder -> holder.bind(item as PlusSection.Feature)
            is UpgradeViewHolder -> holder.bind(item as PlusSection.UpgradeButton)
            is HeaderViewHolder -> holder.bind()
            is TextBlockHolder -> holder.bind(item as PlusSection.TextBlock)
            is LinkBlockHolder -> holder.bind(item as PlusSection.LinkBlock)
        }
    }
}
