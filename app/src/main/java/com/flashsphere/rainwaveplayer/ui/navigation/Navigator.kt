package com.flashsphere.rainwaveplayer.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Handles navigation events (forward and back) by updating the navigation state.
 */
class Navigator(val state: NavigationState) {
    val currentRouteFlow = MutableStateFlow(getCurrentStack().last())
    private val onDestinationChangedListeners = mutableSetOf<OnDestinationChangedListener>()

    fun navigate(route: Route) {
        if (route in state.backStacks.keys) {
            // This is a top level route, just switch to it
            state.topLevelRoute = route
        } else {
            state.backStacks[state.topLevelRoute]?.add(route)
        }

        getCurrentStack().last().let {
            updateCurrentRouteFlow(it)
            notifyListeners(it)
        }
    }

    fun goBackToTopLevel() {
        val currentStack = getCurrentStack()
        currentStack.removeAll { it != state.topLevelRoute }

        updateCurrentRouteFlow(state.topLevelRoute)
        notifyListeners(state.topLevelRoute)
    }

    fun goBack() {
        val currentStack = getCurrentStack()
        val currentRoute = currentStack.last()

        // If we're at the base of the current route, go back to the start route stack.
        if (currentRoute == state.topLevelRoute) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }

        currentStack.last().let {
            updateCurrentRouteFlow(it)
            notifyListeners(it)
        }
    }

    private fun getCurrentStack(): NavBackStack<NavKey> {
        return state.backStacks[state.topLevelRoute]
            ?: error("Stack for ${state.topLevelRoute} not found")
    }

    fun addOnDestinationChangedListener(listener: OnDestinationChangedListener) {
        onDestinationChangedListeners.add(listener)

        listener.onDestinationChanged(getCurrentStack().last())
    }

    fun removeOnDestinationChangedListener(listener: OnDestinationChangedListener) {
        onDestinationChangedListeners.remove(listener)
    }

    private fun updateCurrentRouteFlow(currentRoute: NavKey) {
        currentRouteFlow.value = currentRoute
    }

    private fun notifyListeners(destination: NavKey) {
        for (listener in onDestinationChangedListeners) {
            listener.onDestinationChanged(destination)
        }
    }

    interface OnDestinationChangedListener {
        fun onDestinationChanged(destination: NavKey)
    }
}
