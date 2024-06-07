package ie.equalit.ceno.share

/**
 * Callbacks for possible user interactions on the [SaveToPDFItem]
 */
interface SaveToPDFInteractor {
    /**
     * Generates a PDF from the given [tabId].
     * @param tabId The ID of the tab to save as PDF.
     */
    fun onSaveToPDF(tabId: String?)

    /**
     * Prints from the given [tabId].
     * @param tabId The ID of the tab to print.
     */
    fun onPrint(tabId: String?)
}

