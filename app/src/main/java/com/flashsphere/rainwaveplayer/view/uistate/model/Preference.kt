package com.flashsphere.rainwaveplayer.view.uistate.model

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshotFlow
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flashsphere.rainwaveplayer.util.PreferenceKey
import com.flashsphere.rainwaveplayer.util.get
import com.flashsphere.rainwaveplayer.util.getBlocking
import com.flashsphere.rainwaveplayer.util.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

enum class PreferenceType {
    Category,
    Basic,
    Checkbox,
    List,
    Text,
}

abstract class PreferenceItem<T>(
    scope: CoroutineScope,
    dataStore: DataStore<Preferences>,
    val key: PreferenceKey<T>,
    value: T,
    override val title: String,
): Preference {
    val state = MutableStateFlow(value)

    init {
        state
            .filter { it != dataStore.get(key) }
            .onEach {
                Timber.d("Saving '%s' with value '%s'", key.key, it)
                dataStore.update(key, it)
            }
            .launchIn(scope)
    }
}

@Immutable
class CheckBoxPreferenceItem(
    scope: CoroutineScope,
    dataStore: DataStore<Preferences>,
    key: PreferenceKey<Boolean>,
    value: Boolean = dataStore.getBlocking(key.key, key.defaultValue),
    title: String,
    val options: List<PreferenceItemValue<Boolean>>,
): PreferenceItem<Boolean>(scope, dataStore, key, value, title) {
    override val type: PreferenceType = PreferenceType.Checkbox
    val option get() = options.find { it.value == state.value }
}

@Immutable
class ListPreferenceItem(
    scope: CoroutineScope,
    dataStore: DataStore<Preferences>,
    key: PreferenceKey<String>,
    value: String = dataStore.getBlocking(key.key, key.defaultValue),
    title: String,
    val options: List<PreferenceItemValue<String>>,
): PreferenceItem<String>(scope, dataStore, key, value, title) {
    override val type: PreferenceType = PreferenceType.List
}

@Immutable
abstract class TextPreferenceItem<T>(
    scope: CoroutineScope,
    dataStore: DataStore<Preferences>,
    key: PreferenceKey<T>,
    value: T = dataStore.getBlocking(key.key, key.defaultValue),
    title: String,
    val summary: String,
    val validator: (value: String) -> Boolean,
    val valueToString: (value: T) -> String,
    val stringToValue: (value: String) -> T,
): PreferenceItem<T>(scope, dataStore, key, value, title) {
    val textFieldState = TextFieldState(valueToString(state.value))
    override val type: PreferenceType = PreferenceType.Text

    init {
        scope.launch {
            snapshotFlow { textFieldState.text }
                .collectLatest {
                    if (it.isEmpty()) {
                        state.value = key.defaultValue
                    } else {
                        state.value = stringToValue(it.toString())
                    }
                }
        }
    }
}

@Immutable
class FloatPreferenceItem(
    scope: CoroutineScope,
    dataStore: DataStore<Preferences>,
    key: PreferenceKey<Float>,
    value: Float = dataStore.getBlocking(key.key, key.defaultValue),
    title: String,
    summary: String,
    validator: (value: String) -> Boolean,
    valueToString: (value: Float) -> String,
    stringToValue: (value: String) -> Float,
): TextPreferenceItem<Float>(
    scope, dataStore, key, value, title, summary, validator, valueToString, stringToValue
) {
    override val type: PreferenceType = PreferenceType.Text
}

@Immutable
class BasicPreferenceItem(
    override val title: String,
    val summary: String,
    val onClick: () -> Unit,
): Preference {
    override val type: PreferenceType = PreferenceType.Basic
}

@Immutable
class PreferenceItemValue<T>(
    val value: T,
    val label: String,
    val summary: String = label,
)

@Immutable
class PreferenceCategoryItem(
    override val title: String
): Preference {
    override val type: PreferenceType = PreferenceType.Category
}

interface Preference {
    val type: PreferenceType
    val title: String
}
