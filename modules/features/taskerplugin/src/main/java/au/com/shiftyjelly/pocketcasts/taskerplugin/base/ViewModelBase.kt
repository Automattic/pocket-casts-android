package au.com.shiftyjelly.pocketcasts.taskerplugin.base

import android.app.Application
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutput
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperNoOutput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

abstract class ViewModelBase<TInput : Any, THelper : TaskerPluginConfigHelperNoOutput<TInput, out TaskerPluginRunnerActionNoOutput<TInput>>>(application: Application) : AndroidViewModel(application), TaskerPluginConfig<TInput> {
    override val context get() = getApplication<Application>()
    abstract val helperClass: Class<THelper>
    private val taskerHelper by lazy { helperClass.getConstructor(TaskerPluginConfig::class.java).newInstance(this) }
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
    abstract val inputFields: List<InputFieldBase<*>>

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
