/**
 * ****************  WD200 Dashboard ********************
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
 * Hubitat parent app to be installed along with the "WD200 Condition" child app.
 *
 * Versions:
 * 1.0.0 - 2020-05-xx - Initial release.
 */

def getVersion() {
    "0.0.7"
}

definition(
    name: "WD200 Dashboard",
    namespace: "MFornander",
    author: "Mattias Fornander",
    description: "Turn your HS-WD200 Dimmer into a mini-dashboard",
    importUrl: "https://raw.githubusercontent.com/MFornander/Hubitat/master/apps/wd200dashboard/wd200dashboard-parent.groovy",
    iconUrl: "",
    iconX2Url: ""
)

def getDescription() {
"""<p>This parent-child app pair allows easy linking of Hubitat sensor states to
LEDs of your HS-WD200 dimmers.  You can link states such as contact sensors
open/closed, motion sensors active/inactive, locks locked/unlocked, and more,
to LEDs of various colors on your dimmers.  Several sensors can "share" an
LED such that the same LED can show yellow if a door is unlocked and red if
it's open.

<p>Each set of dimmers can have one or more "Conditions" that link
sensor states to a specific LED and a specific color.  Conditions also have
explicit priorities that determine which condition gets to set an LED if
there is a conflict.  This allows the lock+door example above to always
show door open means red, and only show yellow for unlocked if the door is
closed.

<p>One Dashboard app can control more than one Dimmer such that several
WD200 dimmers can show the same status.  However you can also install many
Dashboard apps if you want two dimmers to show different states.

<p>The current version supports a variety of sensors but there are many
missing.  Make a bugreport or feature request on GitHub and I'll try to
add it.  However, note that you can use a Virtual Switch in an automation
such as RuleMachine with any level of complexity and add link a Condition
to it.

<p>The use case and inspiration for this app is my house with nine major
doors and several ground level windows to the outside.  I wanted to know
at glance if the doors were closed and locked.  The first version was a
RuleMachine instance but it was not pretty but more importantly, I wanted
to learn more Hubitat and Groovy.

<p><b>Note that this is my first Hubitat App and first time using Groovy
so don't trust it with anything important.</b>"""
}

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
    childApps.each { logDebug "Condition: ${it.label}" }
    dimmers.each { } // TODO: Verify they are HS-WD200 Dimmers
}

def mainPage() {
    dynamicPage(name: "mainPage") {
        installCheck()
        if (state.appInstalled == 'COMPLETE') {
            section() {
                paragraph '<h2>WD200 Dashboard</h2>"Turn your HS-WD200 Dimmer into a mini-dashboard"'
                label title: "App Name (optional)", required: false
                // FIX: Allow only selection of HS-WD200 Dimmers
                input "dimmers", "capability.switchLevel", title: "Status Dimmers", required: true, multiple: true
                app name: "anyOpenApp", appName: "WD200 Condition", namespace: "MFornander", title: "Add LED status condition", multiple: true
                input name: "debugEnable", type: "bool", defaultValue: "true", title: "Enable Debug Logging"
            }
            section("Instructions", hideable: true, hidden: true) {
                paragraph "<b>WD200 Dashboard v${getVersion()}</b>"
                paragraph getDescription().replaceAll("[\r\n]+"," ")
            }
        }
    }
}

private installCheck() {
    state.appInstalled = app.getInstallationState()
    if (state.appInstalled != 'COMPLETE') {
        section {
            paragraph "Please hit 'Done' to install '${app.label}'"
        }
  	}
}

def refreshConditions() {
    def children = getChildApps()
    def fail = children.find { it.getVersion() != getVersion() }
    if (fail) {
        log.error "Version mismatch: parent v${getVersion()} != child v${fail.getVersion()}"
        return
    }

    logDebug "Refreshing ${children.size()} conditions..."
    def leds = [:]
    children*.addCondition(leds)

    logDebug "Setting LEDs to ${leds}"
    (1..7).each { if (leds[it]) dimmers.setStatusLED(it as String, leds[it].color) }
    (1..7).each { if (!leds[it]) dimmers.setStatusLED(it as String, "0") }
}

private logDebug(msg) {
    if (debugEnable) log.debug msg
}
