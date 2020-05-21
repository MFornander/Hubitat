/**
 * ************************** Auto Off **************************
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
 * Versions:
 * 1.0.0 (2020-05-21) - Initial release
 */

def getVersion() {
    "1.0.0"
}

// Set app Metadata for the Hub
definition(
    name: "Auto Off",
    namespace: "MFornander",
    author: "Mattias Fornander",
    description: "Automatically turn off devices after set amount of time on",
    importUrl: "https://raw.githubusercontent.com/MFornander/Hubitat/master/apps/AutoOff/AutoOff.groovy",
    iconUrl: "",
    iconX2Url: "",
    singleInstance: false
)


/// Defer to mainPage() function to declare the preference UI
preferences {
    page name: "mainPage", title: "Auto Off", install: true, uninstall: true
}

/**
 * Called after app is initially installed.
 */
def installed() {
    initialize()
}

/**
 * Called after any of the configuration settings are changed.
 */
def updated() {
    unsubscribe()
    unschedule()
    initialize()
}

/**
 * Internal helper function with shared code for installed() and updated().
 */
private initialize() {
    logDebug "Initialize with settings: ${settings}"
    state.offList = [:]

    subscribe(devices, "switch", switchHandler)
    runEvery1Minute(scheduleHandler)
}

/**
 * Main configuration function declares the UI shown.
 */
def mainPage() {
    checkNewVersion()
    dynamicPage(name: "mainPage") {
        section() {
            paragraph '<i>Automatically turn off devices after set amount of time on.</i>'
            label title: "Name", required: false
            input name: "autoTime", type: "number", title: "Time until auto-off (minutes)", required: true
            input name: "devices", type: "capability.switch", title: "Devices", required: true, multiple: true
            input name: "debugEnable", type: "bool", defaultValue: "true", title: "Enable Debug Logging"
            paragraph state.versionMessage
        }
    }
}

/**
 * Handler called when any of our devices turn on.
 *
 * We use the device id of the switch turning on as key since the evt.device
 * object seems to be a proxy object that changes with each callback.  The first
 * implementation used the evt.device as key but that would create multiple
 * entries in the map for the same switch.  Using the device id instead ensures
 * that a user that turn on and off and on the same switch, will only have one
 * entry since the id stays the same and new off times replace old off times.
 */
def switchHandler(evt) {
    if (evt.value == "on") {
        state.offList[evt.device.id] = now() + autoTime * 60 * 1000
    } else {
        state.offList.remove(evt.device.id)
    }

    logDebug "switchHandler evt.device:${evt.device}, evt.value:${evt.value}, state:${state} now:${now()}"
}

/**
 * Handler called every minute to see if any devices should be turned off.
 *
 * THe first pass used an optimized schedule that looked for the next switch to
 * turn off and would schedule a callback for exactly that time and then
 * reschedule the next off item, if any.  However, it seemed error-prone and
 * cumbersome since errors can happen that may interrupt the rescheduling.
 * Calling a tiny function with a quick check seemed ok to do every minute
 * so that's v1.0 for now.
 */
def scheduleHandler() {
    // Find all map entries with an off-time that is earlier than now
    def actionList = state.offList.findAll { it.value < now() }

    // Find all devices that match the off-entries from above
    def deviceList = devices.findAll { device -> actionList.any { it.key == device.id } }

    logDebug "scheduleHandler now:${now()} offList:${state.offList} actionList:${actionList} deviceList:${deviceList}"

    // Call off() on all the relevant devices and remove them from the offList
    deviceList*.off()
    state.offList -= actionList
}

/**
 * Internal SemVer comparator function, with fancy spaceships.
 *
 * Return 1 if the given version is newer than current, 0 if the same, or -1 if older,
 * according to http://semver.org
 */
private compareTo(version) {
    def newVersion = version.tokenize(".")*.toInteger()
    def runningVersion = getVersion().tokenize(".")*.toInteger()
    logDebug "Version new:${newVersion} running:${runningVersion}"
    if (newVersion.size != 3) throw new RuntimeException("Illegal version:${version}")

    if (newVersion[0] == runningVersion[0]) {
        if (newVersion[1] == runningVersion[1]) {
            newVersion[2] <=> runningVersion[2]
        } else {
            newVersion[1] <=> runningVersion[1]
        }
    } else {
        newVersion[0] <=> runningVersion[0]
    }
}

/**
 * Internal version check function.
 *
 * Download a version file and set state.versionMessage if there is a newer
 * version available.  This message is displayed in the settings UI.
 * TODO: Only do this once a day?
 */
private checkNewVersion() {
    def params = [
        uri: "https://raw.githubusercontent.com",
        path: "MFornander/Hubitat/master/apps/AutoOff/packageManifest.json",
        contentType: "application/json",
        timeout: 3
    ]
    try {
        httpGet(params) { response ->
            logDebug "checkNewVersion response data: ${response.data}"
            switch (compareTo(response.data?.version)) {
                case 1:
                    state.versionMessage = "(New app v${response.data?.version} available, running is v${getVersion()})"
                    break
                case 0:
                    state.remove("versionMessage")
                    break
                default:
                    throw new RuntimeException("GitHub v${response.data?.version} is older than running v${getVersion()}")
                    break
            }
        }
    } catch (e) {
        log.error "checkNewVersion error: ${e}"
        state.remove("versionMessage")
    }
}

/**
 * Internal helper debug logging function
 */
private logDebug(msg) {
    if (debugEnable) log.debug msg
}
