package ie.equalit.ceno.home

data class RssAnnouncementResponse(
    override val title: String,
    val link: String = "",
    val description: String = "",
    val item: RssAnnouncementItem
) : BaseMessageCard() {
    override val text: String
        get() = item.description
}

data class RssAnnouncementItem(
    val title: String,
    val guid: String,
    val link: String = "",
    val pubDate: String = "",
    val description: String = ""
)
