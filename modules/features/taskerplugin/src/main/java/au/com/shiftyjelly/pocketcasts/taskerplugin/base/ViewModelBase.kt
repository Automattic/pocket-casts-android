package au.com.shiftyjelly.pocketcasts.taskerplugin.base

import android.app.Application
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

abstract class ViewModelBase<TInput : Any, TOutput : Any, THelper : TaskerPluginConfigHelper<TInput, TOutput, out TaskerPluginRunnerAction<TInput, TOutput>>>(application: Application) : AndroidViewModel(application), TaskerPluginConfig<TInput> {
    override val context get() = getApplication<Application>()
    abstract fun getNewHelper(pluginConfig: TaskerPluginConfig<TInput>): THelper
    private val taskerHelper by lazy { getNewHelper(this) }
    protected var input: TInput? = null

    /**
     * A field that only appears if [askFor] returns true
     * @param valueGetter how to get the value of this input field from the Tasker input
     * @param valueSetter how to set a newly assigned value of this field to the Tasker input
     */
    abstract inner class InputFieldBase<T : Any> constructor(@StringRes val labelResId: Int, @DrawableRes val iconResId: Int, val valueGetter: TInput.() -> String?, val valueSetter: TInput.(String?) -> Unit) {
        protected abstract val askFor: Boolean
        val shouldAskForState by lazy { MutableStateFlow(askFor) }
        fun updateAskForState() {
            shouldAskForState.tryEmit(askFor)
        }

        val valueState by lazy {
            MutableStateFlow(input?.let(valueGetter))
        }
        var value: String? = null
            set(value) {
                input?.let { valueSetter(it, value) }
                valueState.tryEmit(value)
            }

        open fun getPossibleValues(): Flow<List<T>>? = null

        @Suppress("UNCHECKED_CAST")
        fun getValueDescription(possibleValue: Any?): String = tryOrNull { getValueDescriptionSpecific(possibleValue as? T?) } ?: ""
        protected open fun getValueDescriptionSpecific(possibleValue: T?) = possibleValue?.toString()
    }

    protected inner class InputFieldString constructor(@StringRes labelResId: Int, @DrawableRes iconResId: Int, valueGetter: TInput.() -> String?, valueSetter: TInput.(String?) -> Unit) : InputFieldBase<String>(labelResId, iconResId, valueGetter, valueSetter) {
        override val askFor get() = true
    }

    protected inner class InputFieldBoolean constructor(@StringRes labelResId: Int, @DrawableRes iconResId: Int, valueGetter: TInput.() -> String?, valueSetter: TInput.(String?) -> Unit) : InputFieldBase<Boolean>(labelResId, iconResId, valueGetter, valueSetter) {
        override val askFor get() = true
        override fun getPossibleValues() = MutableStateFlow(listOf(true, false))
    }

    @JvmName("InputFieldEnumResId")
    protected inline fun <reified T : Enum<T>> InputFieldEnum(@StringRes labelResId: Int, @DrawableRes iconResId: Int, noinline valueGetter: TInput.() -> String?, noinline valueSetter: TInput.(String?) -> Unit, noinline valueDescriptionResIdGetter: ((T?) -> Int?)? = null): InputFieldBase<T> =
        InputFieldEnumStringDescription(labelResId, iconResId, valueGetter, valueSetter) { item ->
            if (valueDescriptionResIdGetter == null) return@InputFieldEnumStringDescription null
            val resId = valueDescriptionResIdGetter(item) ?: return@InputFieldEnumStringDescription null
            context.getString(resId)
        }

    @JvmName("InputFieldEnumString")
    protected inline fun <reified T : Enum<T>> InputFieldEnumStringDescription(@StringRes labelResId: Int, @DrawableRes iconResId: Int, noinline valueGetter: TInput.() -> String?, noinline valueSetter: TInput.(String?) -> Unit, noinline valueDescriptionGetter: ((T?) -> String?)? = null) = object : InputFieldBase<T>(labelResId, iconResId, valueGetter, valueSetter) {
        override val askFor get() = true
        override fun getPossibleValues() = MutableStateFlow(T::class.java.enumConstants?.toList() ?: listOf())
        override fun getValueDescriptionSpecific(possibleValue: T?): String? {
            if (valueDescriptionGetter == null) return super.getValueDescriptionSpecific(possibleValue)

            return valueDescriptionGetter(possibleValue)
        }
    }

    open val inputFields: List<InputFieldBase<*>> get() = listOf()
    private var taskerInput
        get() = TaskerInput(input ?: taskerHelper.inputClass.newInstance())
        set(value) {
            input = value.regular
        }

    override val inputForTasker: TaskerInput<TInput>
        get() = taskerInput

    override fun assignFromInput(input: TaskerInput<TInput>) {
        taskerInput = input
    }

    fun onCreate(finishFunc: (() -> Unit), getIntentFunc: (() -> Intent?), setResultFunc: ((Int, Intent) -> Unit)) {
        this.finishFunc = finishFunc
        this.getIntentFunc = getIntentFunc
        this.setResultFunc = setResultFunc
        taskerHelper.onCreate()
    }

    private var finishFunc: (() -> Unit)? = null
    override fun finish() = finishFunc?.invoke() ?: Unit

    private var getIntentFunc: (() -> Intent?)? = null
    override fun getIntent() = getIntentFunc?.invoke()

    private var setResultFunc: ((Int, Intent) -> Unit)? = null
    override fun setResult(resultCode: Int, data: Intent) = setResultFunc?.invoke(resultCode, data) ?: Unit

    fun finishForTasker() = taskerHelper.finishForTasker()

    val taskerVariables by lazy { taskerHelper.relevantVariables.distinct().sortedBy { it } }
}
