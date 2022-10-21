package au.com.shiftyjelly.pocketcasts.taskerplugin.base

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import au.com.shiftyjelly.pocketcasts.taskerplugin.controlplayback.InputControlPlayback
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutput
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperNoOutput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput

abstract class ViewModelBase<TInput : Any, THelper : TaskerPluginConfigHelperNoOutput<TInput, out TaskerPluginRunnerActionNoOutput<TInput>>>(application: Application) : AndroidViewModel(application), TaskerPluginConfig<TInput> {
    override val context get() = getApplication<Application>()
    abstract val helperClass: Class<THelper>
    private val taskerHelper by lazy { helperClass.getConstructor(TaskerPluginConfig::class.java).newInstance(this) }
    protected var input: TInput? = null

    private var taskerInput
        get() = TaskerInput(input ?: taskerHelper.inputClass.newInstance())
        set(value) {
            input = value.regular
        }

    override val inputForTasker: TaskerInput<TInput>
        get() = taskerInput

    fun getDescription(command: InputControlPlayback.PlaybackCommand) = command.getDescription(context)
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

    val taskerVariables get() = taskerHelper.relevantVariables
}
