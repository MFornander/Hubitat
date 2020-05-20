/**
 * **********************  Switch Dashboard Condition **********************
 *
 * MIT License - see full license in repository LICENSE file
 * Copyright (c) 2020 Mattias Fornander (@mfornander)
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Description: "Turn your LED switches into mini-dashboards"
 * Hubitat child app to be installed with "Switch Dashboard" parent app.
 *
 * See parent app for version history and more information.
 */

/// Expose child app version to allow version mismatch checks between child and parent
def getVersion() {
    "1.6.0"
}

/// Set app Metadata for the Hub
definition(
    name: "Switch Dashboard Condition",
    namespace: "MFornander",
    author: "Mattias Fornander",
    description: "Switch Dashboard Child App",
    parent: "MFornander:Switch Dashboard",
    importUrl: "https://raw.githubusercontent.com/MFornander/Hubitat/master/apps/SwitchDashboard/SwitchDashboard-child.groovy",
    iconUrl: "",
    iconX2Url: ""
)

/// Defer preference layout to the pageConfig function
preferences {
    page(name: "pageConfig")
}

/**
 * Settings layout function that builds UI to config condition.
 *
 * Using dynamicPage to immediately show a list of available sensors of that
 * type and their possible values.  refreshInterval is zero since no
 * automatic refresh is needed, only when sensorType is updated.  Depending
 * on sensorType selection we use the capabilityName and attributeValues
 * helper functions to limit the selection of that sensorType and show
 * a list of possible values for that type.
 */
def pageConfig() {
    dynamicPage(name: "", title: "Switch Dashboard Condition", install: true, uninstall: true, refreshInterval: 0) {
        section() {
            label title: "Condition Name", required: true
        }
        section("<b>LED Indicator</b>") {
            input name: "color", type: "enum", title: "Color", required: true,
                options: ["Red", "Yellow", "Green", "Cyan", "Blue", "Magenta", "White", "Off"]
            input name: "blink", type: "bool", title: "Blink", defaultValue: false
            input name: "index", type: "number", title: "Index (1-7: bottom to top on HomeSeer Dimmers)", defaultValue: "1", range: "1..7", required: true
            input name: "priority", type: "number", title: "Priority (higher overrides lower conditions)", defaultValue: "0"
            input name: "inovelliFlag", type: "bool", title: "Use alternate Inovelli notification", defaultValue: false, submitOnChange: true, description: "yoyoy"
            if (inovelliFlag) {
                paragraph """<p>\
Inovelli Gen2 switches and dimmers allow for more detailed LED settings.  If the option above is enabled, the settings below are used \
on Inovelli switches instead of the regular Color/Blink options.  Note that Chase is not available on the switch model so Pulse will \
be used on those instead.  Also note that the regular Color/Blink is still used on HomeSeer switches if the Condition is active so you \
may end up with one color on Inovellis and another color on Homeseers if those are your settings.  Lastly, saturation is not available \
so even though the color selection allows you to select pastell and white colors, the switches won't show it.  For now, just use the hue \
(rainbow bar) and brighness (on the right edge of the square only.</p>"""
                input name: "inovelliEffect", type: "enum", title: "Effect", defaultValue: 1, required: true, width: 5,
                    options: [
                        0: "Off",
                        1: "Solid",
                        3: "Fast Blink",
                        4: "Slow Blink",
                        5: "Pulse",
                        2: "Chase"
                    ]
                input name: "inovelliColor", type: "color", title: "Color", defaultValue: "#ff0000", required: true, width: 5
            }
        }
        section("<b>Input</b>") {
            input name: "sensorType", type: "enum", title: "Sensor Type", required: true, submitOnChange: true,
                options: [
                    "switch": "Switch",
                    "contact": "Door/Window Sensor",
                    "lock": "Lock",
                    "motion": "Motion Sensor",
                    "water": "Water Sensor"
                ]
            if (sensorType) {
                input name: "sensorList", type: "capability.${capabilityName(sensorType)}", title: "Sensors (any will trigger)", required: true, multiple: true
                input name: "sensorState", type: "enum", title: "State", required: true, multiple: true,
                    options: attributeValues(sensorType)
            }
        }
    }
}

/**
 * Called after app is initially installed to immediately show the new
 * condition on the LEDs.
 */
def installed() {
    logDebug "Installed with settings: ${settings}"
    initialize()
}

/**
 * Called after any of the configuration settings are changed to allow
 * immediate changes to the LEDs.
 */
def updated() {
    logDebug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

/**
 * Called when app is uninstalled since we may have to turn off the LED this
 * condition was using, or allow a different condition with lower priority
 * to now show its color selection.
 */
def uninstalled() {
    logDebug "Uninstalled with settings: ${settings}"
    initialize()
}

/**
 * Shared helper function used by installed, updated, and uninstalled.
 *
 * All three functions above have a common requirement that the LED states
 * need to be updated.  The atomicState.active is updated for this condition
 * to reflect the the current settings.  Depending on sensors selected in
 * the configuration UI, we subscribe to those sensors.
 *
 * Finally we also trigger a refresh of the parent app.  This causes a full
 * query of all conditions since a change in priority, active state, or even
 * complte removal of this condition may cause another condition to be shown
 * if we are now inactive or lower priority.  It may be possible to cache
 * this better in the parent but...
 *
 * "There are only two hard things in Computer Science: cache invalidation
 * and naming things."" -- Phil Karlton
 */
private initialize() {
    logDebug "Initialize with settings:${settings}, state:${state}"
    updateActive()
    subscribe(sensorList, sensorType, sensorHandler)
    parent.refreshDashboard()
}

/**
 * TODO
 */
private long calclulateConfigValue(inovelli) {
    int red = Integer.parseInt(inovelliColor.substring(1, 3), 16)
    int green = Integer.parseInt(inovelliColor.substring(3, 5), 16)
    int blue = Integer.parseInt(inovelliColor.substring(5, 7), 16)

    long hue = getHue(red, green, blue)
    long bright = Math.round(Math.max(Math.max(red, green), blue) * 10 / 255)

    long value = hue + (bright << 8) + (0xFF << 16) + (inovelliEffect << 24)
    logDebug "CALC Inovelli:${inovelli}  RGB:$red,$green,$blue  Hue:$hue  Bright:$bright"
}

/**
 * TODO
 */
private int getHue(int red, int green, int blue) {
    float min = Math.min(Math.min(red, green), blue)
    float max = Math.max(Math.max(red, green), blue)
    if (min == max) return 0

    float hue = 0f
    if (max == red) {
        hue = (green - blue) / (max - min)
    } else if (max == green) {
        hue = 2f + (blue - red) / (max - min)
    } else {
        hue = 4f + (red - green) / (max - min)
    }

    hue = hue * 60
    if (hue < 0) hue = hue + 360
    Math.round(hue * 256 / 360 % 256)
}

/**
 * Function called if a sensor was selected in the UI and it detected a
 * change. Any change triggers a full refresh on the parent for it to
 * figure out the new correct LED dashboard.
 */
def sensorHandler(evt) {
    updateActive()
    logDebug "sensorHandler evt.value:${evt.value}, state:${atomicState}, sensorState:${sensorState}"
    parent.refreshDashboard()
}

/**
 * Condition is active if any of the sensor values match any of the
 * sensorState in the settings.  atomicState is used since the parent
 * will read the value immedately and a regular state much have some
 * kind of cache since it gives old values to the parent.
 */
private updateActive() {
    atomicState.active = sensorList.any { sensorIt -> sensorState.any { it == sensorIt.latestValue(sensorType) } }
}

/**
 * Update function used by the parent's refreshConditions() function.
 * It is given a map of the new LED dashboard and replaces the map slot
 * with this condition's color if the condition is active and has a higher
 * priority than the current color, if any.
*/
def addCondition(leds) {
    logDebug "Condition ${app.label}: ${atomicState} ${settings}"
    if (!atomicState.active) return
    if (!leds[index as int] || (leds[index as int].priority < priority)) {
        leds[index as int] = [color: color, priority: priority, blink: blink]
        if (inovelliFlag && index == 1) leds[index as int].inovelli = [effect: inovelliEffect, color: inovelliColor]
    }
}

/**
 * Internal logging function that tracks the parent debug setting to
 * allow debugging of an app and all its atached child apps.
 */
private logDebug(msg) {
    if (parent.debugEnable) log.debug msg
}

/**
 * Internal helper function providing lists of possible values given
 * a specific attribute name.  This allows the config UI to dynamically
 * show a list of possible sensor values depending on sensor type selection.
 */
private attributeValues(attributeName) {
    switch (attributeName) {
        case "switch":
            return ["on","off"]
        case "contact":
            return ["open","closed"]
        case "motion":
            return ["active","inactive"]
        case "water":
            return ["wet","dry"]
        case "lock":
            return ["unlocked", "unlocked with timeout", "unknown", "locked"]
        default:
            return ["UNDEFINED"]
    }
}

/**
 * Internal helper function allowing the config UI to filter the selection
 * of devices depending on sensor type selection.
 */
private capabilityName(attributeName) {
    switch (attributeName) {
        case ["switch", "lock"]:
            return attributeName
        default:
            return "${attributeName}Sensor"
    }
}
