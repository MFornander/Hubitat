/**
 * ****************  WD200 Condition ********************
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
 * Description: "Turn your HS-WD200 Dimmer into a mini-dashboard"
 * Hubitat child app to be installed along with the "WD200 Dashboard" parent app.
 * See parent app for more information.
 *
 * Versions:
 *  1.0.0 - 2020-05-xx - Initial release.
 */

def getVersion() {
    "0.0.8"
}

definition(
    name: "WD200 Condition",
    namespace: "MFornander",
    author: "Mattias Fornander",
    description: "WD200 Dashboard Child App",
    parent: "MFornander:WD200 Dashboard",
    importUrl: "https://raw.githubusercontent.com/MFornander/Hubitat/master/apps/wd200dashboard/wd200dashboard-child.groovy",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
    page(name: "pageConfig")
}

def pageConfig() {
    dynamicPage(name: "", title: "WD200 Condition", install: true, uninstall: true, refreshInterval: 0) {
        section() {
            label title: "Condition Name", required: true
        }
        section("<b>LED Indicator</b>") {
            input name: "index", type: "number", title: "Index (1-7:bottom to top LED)", range: "1..7", required: true
            input name: "color", type: "enum", title: "Color", required: true,
                options: [
                    1i: "Red",
                    5i: "Yellow",
                    2i: "Green",
                    6i: "Cyan",
                    3i: "Blue",
                    4i: "Magenta",
                    7i: "White",
                    0i: "Off"
                ]
            input name: "priority", type: "number", title: "Priority (higher overrides lower conditions)", defaultValue: "0"
        }
        section("<b>Input</b>") {
            input name: "sensorType", type: "enum", title: "Sensor Type", required: true, submitOnChange: true,
                options: [
                    "on": "Always On",
                    "switch": "Switch",
                    "contact": "Door/Window Sensor",
                    "lock": "Lock",
                    "motion": "Motion Sensor",
                    "water": "Water Sensor"
                ]
            if (sensorType && sensorType != "on") {
                input name: "sensorList", type: "capability.${capabilityName(sensorType)}", title: "Sensor", required: true, multiple: false
                input name: "sensorState", type: "enum", title: "State", required: true,
                    options: attributeValues(sensorType)
            }
        }
    }
}

def installed() {
    logDebug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    logDebug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def uninstalled() {
    logDebug "Uninstalled with settings: ${settings}"
    initialize()
}

private initialize() {
    logDebug "Initialize with settings:${settings}, state:${state}"

    atomicState.active = sensorType == "on"
    switch (sensorType) {
        case [null, "on"]:
            break
        default:
            logDebug "${sensorList} ${sensorType} ${atomicState}"
            subscribe(sensorList, sensorType, sensorHandler)
            break
    }
    parent.refreshConditions()
}

def sensorHandler(evt) {
    atomicState.active = evt.value == sensorState
    logDebug "sensorHandler evt.value:${evt.value}, state:${atomicState}, sensorState:${sensorState}"
    parent.refreshConditions()
}

def addCondition(leds) {
    logDebug "addCondition ${atomicState} ${settings}"
    if (!atomicState.active) return
    if (!leds[index as int] || (leds[index as int].priority < priority)) leds[index as int] = [color: color, priority: priority]
}

private logDebug(msg) {
    if (parent.debugEnable) log.debug msg
}

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
            return ["unlocked", "locked"] // TODO: Add "unknown" and other states... Solve UI issues...
        default:
            return ["UNDEFINED"]
    }
}

private capabilityName(attributeName) {
    switch (attributeName) {
        case ["switch", "lock"]:
            return attributeName
        default:
            return "${attributeName}Sensor"
    }
}
