package com.flashsphere.rainwaveplayer.view.uistate

import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.model.CategoryState

data class CategoryScreenState(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val category: CategoryState? = null,
    val error: OperationError? = null,
) {

    companion object {
        fun init(category: CategoryState): CategoryScreenState {
            return CategoryScreenState(loading = false, loaded = false, category = category, error = null)
        }

        fun loading(state: CategoryScreenState): CategoryScreenState {
            return state.copy(loading = true, error = null)
        }

        fun loaded(category: CategoryState): CategoryScreenState {
            return CategoryScreenState(loading = false, loaded = true, category = category, error = null)
        }

        fun error(state: CategoryScreenState, error: OperationError): CategoryScreenState {
            return state.copy(loading = false, error = error)
        }

    }
}
