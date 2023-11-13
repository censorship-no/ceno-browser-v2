package ie.equalit.ceno.home

data class RssAnnouncementResponse(
    override val title: String,
    val link: String,
    override val text: String,
    val items: List<RssItem>
) : BaseMessageCard()

data class RssItem(
    var title: String,
    var link: String,
    var guid: String,
    var pubDate: String,
    var description: String
)
