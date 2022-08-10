/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package ie.equalit.cenoV2.ext

import androidx.test.uiautomator.SearchCondition
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import org.junit.Assert.assertNotNull
import ie.equalit.cenoV2.helpers.TestAssetHelper.waitingTime

/**
 * Wait for an [UiObject2] and perform an interaction on it.
 *
 * @throws AssertionError if no [UiObject2] matches the condition
 */
fun UiDevice.waitAndInteract(condition: SearchCondition<UiObject2>, interaction: UiObject2.() -> Unit) {
    val obj = this.wait(condition, waitingTime)
    assertNotNull(obj)
    interaction.invoke(obj)
    obj.recycle()
}
