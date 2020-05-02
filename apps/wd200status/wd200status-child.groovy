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

def setVersion(){
    state.name = "WD200 Status Condition"
	state.version = "1.0.0"
}

definition(
    name: "WD200 Status Condition",
    namespace: "MFornander",
    author: "Mattias Fornander",
    description: "TODO",
	parent: "MFornander:WD200 Status",
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
            label title: "Label (optional)", required: false
        }
		section("<b>LED Indicator</b>") {
            input name: "index", type: "number", title: "Index (0=all, 1=top, 7=bottom)", range: "0..7", required: true
			input name: "color", type: "enum", title: "Color", required: true,
                options: ["Red", "Yellow", "Green", "Cyan", "Blue", "Magenta", "White"]
            		}
        section("<b>Input</b>") {
            input name: "cap", type: "enum", title: "Sensor Type", required: true, default: "off", submitOnChange: true,
                options: [
                    "on": "Always On",
                    "off": "Disabled",
                    "contactSensor": "Contact"
                ] // "Lock", "Motion", "Alarm", ...
            if (cap != null && cap != "on" && cap != "off") {
                input name: "sensorList", type: "capability.${cap}", title: "Sensors", required: true, multiple: true
                input name: "sensorState", type: "enum", title: "State", required: true, multiple: false,
                    options: attributeValues(cap)
            }
        }
        section() {
            input name: "debugEnable", type: "bool", defaultValue: "true", title: "Enable Debug Logging"
        }
        section("Instructions", hideable: true, hidden: true) {
    		paragraph "For use with any valve device connected to your hose, like the Orbit Hose Water Timer. Features multiple timers and restrictions."
        }
	}
}

def installed() {
    logDebug "Installed with settings: ${settings}"
	initialize()
}

def updated() {	
    logDebug "Updated with settings: ${settings}"
	unschedule()
	initialize()
}

def initialize() {
    setVersion()
    setDefaults()
}

def logDebug(msg) {
    if (debugEnable) { log.debug msg }
}

def setDefaults() {
	if (debugEnable == null) { logEnable = false }
	if (state.msg == null) { state.msg = "" }
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
