package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.settings.R
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.views.component.GradientIcon
import au.com.shiftyjelly.pocketcasts.views.helper.WhatsNewItem
import au.com.shiftyjelly.pocketcasts.views.helper.WhatsNewPage
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class WhatsNewAdapter(
    private var page: WhatsNewPage,
    private val clickListener: (WhatsNewItem.Link) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return when (page.items[position]) {
            is WhatsNewItem.Image -> R.layout.adapter_whatsnew_picture_item
            is WhatsNewItem.Title -> R.layout.adapter_whatsnew_title_item
            is WhatsNewItem.Body -> R.layout.adapter_whatsnew_body_item
            is WhatsNewItem.Bullet -> R.layout.adapter_whatsnew_bullet_item
            is WhatsNewItem.Link -> R.layout.adapter_whatsnew_link_item
        }
    }

    class ErrorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return when (viewType) {
            R.layout.adapter_whatsnew_picture_item -> WhatsNewImageViewHolder(view)
            R.layout.adapter_whatsnew_title_item -> WhatsNewTitleViewHolder(view)
            R.layout.adapter_whatsnew_body_item -> WhatsNewBodyViewHolder(view)
            R.layout.adapter_whatsnew_bullet_item -> WhatsNewBulletViewHolder(view)
            R.layout.adapter_whatsnew_link_item -> WhatsNewLinkViewHolder(view, clickListener)
            else -> ErrorViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val whatsNewItem = page.items[position]
        when (holder) {
            is WhatsNewImageViewHolder -> {
                holder.whatsNewItem = (whatsNewItem as? WhatsNewItem.Image)
            }
            is WhatsNewTitleViewHolder -> {
                holder.whatsNewItem = (whatsNewItem as? WhatsNewItem.Title)
            }
            is WhatsNewBodyViewHolder -> {
                holder.whatsNewItem = (whatsNewItem as? WhatsNewItem.Body)
            }
            is WhatsNewBulletViewHolder -> {
                holder.whatsNewItem = (whatsNewItem as? WhatsNewItem.Bullet)
            }
            is WhatsNewLinkViewHolder -> {
                holder.whatsNewItem = (whatsNewItem as? WhatsNewItem.Link)
            }
        }
    }

    override fun getItemCount() = page.items.count()
}

class WhatsNewImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val imgPicture: GradientIcon = itemView.findViewById(R.id.imgPicture)
    private val imgSecondary: ImageView = itemView.findViewById(R.id.imgSecondaryPicture)
    var whatsNewItem: WhatsNewItem.Image? = null
        set(value) {
            field = value

            val firstImage = drawableFromResource(field?.resourceName)
            val gradStart = imgPicture.context.getThemeColor(UR.attr.gradient_02_a)
            val gradEnd = imgPicture.context.getThemeColor(UR.attr.gradient_02_e)
            imgPicture.setup(firstImage, gradStart, gradEnd)

            val secondImage = drawableFromResource(field?.secondaryResourceName)
            imgSecondary.setImageDrawable(secondImage)
        }

    private fun drawableFromResource(@DrawableRes resourceId: Int?): Drawable? {
        resourceId ?: return null
        return AppCompatResources.getDrawable(imgPicture.context, resourceId)
    }
}

class WhatsNewTitleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
    var whatsNewItem: WhatsNewItem.Title? = null
        set(value) {
            field = value
            if (value != null) {
                txtTitle.setText(value.title)
            }
        }
}

class WhatsNewBodyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val txtBody: TextView = itemView.findViewById(R.id.txtBody)
    var whatsNewItem: WhatsNewItem.Body? = null
        set(value) {
            field = value
            if (value != null) {
                txtBody.setText(value.body)
            }
        }
}

class WhatsNewBulletViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val txtBody: TextView = itemView.findViewById(R.id.txtBody)
    var whatsNewItem: WhatsNewItem.Bullet? = null
        set(value) {
            field = value
            if (value != null) {
                txtBody.setText(value.body)
            }
        }
}

class WhatsNewLinkViewHolder(itemView: View, val clickListener: (WhatsNewItem.Link) -> Unit) : RecyclerView.ViewHolder(itemView) {
    private val txtLink: TextView = itemView.findViewById(R.id.txtLink)
    var whatsNewItem: WhatsNewItem.Link? = null
        set(value) {
            field = value
            if (value != null) {
                txtLink.setText(value.title)
                txtLink.setOnClickListener {
                    clickListener(value)
                }
            }
        }
}
