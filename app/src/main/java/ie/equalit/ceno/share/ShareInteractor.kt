package ie.equalit.ceno.share

import mozilla.components.concept.sync.Device

/**
 * Interactor for the share screen.
 */
class ShareInteractor(
    private val controller: ShareController,
) : ShareCloseInteractor,
    ShareToAppsInteractor,
    SaveToPDFInteractor {
    override fun onShareClosed() {
        controller.handleShareClosed()
    }

    override fun onShareToApp(appToShareTo: AppShareOption) {
        controller.handleShareToApp(appToShareTo)
    }

    override fun onSaveToPDF(tabId: String?) {
        controller.handleSaveToPDF(tabId)
    }
    override fun onPrint(tabId: String?) {
        controller.handlePrint(tabId)
    }
}
