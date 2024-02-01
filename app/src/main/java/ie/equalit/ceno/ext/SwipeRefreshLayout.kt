package ie.equalit.ceno.ext

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout


fun SwipeRefreshLayout.setDistanceToTriggerSync(context: Context) {

    val displayMetrics = context.createDisplayContext(
        ((context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)).getDisplay(Display.DEFAULT_DISPLAY)
    ).resources.displayMetrics

    val screenHeight = displayMetrics.heightPixels
    val percentage = 0.8 // percentage of screen height to be swiped on for refresh

    // set percentage to SwipeRefreshLayout
    setDistanceToTriggerSync((screenHeight * percentage).toInt())
}
