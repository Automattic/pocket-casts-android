package au.com.shiftyjelly.pocketcasts.navigation

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import au.com.shiftyjelly.pocketcasts.navigation.FragmentTransactionCommand.AddAndShow
import au.com.shiftyjelly.pocketcasts.navigation.FragmentTransactionCommand.AddOnTop
import au.com.shiftyjelly.pocketcasts.navigation.FragmentTransactionCommand.Clear
import au.com.shiftyjelly.pocketcasts.navigation.FragmentTransactionCommand.RemoveAllAndAdd
import au.com.shiftyjelly.pocketcasts.navigation.FragmentTransactionCommand.RemoveAllAndShowExisting
import au.com.shiftyjelly.pocketcasts.navigation.FragmentTransactionCommand.RemoveUnknown
import au.com.shiftyjelly.pocketcasts.navigation.FragmentTransactionCommand.ShowAndRemove
import au.com.shiftyjelly.pocketcasts.navigation.FragmentTransactionCommand.ShowExisting
import java.util.UUID

internal data class CommandWithRunnable(
    val command: FragmentTransactionCommand,
    val runAfterCommit: () -> Unit
)

internal sealed class FragmentTransactionCommand {
    data class AddAndShow(val fragment: Fragment, val tag: TagStructure) : FragmentTransactionCommand()
    data class AddOnTop(val fragment: Fragment, val tag: TagStructure) : FragmentTransactionCommand()
    data class ShowExisting(val tag: TagStructure) : FragmentTransactionCommand()
    data class ShowAndRemove(val showTag: TagStructure, val removeTag: TagStructure) : FragmentTransactionCommand()
    data class Clear(val allCurrentTags: List<TagStructure>) : FragmentTransactionCommand()
    data class RemoveAllAndAdd(val remove: List<TagStructure>, val add: AddAndShow) : FragmentTransactionCommand()
    data class RemoveAllAndShowExisting(val remove: List<TagStructure>, val show: ShowExisting) : FragmentTransactionCommand()
    data class RemoveUnknown(val knownFragments: List<TagStructure>) : FragmentTransactionCommand()
}

internal class FragmentTransactionHandler(
    private val fm: FragmentManager,
    @IdRes private val container: Int,
    @IdRes private val modalContainer: Int
) {
    fun handle(commandWithRunnable: CommandWithRunnable) {
        val (command, runnable) = commandWithRunnable

        @Suppress("UNUSED_VARIABLE") // val exhaustWhen is just used to help us remember to add the when if a new command is added
        val exhaustWhen = when (command) {
            is AddAndShow -> addAndShowFragment(command.fragment, command.tag, runnable)
            is AddOnTop -> addOnTop(command.fragment, command.tag, runnable)
            is ShowExisting -> showFragment(command.tag, runnable)
            is ShowAndRemove -> showAndRemoveFragment(
                command.showTag,
                command.removeTag,
                runnable
            )
            is Clear -> clear(runnable)
            is RemoveAllAndAdd -> removeAllAndAdd(command.remove, command.add.fragment, command.add.tag, runnable)
            is RemoveAllAndShowExisting -> removeAllAndShow(command.remove, command.show.tag, runnable)
            is RemoveUnknown -> removeUnknown(command, runnable)
        }
    }

    private fun containerFor(tag: TagStructure): Int {
        return if (tag.isModal) modalContainer else container
    }

    private fun removeAllAndAdd(
        remove: List<TagStructure>,
        add: Fragment,
        addTag: TagStructure,
        runnable: () -> Unit
    ) {
        val transaction = fm.beginTransaction()
        for (removeTag in remove) {
            val removeFragment = fm.findFragmentByTag(removeTag.toString())
            if (removeFragment != null) {
                transaction.remove(removeFragment)
            }
        }
        transaction.add(containerFor(addTag), add, addTag.toString())
            .detachOtherFragments(add)
            .runOnCommit(runnable)
            .setReorderingAllowed(true)
            .commitNow()
    }

    private fun removeAllAndShow(
        remove: List<TagStructure>,
        show: TagStructure,
        runnable: () -> Unit
    ) {
        val transaction = fm.beginTransaction()
        for (removeTag in remove) {
            val removeFragment = fm.findFragmentByTag(removeTag.toString())
            if (removeFragment != null) {
                transaction.remove(removeFragment)
            }
        }
        val fragment = fm.findFragmentByTag(show.toString())!!
        transaction.showOrAttach(fragment)
            .detachOtherFragments(fragment)
            .runOnCommit(runnable)
            .setReorderingAllowed(true)
            .commitNow()
    }

    private fun showAndRemoveFragment(
        showTag: TagStructure,
        removeTag: TagStructure,
        runnable: () -> Unit
    ) {
        val showFragment = fm.findFragmentByTag(showTag.toString())!!
        val removeFragment = fm.findFragmentByTag(removeTag.toString())!!
        fm.beginTransaction()
            .remove(removeFragment)
            .detachOtherFragments(showFragment)
            .showOrAttach(showFragment)
            .runOnCommit(runnable)
            .setReorderingAllowed(true)
            .commitNow()
    }

    private fun showFragment(
        tag: TagStructure,
        runnable: () -> Unit
    ) {
        val fragment = fm.findFragmentByTag(tag.toString())!!
        fm.beginTransaction()
            .showOrAttach(fragment)
            .detachOtherFragments(fragment)
            .runOnCommit(runnable)
            .setReorderingAllowed(true)
            .commitNow()
    }

    private fun FragmentTransaction.showOrAttach(fragment: Fragment): FragmentTransaction {
        return if (TagStructure.fromTag(fragment.tag!!).isDetachable) {
            attach(fragment)
        } else {
            show(fragment)
        }
    }

    private fun addAndShowFragment(
        fragment: Fragment,
        tag: TagStructure,
        runnable: () -> Unit
    ) {
        fm.beginTransaction()
            .add(containerFor(tag), fragment, tag.toString())
            .detachOtherFragments(fragment)
            .runOnCommit(runnable)
            .setReorderingAllowed(true)
            .commitNow()
    }

    private fun addOnTop(
        fragment: Fragment,
        tag: TagStructure,
        runnable: () -> Unit
    ) {
        fm.beginTransaction()
            .add(containerFor(tag), fragment, tag.toString())
            .runOnCommit(runnable)
            .setReorderingAllowed(true)
            .commitNow()
    }

    private fun FragmentTransaction.detachOtherFragments(keep: Fragment): FragmentTransaction {
        return fm.fragments
            .filter { it != keep && TagStructure.fromTag(it.tag).isOurFragment }
            .fold(this) { transaction, fragment ->
                if (TagStructure.fromTag(fragment.tag!!).isDetachable) {
                    transaction.detach(fragment)
                } else {
                    transaction.hide(fragment)
                }
            }
    }

    private fun clear(runnable: () -> Unit) {
        fm.fragments
            .filter { TagStructure.fromTag(it.tag).isOurFragment }
            .fold(fm.beginTransaction()) { transaction, fragment ->
                transaction.remove(fragment)
            }
            .runOnCommit(runnable)
            .setReorderingAllowed(true)
            .commitNow()
    }

    private fun removeUnknown(
        command: FragmentTransactionCommand.RemoveUnknown,
        runnable: () -> Unit
    ) {
        val knownFragments = command.knownFragments

        val unknown = fm.fragments
            .filter {
                val tag = TagStructure.fromTag(it.tag)

                // it's our fragment but we don't know about it
                tag.isOurFragment && !knownFragments.contains(tag)
            }

        if (unknown.isNotEmpty()) {
            unknown
                .fold(fm.beginTransaction()) { transaction, fragment ->
                    transaction.remove(fragment)
                }
                .runOnCommit(runnable)
                .setReorderingAllowed(true)
                .commitNow()
        }
    }
}

/**
 * Info that gets serialized into the FragmentManager's fragment tag string
 */
@Suppress("DataClassPrivateConstructor")
internal data class TagStructure private constructor(
    val className: String?,
    val detachable: Boolean?,
    val uuid: String?,
    val modal: Boolean?
) {
    constructor(fragment: Fragment, detachable: Boolean, modal: Boolean) :
        this(fragment::class.java.name, detachable, UUID.randomUUID().toString(), modal)

    var isOurFragment = true
        private set
    var isDetachable = detachable == true
    var isModal = modal == true

    override fun toString(): String {
        return StringBuilder()
            .append(
                OURTAG, SEPARATOR, className, SEPARATOR, if (detachable == true) DETACHABLE else "", SEPARATOR, uuid, SEPARATOR, if (modal == true) MODAL else ""
            )
            .toString()
    }

    companion object {
        /**
         * Used to identify fragments we've added to the fragment manager
         */
        private const val OURTAG = "au.com.shiftyjelly.pocketcasts.navigator"
        private const val SEPARATOR = "|"
        private const val DETACHABLE = "DETACHABLE"
        private const val MODAL = "MODAL"

        fun fromTag(tag: String?): TagStructure {
            if (tag == null || !tag.startsWith(OURTAG)) {
                return TagStructure(null, null, null, null)
                    .apply { isOurFragment = false }
            }

            val (ourTag, className, detachable, uuid, modal) = tag.split(SEPARATOR)

            if (ourTag != OURTAG) return TagStructure(null, null, null, null)
                .apply { isOurFragment = false }

            return TagStructure(className, DETACHABLE == detachable, uuid, MODAL == modal)
        }
    }
}
