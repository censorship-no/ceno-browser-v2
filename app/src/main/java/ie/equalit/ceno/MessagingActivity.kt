/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package ie.equalit.ceno

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtension.MessageDelegate
import org.mozilla.geckoview.WebExtension.MessageSender

class MessagingActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId", "WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messaging)
        val view = findViewById<GeckoView>(R.id.geckoview)
        val session = GeckoSession()
        sRuntime = EngineProvider.getOrCreateRuntime(this)

        val messageDelegate: MessageDelegate = object : MessageDelegate {
            override fun onMessage(
                nativeApp: String,
                message: Any,
                sender: MessageSender
            ): GeckoResult<Any>? {
                print("GOT MESSAGE")
                Log.d("MessageDelegate", "Got message for $nativeApp from $sender: $message ")
                if (message is JSONObject) {
                    val json = message
                    try {
                        if (json.has("type") && ("WPAManifest" === json.getString("type"))) {
                            val manifest = json.getJSONObject("manifest")
                            Log.d("MessageDelegate", "Found WPA manifest: $manifest")
                        }
                    } catch (ex: JSONException) {
                        Log.e("MessageDelegate", "Invalid manifest", ex)
                    }
                }
                return null
            }
        }

        // Let's make sure the extension is installed
        sRuntime!!
            .webExtensionController
            .ensureBuiltIn(EXTENSION_LOCATION, "ceno@equalit.ie")
            .accept( // Set delegate that will receive messages coming from this extension.
                { extension: WebExtension? ->
                    session
                        .webExtensionController
                        .setMessageDelegate(extension!!, messageDelegate, "browser")
                }
            )  // Something bad happened, let's log an error
            { e: Throwable? ->
                Log.e(
                    "MessageDelegate",
                    "Error registering extension",
                    e
                )
            }
        session.open(sRuntime!!)
        view.setSession(session)
        session.loadUri("https://www.example.com")
    }

    companion object {
        private var sRuntime: GeckoRuntime? = null
        private const val EXTENSION_LOCATION = "resource://android/assets/addons/ceno/"
    }
}