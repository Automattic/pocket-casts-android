package au.com.shiftyjelly.pocketcasts.profile

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.button.MaterialButton
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class UserUpgradeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    val txtSubscription: TextView
    val lblFeature3: TextView
    val lblFindMore: TextView
    val btnUpgrade: MaterialButton

    init {
        LayoutInflater.from(context).inflate(R.layout.view_user_upgrade, this, true)
        txtSubscription = findViewById(R.id.txtSubscription)
        lblFeature3 = findViewById(R.id.lblFeature3)
        lblFindMore = findViewById(R.id.lblFindMore)
        btnUpgrade = findViewById(R.id.btnUpgrade)
    }

    fun setup(pricePerMonth: String?, storageLimit: Long) {
        txtSubscription.text = if (pricePerMonth == null) null else resources.getString(LR.string.plus_month_price, pricePerMonth)
        lblFeature3.text = resources.getString(LR.string.plus_cloud_storage_limit, storageLimit)
    }
}
