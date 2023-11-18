/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package ie.equalit.ceno

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtension.MessageDelegate
import org.mozilla.geckoview.WebExtension.PortDelegate

class MessagingActivity : AppCompatActivity() {
    private var mPort: WebExtension.Port? = null
    @SuppressLint("MissingInflatedId", "WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messaging)
        val view = findViewById<GeckoView>(R.id.geckoview)
        val button = findViewById(R.id.btn_onboarding_start) as Button

        val session = GeckoSession()
        sRuntime = EngineProvider.getOrCreateRuntime(this)

        val portDelegate: PortDelegate = object : PortDelegate {
            override fun onPortMessage(
                message: Any, port: WebExtension.Port
            ) {
                Log.d("PortDelegate", "Received message from extension: $message")
            }

            override fun onDisconnect(port: WebExtension.Port) {
                // This port is not usable anymore.
                if (port === mPort) {
                    mPort = null
                }
            }
        }

        val messageDelegate: MessageDelegate = object : MessageDelegate {
            @Nullable
            override fun onConnect(port: WebExtension.Port) {
                mPort = port
                mPort!!.setDelegate(portDelegate)
            }
        }

        sRuntime!!
            .webExtensionController
            .ensureBuiltIn(EXTENSION_LOCATION, EXTENSION_ID)
            .accept( // Register message delegate for background script
                { extension -> extension!!.setMessageDelegate(messageDelegate, "browser") }
            ) { e -> Log.e("MessageDelegate", "Error registering WebExtension", e) }

        button.setOnClickListener{
            val message = JSONObject()
            message.put("count", count)
            mPort!!.postMessage(message)
            count++
        }

        session.open(sRuntime!!)
        view.setSession(session)
        session.loadUri("https://www.example.com")
    }

    companion object {
        private var sRuntime: GeckoRuntime? = null
        private var count = 0
        private const val EXTENSION_LOCATION = "resource://android/assets/addons/ceno/"
        private const val EXTENSION_ID = "ceno@equalit.ie"
    }
}