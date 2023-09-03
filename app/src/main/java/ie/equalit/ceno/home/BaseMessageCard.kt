package ie.equalit.ceno.home

import android.graphics.drawable.Drawable

sealed class BaseMessageCard {
    abstract val title: String
    abstract val text: String
}
