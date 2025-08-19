package com.flashsphere.rainwaveplayer.view.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashsphere.rainwaveplayer.autovote.v1.Condition
import com.flashsphere.rainwaveplayer.autovote.v1.Rule
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.repository.RulesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AutoVoteViewModel @Inject constructor(
    private val rulesRepository: RulesRepository,
) : ViewModel() {
    val rules = mutableStateListOf<Rule>()

    init {
        viewModelScope.launchWithDefaults("Load auto vote rules") {
            rules.addAll(rulesRepository.get())
        }
    }

    fun addRule(rule: Rule) {
        rules.add(rule)
        saveToDataStore(rules.toList())
    }

    fun updateRule(rule: Rule, newConditions: List<Condition>) {
        val index = rules.indexOfFirst { it.id == rule.id }
        if (index != -1) {
            rules[index] = rules[index].copy(conditions = newConditions)
            saveToDataStore(rules.toList())
        }
    }

    fun reorderRule(fromIndex: Int, toIndex: Int) {
        val fromItem = rules[fromIndex]
        rules[fromIndex] = rules[toIndex]
        rules[toIndex] = fromItem
    }

    fun deleteRule(rule: Rule, index: Int): Boolean {
        if (rules[index] == rule) {
            rules.removeAt(index)
            saveToDataStore(rules.toList())

            return true
        }
        return false
    }

    fun reorderRules() {
        saveToDataStore(rules.toList())
    }

    private fun saveToDataStore(rules: List<Rule>) {
        viewModelScope.launchWithDefaults("Save auto vote rules") {
            rulesRepository.save(rules.toList())
        }
    }
}
