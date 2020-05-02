/**
 * ****************  WD200 Status ********************
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

private def setVersion(){
    state.name = "WD200 Status"
    state.version = "1.0.0"
}

definition(
    name: "WD200 Status",
    namespace: "MFornander",
    author: "Mattias Fornander",
    description: "Display sensor states on HomeSeer WD-200 LEDs",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
    page name: "mainPage", title: "", install: true, uninstall: true
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

private def initialize() {
    logDebug "There are ${childApps.size()} child apps..."
    childApps.each {child ->
        logDebug "Child app: ${child.label}"
    }
}

def mainPage() {
    dynamicPage(name: "mainPage") {
        installCheck()
        if (state.appInstalled == 'COMPLETE') {
            section() {
                paragraph "<h3>${state.name}</h3>"
                label title: "App Name (optional)", required: false
                input "dimmers", "capability.switchLevel", title: "Target Dimmers", required: true, multiple: true
                app name: "anyOpenApp", appName: "WD200 Status Condition", namespace: "MFornander", title: "Add LED status condition", multiple: true
                input name: "debugEnable", type: "bool", defaultValue: "true", title: "Enable Debug Logging"
            }
            section("Instructions", hideable: true, hidden: true) {
                paragraph "<h2>${state.name} v${state.version}<h2>"
            }
        }
    }
}

private def installCheck() {
    setVersion()
    state.appInstalled = app.getInstallationState() 
    if (state.appInstalled != 'COMPLETE') {
        section {
            paragraph "Please hit 'Done' to install '${app.label}'"
        }
  	}
  	else {
        //logDebug "${app.label} Installed OK"
  	}
}

def refreshConditions() {
    def children = getChildApps()
    logDebug "Refreshing ${children.size()} conditions..."
    def leds = [1: null, 2: null, 3: null, 4: null, 5: null, 6: null, 7:null]
    children.each { child ->
        def condition = child.getCondition()
        logDebug "Condition ${child.label}: ${condition}"
        if (condition != null) {
            if (leds[condition.index] == null || leds[condition.index].priority < condition.priority) {
                leds[condition.index] = condition
            }
        }
    }

    logDebug "Setting LEDs to ${leds}"
    leds.each { led ->
        logDebug "Set LED ${led}"
        dimmers.setStatusLED(led.key as String, led.value == null ? "0" : led.value.color as String)
    }
}

private def logDebug(msg) {
    if (debugEnable) { log.debug msg }
}
