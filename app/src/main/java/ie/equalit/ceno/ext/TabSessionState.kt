package ie.equalit.ceno.ext

import mozilla.components.browser.state.state.TabSessionState


/**
 * Returns the URL of the [TabSessionState].
 */
fun TabSessionState.getUrl(): String? {
    return if (this.readerState.active) {
        this.readerState.activeUrl
    } else {
        this.content.url
    }
}