package ie.equalit.ceno.home


data class CenoMessageCard(
    override val title: String, override val text: String,
    val showMessage: Boolean = false
) : BaseMessageCard()
