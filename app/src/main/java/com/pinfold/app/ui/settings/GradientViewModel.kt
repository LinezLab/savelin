package com.pinfold.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pinfold.app.data.settings.SettingsRepository
import com.pinfold.app.ui.background.BuiltInGradients
import com.pinfold.app.ui.background.DEFAULT_GRADIENT_ID
import com.pinfold.app.ui.background.GradientPreset
import com.pinfold.app.ui.background.generatePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GradientViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SettingsRepository(app)
    private val json = Json { ignoreUnknownKeys = true }
    private val defaultPreset = BuiltInGradients.first { it.id == DEFAULT_GRADIENT_ID }

    private val _savedPresets = MutableStateFlow<List<GradientPreset>>(emptyList())
    val savedPresets: StateFlow<List<GradientPreset>> = _savedPresets.asStateFlow()

    private val _selectedPreset = MutableStateFlow(defaultPreset)
    val selectedPreset: StateFlow<GradientPreset> = _selectedPreset.asStateFlow()

    private var generated: GradientPreset? = null

    init {
        viewModelScope.launch {
            repo.settings
                .map { it.settingsGradientPresetId to it.settingsGradientPresetsJson }
                .distinctUntilChanged()
                .collect { (selectedId, presetsJson) ->
                    _savedPresets.value = decodePresets(presetsJson)
                    _selectedPreset.value = allPresets().firstOrNull { it.id == selectedId } ?: defaultPreset
                }
        }
    }

    private fun decodePresets(raw: String): List<GradientPreset> =
        runCatching { json.decodeFromString<List<GradientPreset>>(raw) }.getOrDefault(emptyList())

    private fun allPresets(): List<GradientPreset> =
        BuiltInGradients + _savedPresets.value + listOfNotNull(generated)

    fun select(id: String) {
        val preset = allPresets().firstOrNull { it.id == id } ?: return
        _selectedPreset.value = preset
        viewModelScope.launch { repo.set("settingsGradientPresetId", preset.id) }
    }

    fun save(list: List<GradientPreset>) {
        _savedPresets.value = list
        generated = null
        viewModelScope.launch { repo.set("settingsGradientPresetsJson", json.encodeToString(list)) }
    }

    fun generate() {
        val preset = generatePreset(System.currentTimeMillis())
        generated = preset
        select(preset.id)
    }
}
