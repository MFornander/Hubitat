/**
 * ****************  WD200 Status Condition ********************
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
 * TODO: Documentation
 * 
 * Versions:
 *  1.0.0 - 2020-05-xx - Initial release.
 */

private setVersion() {
    state.name = "WD200 Status Condition"
    state.version = "1.0.0"
}

definition(
    name: "WD200 Status Condition",
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
        section("<b>WD200 Status Condition</b>") {
            label title: "Label", required: true
        }
        section("<b>LED Indicator</b>") {
            input name: "index", type: "number", title: "Index (1-7:bottom to top LED)", range: "1..7", required: true
            input name: "color", type: "enum", title: "Color", required: true,
                options: [
                    1: "Red",
                    5: "Yellow",
                    2: "Green",
                    6: "Cyan",
                    3: "Blue",
                    4: "Magenta",
                    7: "White",
                    0: "Off"
                ]
            input name: "priority", type: "number", title: "Priority (higher overrides lower conditions)", defaultValue: "0"
        }
        section("<b>Input</b>") {
            input name: "cap", type: "enum", title: "Sensor Type", required: true, submitOnChange: true,
                options: [
                    "on": "Always On",
                    "off": "Disabled",
                    "switch": "Switch",
                    "contactSensor": "Contact"
                ]
            if (cap != null && cap != "on" && cap != "off") {
                input name: "sensorList", type: "capability.${cap}", title: "Sensors", required: true, multiple: true
                input name: "sensorState", type: "enum", title: "State", required: true, multiple: false,
                    options: attributeValues(cap)
            }
        }
        section("Instructions", hideable: true, hidden: true) {
            paragraph "TODO"
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
    def condition = getCondition()
    logDebug "Initialize with settings:${settings}, condition:${condition}, state:${state}"
    parent.refreshConditions(condition)
}

private isActive() {
}

def getCondition() {
    if (cap == "on") {[
        index: index as int,
        color: color as int,
        priority: priority as int
    ]}
}

private logDebug(msg) {
    if (parent.debugEnable) { log.debug msg }
}

private attributeValues(attributeName) {
    switch (attributeName) {
        case "switch":
            return ["on","off"]
        case "contactSensor":
            return ["open","closed"]
        case "motionSensor":
            return ["active","inactive"]
        case "waterSensor":
            return ["wet","dry"]
        case "lock":
            return ["unlocked (or unknown)", "locked"]
        default:
            return ["UNDEFINED"]
    }
}
