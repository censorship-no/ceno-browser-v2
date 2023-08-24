package ie.equalit.ceno.home

import android.graphics.drawable.Drawable

data class CenoMessageCard(
    override val title: String, override val text: String,
    val showMessage: Boolean = false
) : BaseMessageCard()
