package ie.equalit.ceno.home.announcements

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import ie.equalit.ceno.home.sessioncontrol.HomePageInteractor

class AnnouncementCardSwipeCallback(
    dragDirs: Int,
    swipeDirs: Int,
    val interactor: HomePageInteractor
) : ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs) {

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        return makeMovementFlags(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        )
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        interactor.onAnnouncementSwiped(viewHolder.absoluteAdapterPosition)
    }
}