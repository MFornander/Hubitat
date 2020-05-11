/**
 * ****************  Dimmer Dashboard Condition ********************
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
 * Description: "Turn your LED Dimmers into mini-dashboards"
 * Hubitat child app to be installed along with the "DimmerDashboard" parent app.
 * See parent app for more information.
 *
 * Versions:
 *  1.0.0 - 2020-05-xx - Initial release.
 */

/// Expose child app version to allow version mismatch checks between child and parent
def getVersion() {
    "0.0.16"
}

/// Set app Metadata for the Hub
definition(
    name: "Dimmer Dashboard Condition",
    namespace: "MFornander",
    author: "Mattias Fornander",
    description: "Dimmer Dashboard Child App",
    parent: "MFornander:Dimmer Dashboard",
    importUrl: "https://raw.githubusercontent.com/MFornander/Hubitat/master/apps/DimmerDashboard/DimmerDashboard-child.groovy",
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
    dynamicPage(name: "", title: "DimmerDashboard Condition", install: true, uninstall: true, refreshInterval: 0) {
        section() {
            label title: "Condition Name", required: true
        }
        section("<b>LED Indicator</b>") {
            input name: "color", type: "enum", title: "Color", required: true,
                options: [
                    "1": "Red",
                    "5": "Yellow",
                    "2": "Green",
                    "6": "Cyan",
                    "3": "Blue",
                    "4": "Magenta",
                    "7": "White",
                    "0": "Off"
                ]
            input name: "index", type: "number", title: "Index (1-7:bottom to top LED)", defaultValue: "1", range: "1..7", required: true
            input name: "priority", type: "number", title: "Priority (higher overrides lower conditions)", defaultValue: "0"
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
 * sensorState in the settings.
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
    if (!leds[index as int] || (leds[index as int].priority < priority))
        leds[index as int] = [color: color, priority: priority]
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
