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
 * 1.0.0 (2020-05-xx) - Initial release
 */

def getVersion() {
    "1.0.0"
}

// Set app Metadata for the Hub
definition(
    name: "Auto Off",
    namespace: "MFornander",
    author: "Mattias Fornander",
    description: "Automatically turn off device after set amount of time on",
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
    initialize()
}

/**
 * Internal helper function with shared code for installed() and updated().
 */
private initialize() {
    logDebug "Initialize with settings: ${settings}"
    state.offList = [:]
    subscribe(devices, "switch.on", switchHandler)
}

/**
 * Main configuration function declares the UI shown.
 */
def mainPage() {
    checkNewVersion()
    dynamicPage(name: "mainPage") {
        section() {
            paragraph '<i>Automatically turn off device after set amount of time on.</i>'
            label title: "Name", required: false
            input name: "autoTime", type: "number", title: "Time until auto-off (seconds)", required: true
            input name: "devices", type: "capability.switch", title: "Devices", required: true, multiple: true
            input name: "debugEnable", type: "bool", defaultValue: "true", title: "Enable Debug Logging"
            paragraph state.versionMessage
        }
    }
}

/**
 * TODO
 */
def switchHandler(evt) {
    if (evt.value == "on") state.offList[evt.device.id] = (now() / 1000 + autoTime) as long

    logDebug "switchHandler evt.device:${evt.device}, evt.value:${evt.value}, state:${state} now:${(now() / 1000) as long}"
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
