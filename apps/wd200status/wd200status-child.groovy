/**
 * ****************  WD200 Status Condition ********************
 *
 * MIT License - see full license in repository LICENSE file
 *
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
 * TODO: Documentation
 *
 * Versions:
 *  1.0.0 - 2020-05-xx - Initial release.
 */

private setVersion() {
    state.name = "WD200 Condition"
    state.version = "0.0.4"
}

definition(
    name: "WD200 Condition",
    namespace: "MFornander",
    author: "Mattias Fornander",
    description: "TODO",
    parent: "MFornander:WD200 Status",
    importUrl: "https://raw.githubusercontent.com/MFornander/Hubitat/master/apps/wd200status/wd200status-child.groovy",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
    page(name: "pageConfig")
}

def pageConfig() {
    dynamicPage(name: "", title: "", install: true, uninstall: true, refreshInterval:0) {
        setVersion()
        section("<b>WD200 Condition</b>") {
            label title: "Condition Name", required: true
        }
        section("<b>LED Indicator</b>") {
            input name: "index", type: "number", title: "Index (1-7:bottom to top LED)", range: "1..7", required: true
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
    //BUG: Status not updating correctly after removing a condition
    initialize()
}

private initialize() {
    logDebug "Initialize with settings:${settings}, state:${state}"

    atomicState.active = sensorType == "on"
    switch (sensorType) {
        case [null, "on", "off"]:
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
    if (!leds[index] || (leds[index].priority < priority)) leds[index as String] = [color: color, priority: priority]
}

def checkVersion(version) {
    def pass = version == state.version
    if (!pass) log.error "Parent/Child version mismatch: parent v${version} != child v${state.version}"
    return pass
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
            return ["unlocked", "locked"] //TODO: Add "unknown" and other states... Solve UI issues...
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
