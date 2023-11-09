package ie.equalit.ceno.utils

import androidx.core.text.HtmlCompat
import ie.equalit.ceno.home.RssAnnouncementResponse
import ie.equalit.ceno.home.RssItem
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

object XMLParser {

    fun parseRssXml(xmlString: String): RssAnnouncementResponse? {
        val factory: XmlPullParserFactory = XmlPullParserFactory.newInstance()
        val parser: XmlPullParser = factory.newPullParser()

        parser.setInput(StringReader(xmlString))

        // For tracking the current tag while looping across the XML
        var tag = ""

        var currentRssItem: RssItem? = null
        val rssFeedItems = mutableListOf<RssItem>()

        var rssFeedTitle = ""
        var rssFeedLink = ""
        var rssFeedDescription = ""

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    tag = parser.name
                    when (tag) {
                        "item" -> {
                            currentRssItem = RssItem(
                                "",
                                "",
                                "",
                                "",
                                HtmlCompat.fromHtml("", HtmlCompat.FROM_HTML_MODE_LEGACY)
                            )
                        }
                    }
                }

                XmlPullParser.TEXT -> {
                    val text = parser.text.trim()
                    when (tag) {
                        "title" -> {
                            if (currentRssItem == null) {
                                rssFeedTitle = text
                            } else {
                                currentRssItem.title = text
                            }
                        }

                        "link" -> {
                            if (currentRssItem == null) {
                                rssFeedLink = text
                            } else {
                                currentRssItem.link = text
                            }
                        }

                        "description" -> {
                            if (currentRssItem == null) {
                                rssFeedDescription = text
                            } else {
                                currentRssItem.description = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
                            }
                        }

                        "guid" -> {
                            currentRssItem?.guid = text
                        }

                        "pubDate" -> {
                            currentRssItem?.pubDate = text
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    tag = ""
                    when (parser.name) {
                        "item" -> {
                            currentRssItem?.let {
                                rssFeedItems.add(it)
                            }
                        }

                        else -> {} // do nothing
                    }
                }
            }
        }


        // If any of the vital fields is null, return a null response thus hiding the view

        if (rssFeedTitle.isEmpty()
            || rssFeedLink.isEmpty()
            || rssFeedDescription.isEmpty()
            || rssFeedItems.isEmpty()
        ) {
            return null
        }

        return RssAnnouncementResponse(rssFeedTitle, rssFeedLink, rssFeedDescription, rssFeedItems)
    }
}