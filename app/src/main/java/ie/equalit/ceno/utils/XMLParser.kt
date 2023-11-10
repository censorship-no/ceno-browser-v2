package ie.equalit.ceno.utils

import ie.equalit.ceno.ext.extractLinks
import ie.equalit.ceno.home.RssAnnouncementResponse
import ie.equalit.ceno.home.RssItem
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

object XMLParser {

    fun parseRssXml(xmlString: String): RssAnnouncementResponse? {

        // extract URLs from description
        var formattedXML: String? = xmlString

        var count = 0

        val descriptionUrls = xmlString.extractLinks()
        descriptionUrls.forEach {
            formattedXML = formattedXML?.replace(it, CENO_CUSTOM_PLACEHOLDER)
        }

        val factory: XmlPullParserFactory = XmlPullParserFactory.newInstance()
        val parser: XmlPullParser = factory.newPullParser()

        parser.setInput(StringReader(formattedXML))

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
                                ""
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
                                val occurrences = text.split(CENO_CUSTOM_PLACEHOLDER).size - 1
                                var result = text
                                for(i in 0 until occurrences) {
                                    result = result.replaceFirst(CENO_CUSTOM_PLACEHOLDER, descriptionUrls[count])
                                    count++
                                }
                                currentRssItem.description = result
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

    private const val CENO_CUSTOM_PLACEHOLDER = "ceno_custom_placeholder"
}