/**
 * ****************  WD200 Status ********************
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

private setVersion(){
    state.name = "WD200 Status"
    state.version = "0.0.4"
}

definition(
    name: "WD200 Status",
    namespace: "MFornander",
    author: "Mattias Fornander",
    description: "Display sensor states on HomeSeer WD-200 LEDs",
    importUrl: "https://raw.githubusercontent.com/MFornander/Hubitat/master/apps/wd200status/wd200status-parent.groovy",
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

private initialize() {
    logDebug "There are ${childApps.size()} conditions..."
    childApps.each { child -> logDebug "Condition: ${child.label}" }
}

def mainPage() {
    dynamicPage(name: "mainPage") {
        installCheck()
        if (state.appInstalled == 'COMPLETE') {
            section() {
                paragraph "<h2>${state.name}</h2>TODO"
                label title: "App Name (optional)", required: false
                input "dimmers", "capability.switchLevel", title: "Status Dimmers", required: true, multiple: true
                app name: "anyOpenApp", appName: "WD200 Condition", namespace: "MFornander", title: "Add LED status condition", multiple: true
                input name: "debugEnable", type: "bool", defaultValue: "true", title: "Enable Debug Logging"
            }
            section("Instructions", hideable: true, hidden: true) {
                paragraph "<b>${state.name} v${state.version}</b>"
                paragraph "TODO"
            }
        }
    }
}

private installCheck() {
    setVersion()
    state.appInstalled = app.getInstallationState()
    if (state.appInstalled != 'COMPLETE') {
        section {
            paragraph "Please hit 'Done' to install '${app.label}'"
        }
  	}
}

def refreshConditions() {
    def leds = ["1": null, "2": null, "3": null, "4": null, "5": null, "6": null, "7":null]

    def children = getChildApps()
    if (children.find { !it.checkVersion(state.version) }) return

    logDebug "Refreshing ${children.size()} conditions..."
    children.each { child -> child.addCondition(leds) }

    logDebug "Setting LEDs to ${leds}"
    leds.each { led -> if (led.value) dimmers.setStatusLED(led.key, led.value.color) }
    leds.each { led -> if (!led.value) dimmers.setStatusLED(led.key, "0") }
}

private logDebug(msg) {
    if (debugEnable) log.debug msg
}
