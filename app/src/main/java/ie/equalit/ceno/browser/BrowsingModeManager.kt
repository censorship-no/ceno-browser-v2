package ie.equalit.ceno.browser

import ie.equalit.ceno.utils.CenoPreferences

interface BrowsingModeManager {
    var mode: BrowsingMode
}

/**
 * Enum that represents whether or not private browsing is active.
 */
enum class BrowsingMode {
    Normal, Personal;

    /**
     * Returns true if the [BrowsingMode] is [Personal]
     */
    val isPersonal get() = this == Personal

    companion object {

        /**
         * Convert a boolean into a [BrowsingMode].
         * True corresponds to [Personal] and false corresponds to [Normal].
         */
        fun fromBoolean(isPersonal: Boolean) = if (isPersonal) Personal else Normal
    }
}

class DefaultBrowsingManager(
    private var _mode: BrowsingMode,
    private val settings: CenoPreferences,
    private val modeDidChange: (BrowsingMode) -> Unit,
) : BrowsingModeManager {

    override var mode: BrowsingMode
        get() = _mode
        set(value) {
            _mode = value
            modeDidChange(value)
            settings.lastKnownBrowsingMode = value
        }
}