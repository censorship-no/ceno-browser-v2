package ie.equalit.ceno.share

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.feature.share.RecentAppsStorage

class LogsShareController(
    private val context: Context,
    private val logsUri: Uri,
    private val navController: NavController,
    private val viewLifecycleScope: CoroutineScope,
    private val recentAppsStorage: RecentAppsStorage,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val dismiss: (ShareController.Result) -> Unit,
):ShareController {
    override fun handleShareClosed() {
        dismiss(ShareController.Result.DISMISSED)
    }

    override fun handleShareToApp(app: AppShareOption) {
        viewLifecycleScope.launch(dispatcher) {
            recentAppsStorage.updateRecentApp(app.activityName)
        }
        val intent = Intent(Intent.ACTION_SEND)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.setType("*/*")
        intent.putExtra(Intent.EXTRA_STREAM, logsUri)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setClassName(app.packageName, app.activityName)
        @Suppress("TooGenericExceptionCaught")
        val result = try {
            context.startActivity(intent)
            ShareController.Result.SUCCESS
        } catch (e: Exception) {
            when (e) {
                is SecurityException, is ActivityNotFoundException -> {
                    ShareController.Result.SHARE_ERROR
                }
                else -> throw e
            }
        }
        dismiss(result)
    }

    override fun handleSaveToPDF(tabId: String?) {
    }

    override fun handlePrint(tabId: String?) {
    }
}