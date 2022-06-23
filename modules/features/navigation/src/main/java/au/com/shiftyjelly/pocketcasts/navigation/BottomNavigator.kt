package au.com.shiftyjelly.pocketcasts.navigation

/**
 * Copyright 2019 Pandora Media, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * See accompanying LICENSE file or you may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import au.com.shiftyjelly.pocketcasts.navigation.FragmentTransactionCommand.AddAndShow
import au.com.shiftyjelly.pocketcasts.navigation.FragmentTransactionCommand.AddOnTop
import au.com.shiftyjelly.pocketcasts.navigation.FragmentTransactionCommand.Clear
import au.com.shiftyjelly.pocketcasts.navigation.FragmentTransactionCommand.RemoveAllAndAdd
import au.com.shiftyjelly.pocketcasts.navigation.FragmentTransactionCommand.RemoveAllAndShowExisting
import au.com.shiftyjelly.pocketcasts.navigation.FragmentTransactionCommand.ShowAndRemove
import au.com.shiftyjelly.pocketcasts.navigation.FragmentTransactionCommand.ShowExisting
import com.google.android.material.bottomnavigation.BottomNavigationView
import hu.akarnokd.rxjava2.subjects.UnicastWorkSubject
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

open class BottomNavigator internal constructor() : ViewModel() {
    internal val fragmentTransactionPublisher = UnicastWorkSubject.create<CommandWithRunnable>()
    internal val bottomnavViewSetSelectedItemObservable = UnicastWorkSubject.create<Int>()
    private val resetRootFragmentSubject = UnicastWorkSubject.create<Fragment>()

    /**
     * Subscribing to this stream changes the behavior of tapping on the current tab when showing a RootFragment.
     * Without being subscribed to this stream tapping on the current tab will discard the root fragment
     * and instantiate a new RootFragment.
     * When subscribed to this stream it will instead send an event to the stream allowing you to reset the fragment yourself.
     * This allows you to, for example, do a smoothScrollToTop or reset other fragment state instead of having the whole fragment recreated.
     */
    open fun resetRootFragmentCommand(): Observable<Fragment> = resetRootFragmentSubject.hide()!!

    private val infoPublisher = PublishSubject.create<NavigatorAction>()

    /**
     * Fragment change events fired when fragment transactions are completed.
     */
    open fun infoStream(): Observable<NavigatorAction> = infoPublisher.hide()!!

    /*
     * Backstack per tab, map of tabs to Fragment tags
     */
    private val tabStackMap = StackOfStacks<Int, TagStructure>()

    // keep track of tab switches
    private val tabSwitches = mutableListOf<NavigatorAction.TabSwitched>()

    private var currentTab = -1
        set(value) {
            tabSwitches.add(NavigatorAction.TabSwitched(newTab = value, previousTab = field))

            field = value
            if (value != -1) {
                bottomnavViewSetSelectedItemObservable.onNext(value)
            }
        }

    private var defaultTab: Int = -1
    private lateinit var rootFragmentsFactory: Map<Int, () -> FragmentInfo>

    internal fun onNavigationItemSelected(menuItem: MenuItem) {
        val tab = menuItem.itemId
        if (currentTab != tab) {
            switchTab(tab)
        } else {
            if (isAtRootOfStack()) {
                val currentFragment = currentFragment()
                val tabstackAndFragmentManagerInSync = currentFragment != null &&
                    tabStackMap.peekValue().toString() == currentFragment.tag

                if (resetRootFragmentSubject.hasObservers() && tabstackAndFragmentManagerInSync) {
                    resetRootFragmentSubject.onNext(currentFragment!!)
                } else {
                    reset(tab, true)
                }
            } else {
                reset(tab, false)
            }
        }
    }

    open fun isShowingModal() = tabStackMap.peek()?.second?.isModal ?: false

    open fun isAtRootOfStack() = tabStackMap[currentTab]?.size == 1

    open fun currentStackSize() = tabStackMap[currentTab]?.size!!

    open fun stackSize(@IdRes tab: Int): Int = tabStackMap[tab]?.size ?: 0

    /**
     * Switch to the specified tab
     */
    open fun switchTab(@IdRes tab: Int) {
        if (currentTab == tab || !rootFragmentsFactory.containsKey(tab)) return

        currentTab = tab

        if (tabStackMap.stackExists(tab)) {
            tabStackMap.moveToTop(tab)
            fragmentCommand(ShowExisting(tabStackMap.peekValue()!!))
        } else {
            if (rootFragmentsFactory.containsKey(tab)) {
                val (rootFragment, isDetachable) = rootFragmentsFactory.getValue(tab)()
                addFragmentInternal(rootFragment, tab, isDetachable, modal = false, onTop = false)
            }
        }
    }

    /**
     * Adds fragment to the current tab.
     *
     * detachable means that the fragment can handle being detached and re-attached from the FragmentManager
     * as the user switches tabs back and forth or puts it in a backstack. When a fragment is detachable
     * onDestroyView is called and then onCreateView when it comes back, this allows the fragment's
     * View to be removed from memory while it's not being shown to reduce the memory pressure on the app.
     * Ideally you desing your fragments to be able to handle detach/attach but in some situations
     * that might not be feasable. By setting detachable = false the fragment's view will be kept
     * and memory and just hidden view.
     */
    open fun addFragment(fragment: Fragment, detachable: Boolean = true, modal: Boolean = false, onTop: Boolean = false) {
        addFragmentInternal(fragment, currentTab, detachable, modal, onTop)
    }

    /**
     * Add fragment to the specified tab and switches to that tab
     */
    private fun addFragmentInternal(fragment: Fragment, @IdRes tab: Int, detachable: Boolean, modal: Boolean, onTop: Boolean) {
        val fragmentTag = TagStructure(fragment, detachable, modal)
        if (currentTab != tab) currentTab = tab
        tabStackMap.push(tab, fragmentTag)
        val command = if (onTop) AddOnTop(fragment, fragmentTag) else AddAndShow(fragment, fragmentTag)
        fragmentCommand(command)
    }

    /**
     * Add fragment to the specified tab and switches to that tab.
     *
     * detachable means that the fragment can handle being detached and re-attached from the FragmentManager
     * as the user switches tabs back and forth or puts it in a backstack. When a fragment is detachable
     * onDestroyView is called and then onCreateView when it comes back, this allows the fragment's
     * View to be removed from memory while it's not being shown to reduce the memory pressure on the app.
     * Ideally you desing your fragments to be able to handle detach/attach but in some situations
     * that might not be feasable. By setting detachable = false the fragment's view will be kept
     * and memory and just hidden view.
     */
    open fun addFragment(fragment: Fragment, @IdRes tab: Int, detachable: Boolean = true) {
        switchTab(tab)
        addFragment(fragment, detachable)
    }

    /**
     * Remove the current fragment. This should normally be called from onBackPressed.
     * This will show the previous fragment in the stack which might cause the bottom tab to be switched.
     * If there are no fragments left to pop returns false in which case you should typically finish the activity.
     */
    open fun pop(): Boolean {
        val popped = tabStackMap.pop() ?: return false
        val peek = tabStackMap.peek()
        return if (peek == null) {
            false
        } else {
            val (tab, nextFragment) = peek
            if (!popped.isModal && currentTab != tab) { // We never want to jump tabs, but we should always still close modals which shouldn't change tabs
                tabStackMap.push(currentTab, popped) // Add it back on to the stack because we are doing nothing with it
                return false
            }
            fragmentCommand(ShowAndRemove(nextFragment, popped))
            true
        }
    }

    /**
     * Clears backstacks on all tabs, resets everything back to the default tab with its default root fragment.
     */
    open fun clearAll() {
        val allTags = tabStackMap.keys().fold(listOf<TagStructure>()) { list, key ->
            list.plus(tabStackMap[key]!!)
        }
        tabStackMap.clear()

        fragmentCommand(Clear(allTags))
        val (rootFragment, isDetachable) = rootFragmentsFactory.getValue(defaultTab)()
        addFragmentInternal(rootFragment, defaultTab, isDetachable, modal = false, onTop = false)
    }

    /**
     * Clears the given tab's stack. Also switches to the specified tab.
     * If resetRootFragment is false the rootFragment is not affected.
     * If it's true the rootFragment is re-created or an event is sent to the resetRootFragmentCommand() stream.
     */
    open fun reset(@IdRes tab: Int, resetRootFragment: Boolean) {
        if (resetRootFragment) {
            val (rootFragment, isDetachable) = rootFragmentsFactory.getValue(tab)()
            addRootFragment(tab, rootFragment, isDetachable)
        } else {
            switchTab(tab)
            val tabStack = tabStackMap.get(tab)
            val toRemove = tabStack?.subList(1, tabStack.size) ?: emptyList()
            if (toRemove.isNotEmpty()) {
                for (i in 0 until toRemove.size) {
                    tabStackMap.pop()
                }
                fragmentCommand(
                    RemoveAllAndShowExisting(toRemove, ShowExisting(tabStackMap.peekValue()!!))
                )
            }
        }
    }

    /**
     * Switches to and clears the given tab's stack and sets the given fragment as the root fragment.
     * If the tab is subsequently reset the original fragment from the rootFragmentMap will be added.
     *
     * detachable means that the fragment can handle being detached and re-attached from the FragmentManager
     * as the user switches tabs back and forth or puts it in a backstack. When a fragment is detachable
     * onDestroyView is called and then onCreateView when it comes back, this allows the fragment's
     * View to be removed from memory while it's not being shown to reduce the memory pressure on the app.
     * Ideally you desing your fragments to be able to handle detach/attach but in some situations
     * that might not be feasable. By setting detachable = false the fragment's view will be kept
     * and memory and just hidden view.
     */
    open fun addRootFragment(@IdRes tab: Int, fragment: Fragment, isDetachable: Boolean = true) {
        val toRemove = tabStackMap.get(tab) ?: emptyList()
        tabStackMap.remove(tab)
        if (currentTab != tab) currentTab = tab
        val fragmentTag = TagStructure(fragment, isDetachable, modal = false)
        tabStackMap.push(tab, fragmentTag)
        fragmentCommand(RemoveAllAndAdd(toRemove, AddAndShow(fragment, fragmentTag)))
    }

    /**
     * Returns the fragment instance currently being shown.
     */
    open fun currentFragment(): Fragment? {
        return activityDelegate?.fragmentManager?.findFragmentByTag(
            tabStackMap.peekValue().toString()
        )
    }

    /**
     * Returns the currently shown tab
     */
    open fun currentTab(): Int {
        return currentTab
    }

    private fun fragmentCommand(command: FragmentTransactionCommand) {
        val infoEvents = tabSwitches.plus(getInfoEvents(command))
        tabSwitches.clear()

        fragmentTransactionPublisher.onNext(
            CommandWithRunnable(command) {
                infoEvents.forEach {
                    infoPublisher.onNext(it)
                }
            }
        )
    }

    private fun getInfoEvents(command: FragmentTransactionCommand): List<NavigatorAction> {
        return when (command) {
            is AddAndShow -> listOf(NavigatorAction.NewFragmentAdded(command.fragment))
            is AddOnTop -> listOf(NavigatorAction.NewFragmentAdded(command.fragment))
            is RemoveAllAndAdd -> {
                command.remove
                    .map {
                        NavigatorAction.FragmentRemoved(it.className, command.add.tag.className)
                    }
                    .plus(NavigatorAction.NewFragmentAdded(command.add.fragment))
            }
            is ShowAndRemove ->
                listOf(
                    NavigatorAction.FragmentRemoved(
                        command.removeTag.className, command.showTag.className
                    )
                )
            is RemoveAllAndShowExisting -> {
                command.remove
                    .map {
                        NavigatorAction.FragmentRemoved(it.className, command.show.tag.className)
                    }
            }
            is ShowExisting -> emptyList() // tracked by tabSwitches
            is Clear -> command.allCurrentTags.map {
                NavigatorAction.FragmentRemoved(
                    it.className, null
                )
            }
            is FragmentTransactionCommand.RemoveUnknown -> emptyList() // no info
        }
    }

    companion object {
        /**
         * Configure the BottomNavigator and returns the Activity-scoped instance.
         * BottomNavigator uses Architecture Component's ViewModel to provide the same instance across configuration changes.
         * `initialize` should be called on every Activity onCreate.
         *
         * When using this method all rootFragment are setup to be detachable. If you need to specify
         * detachability for the root fragments use onCreateWithDetachability.
         *
         * @param activity The activity that hosts the fragment container and the bottomNavigationView
         * @param rootFragmentsFactory A map with a function for generating the top-level fragment for each menuItem id in bottomNavigationView.
         * @param fragmentContainer id of the fragment container in which BottomNavigator will manage the fragments
         * @param bottomNavigationView The BottomNavigationView that will be wired up the fragment container
         * @param defaultTab The menuItem Id of the default landing tab
         */
        @JvmStatic
        fun onCreate(
            activity: FragmentActivity,
            rootFragmentsFactory: Map<Int, () -> Fragment>,
            @IdRes defaultTab: Int,
            @IdRes fragmentContainer: Int,
            @IdRes modalContainer: Int,
            bottomNavigationView: BottomNavigationView
        ): BottomNavigator {
            val navigator = ViewModelProvider(activity).get(BottomNavigator::class.java)
            val fragmentFactoryWithDetachability =
                rootFragmentsFactory.mapValues { { FragmentInfo(it.value(), true) } }
            navigator.onCreate(
                rootFragmentsFactory = fragmentFactoryWithDetachability,
                defaultTab = defaultTab,
                activity = activity,
                fragmentContainer = fragmentContainer,
                modalContainer = modalContainer,
                bottomNavigationView = bottomNavigationView
            )
            return navigator
        }

        /**
         * Configure the BottomNavigator and returns the Activity-scoped instance.
         * BottomNavigator uses Architecture Component's ViewModel to provide the same instance across configuration changes.
         * `initialize` should be called on every activity onCreate.
         *
         * @param activity The activity that hosts the fragment container and the bottomNavigationView
         * @param rootFragmentsFactory A map with a function for generating the top-level FragmentInfo for each menuItem id in bottomNavigationView.
         * The FragmentInfo class allows you to specify the detachibility of each root fragment.
         *
         * isDetachable means that the fragment can handle being detached and re-attached from the FragmentManager
         * as the user switches tabs back and forth or puts it in a backstack. When a fragment is detachable
         * onDestroyView is called and then onCreateView when it comes back, this allows the fragment's
         * View to be removed from memory while it's not being shown to reduce the memory pressure on the app.
         * Ideally you design your fragments to be able to handle detach/attach but in some situations
         * that might not be feasable. By setting detachable = false the fragment's view will be kept
         * and memory and just hidden view.
         *
         * @param fragmentContainer id of the fragment container in which BottomNavigator will manage the fragments
         * @param bottomNavigationView The BottomNavigationView that will be wired up the fragment container
         * @param defaultTab The menuItem Id of the default landing tab
         */
        @JvmStatic
        fun onCreateWithDetachability(
            activity: FragmentActivity,
            rootFragmentsFactory: Map<Int, () -> FragmentInfo>,
            @IdRes defaultTab: Int,
            @IdRes fragmentContainer: Int,
            @IdRes modalContainer: Int,
            bottomNavigationView: BottomNavigationView
        ): BottomNavigator {
            val navigator = ViewModelProvider(activity).get(BottomNavigator::class.java)
            navigator.onCreate(
                rootFragmentsFactory = rootFragmentsFactory,
                defaultTab = defaultTab,
                activity = activity,
                fragmentContainer = fragmentContainer,
                modalContainer = modalContainer,
                bottomNavigationView = bottomNavigationView
            )
            return navigator
        }

        /**
         * Retrieves the Activity-scoped instance of BottomNavigator that has been previously initialized.
         * BottomNavigator uses Architecture Component's ViewModel to provide the same instance across configuration changes.
         */
        @JvmStatic
        fun provide(activity: FragmentActivity): BottomNavigator = ViewModelProvider(activity).get(BottomNavigator::class.java)
    }

    @VisibleForTesting()
    internal var activityDelegate: ActivityDelegate? = null

    private fun onCreate(
        rootFragmentsFactory: Map<Int, () -> FragmentInfo>,
        @IdRes defaultTab: Int,
        activity: FragmentActivity,
        fragmentContainer: Int,
        modalContainer: Int,
        bottomNavigationView: BottomNavigationView
    ) {
        validateInputs(bottomNavigationView, rootFragmentsFactory, defaultTab)
        this.rootFragmentsFactory = rootFragmentsFactory
        this.defaultTab = defaultTab
        if (currentTab == -1) switchTab(defaultTab)
        val fragmentManagerFactory = { activity.supportFragmentManager }

        activityDelegate?.clear()
        activityDelegate = ActivityDelegate(
            fragmentContainer, modalContainer, fragmentManagerFactory, activity.lifecycle, bottomNavigationView,
            this
        )

        cleanupUnknownFragments()
    }

    /**
     * If the app is killed in the background FragmentManager will restore the previous Fragments
     * but BottomNavigator will be unaware of those restored Fragments. This fragmentCommand will
     * remove all fragments that we are unaware of to prevent that memory leak.
     */
    private fun cleanupUnknownFragments() {
        val allKnownTags = tabStackMap.keys().map { tabStackMap[it]!! }.flatMap { it.toList() }
        fragmentCommand(FragmentTransactionCommand.RemoveUnknown(allKnownTags))
    }

    private fun validateInputs(
        bottomNavigationView: BottomNavigationView,
        rootFragmentsFactory: Map<Int, () -> FragmentInfo>,
        defaultTab: Int
    ) {
        val bottomNavItems =
            (0 until bottomNavigationView.menu.size()).map { bottomNavigationView.menu.getItem(it) }
        var foundDefaultTab = false
        bottomNavItems.forEach {
            if (rootFragmentsFactory[it.itemId] == null) {
                throw IllegalArgumentException(
                    "rootFragmentsFactory is missing a the fragment for tab ${it.title}"
                )
            }
            if (it.itemId == defaultTab) foundDefaultTab = true
        }

        if (!foundDefaultTab) throw IllegalArgumentException(
            "defaultTab was not found in the BottomNavigationView"
        )
    }
}

sealed class NavigatorAction {
    data class NewFragmentAdded(val fragment: Fragment) : NavigatorAction()
    data class TabSwitched(@IdRes val newTab: Int, @IdRes val previousTab: Int) : NavigatorAction()
    data class FragmentRemoved(
        val removedFragmentClassName: String?,
        val newShownFragmentClassName: String?
    ) : NavigatorAction()
}

data class FragmentInfo(
    val fragment: Fragment,
    /**
     * isDetachable means that the fragment can handle being detached and re-attached from the FragmentManager
     * as the user switches tabs back and forth or puts it in a backstack. When a fragment is detachable
     * onDestroyView is called and then onCreateView when it comes back, this allows the fragment's
     * View to be removed from memory while it's not being shown to reduce the memory pressure on the app.
     * Ideally you design your fragments to be able to handle detach/attach but in some situations
     * that might not be feasable. By setting detachable = false the fragment's view will be kept
     * and memory and just hidden view.
     */
    val isDetachable: Boolean
)
