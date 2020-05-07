/**
 * ****************  Dimmer Dashboard ********************
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
 * Description: "Turn your LED status dimmers into mini-dashboards"
 * Hubitat parent app to be installed along with the "Dimmer Dashboard Condition" child app.
 *
 * Versions:
 * 1.0.0 - 2020-05-xx - Initial release.
 */

/// Expose parent app version to allow version mismatch checks between child and parent
def getVersion() {
    "0.0.13"
}

// Set app Metadata for the Hub
definition(
    name: "Dimmer Dashboard",
    namespace: "MFornander",
    author: "Mattias Fornander",
    description: "Turn your LED status dimmers into mini-dashboards",
    importUrl: "https://raw.githubusercontent.com/MFornander/Hubitat/master/apps/DimmerDashboard/DimmerDashboard-parent.groovy",
    iconUrl: "",
    iconX2Url: "",
    singleInstance: false
)

/**
 * Internal helper function providing the text displayed when the
 * user opens the 'Instructions' section.  Broken out to a separate
 * function to bring this text towards the top of the source and to
 * keep the mainPage preferences section clean and readable.
 */
private getDescription() {
"""<p>This parent-child app pair allows easy linking of Hubitat sensor
states to LEDs of your HomeSeer HS-WD200 dimmers.  You can link states
such as contact sensors open/closed, motion sensors active/inactive,
locks locked/unlocked, and more, to LEDs of various colors on your
dimmers.  Several sensors can "share" an LED such that the same LED
can show yellow if a door is unlocked and red if it's open.

<p>Each set of dimmers can have one or more "Conditions" that link
sensor states to a specific LED and a specific color.  Conditions also
have explicit priorities that determine which condition gets to set an
LED if there is a conflict.  This allows the lock+door example above
to always show door open means red, and only show yellow for unlocked
if the door is closed.

<p>One Dashboard app can control more than one Dimmer such that several
WD200 dimmers can show the same status.  However you can also install
many Dashboard apps if you want two dimmers to show different states.

<p>The current version supports a variety of sensors but there are many
missing.  Make a bugreport or feature request on GitHub and I'll try to
add it.  However, note that you can use a Virtual Switch in an
automation such as RuleMachine with any level of complexity and add
link a Condition to it.

<p>The use case and inspiration for this app is my house with nine major
doors and several ground level windows to the outside.  I wanted to know
at glance if the doors were closed and locked.  The first version was a
RuleMachine instance but it was not pretty but more importantly, I wanted
to learn more Hubitat and Groovy.

<p><b>Note that this is my first Hubitat App and first time using Groovy
so don't trust it with anything important.</b>"""
}

/// Defer to mainPage() function to declare the preference UI
preferences {
    page name: "mainPage", title: "Dimmer Dashboard", install: true, uninstall: true
}

/**
 * Called after app is initially installed.
 */
def installed() {
    logDebug "Installed with settings: ${settings}"
    initialize()
}

/**
 * Called after any of the configuration settings are changed.
 */
def updated() {
    logDebug "Updated with settings: ${settings}"
    initialize()
}

/**
 * Shared helper function used by installed and updated functions.
 *
 * For now it just logs various states and checks if the dimmers selected
 * are actually HS-WD200+ dimmers.  It is possible to show lists of devices
 * based on either capbility or device name.  Ideally the UI would only
 * show a list of usable dimmers but until I figure out how to create
 * a usable input filter, I just log the illegal dimmers here,
 */
private initialize() {
    logDebug "There are ${childApps.size()} conditions: ${childApps.label}"
    // TODO: Find way to filter out only WD200 Dimmers (https://community.hubitat.com/t/device-specific-inputs/36734/7)
    dimmers.findAll { !it.hasCommand("setStatusLED") }.each { log.error "${it.label} is not a HS-WD200 Dimmer" }
    doRefreshDashboard()
}

/**
 * Main configuration function declares the UI shown.
 *
 * A simple UI that just requests a name of the app and allows the user to
 * add one or more conditions that will control the LEDs of the dimmers.
 *
 * Each Dashboard app instance allows the creation of a dashboard that
 * can be displayed on one or more dimmers.  You can add additional
 * instance of this app if you want different dashboards on different
 * dimmerd.
 *
 * The use of child apps may seem overly complicated at first and I went
 * through quite a few UI iterations before ending up here.  I really
 * wanted a simple single-source app but the dynamic nature of having
 * one or more dimmers sharing the same dashboard, and one or more conditions
 * per dashboard, with seven LEDs per dimmer and overloaded conditions
 * per LED... caused me to end up here.
 *
 * Debug logging is set per parent app instance and caues all its child
 * apps, i.e. conditions to also enable debg logging.  Instructions are
 * hidden at the bottom in a closed section since most people will only
 * read it once and then it should be out of the way.
 */
def mainPage() {
    dynamicPage(name: "mainPage") {
        section() {
            paragraph '"Turn your LED status dimmers into mini-dashboards"'
            label title: "App Name (optional)", required: false
            // TODO: Allow only selection of WD200 Dimmers (https://community.hubitat.com/t/device-specific-inputs/36734/7)
            input "dimmers", "capability.switchLevel", title: "HomeSeer WD200+ Dimmers", required: true, multiple: true, submitOnChange: true
            app name: "anyOpenApp", appName: "Dimmer Dashboard Condition", namespace: "MFornander", title: "Add LED status condition", multiple: true
            input name: "debugEnable", type: "bool", defaultValue: "true", title: "Enable Debug Logging"
        }
        section("Instructions", hideable: true, hidden: true) {
            paragraph "<b>Dimmer Dashboard v${getVersion()}</b>"
            // Strip newlines to allow the description text to flow naturally
            paragraph getDescription().replaceAll("[\r\n]+"," ")
        }
    }
}

/**
 * Trigger a refresh of the dashboard, called by child apps.
 *
 * Called by child apps when their state or settings have changed.  The use
 * of runIn() is a workaround since this function is called in a child's
 * installed() and uninstalled() function but the parent's list of apps
 * is not updated until *after* those methods return.  To allow immediate
 * updates of new or removed conditons, I ask the parent to refresh its
 * dashboard in zero seconds which still happens after the installed or
 * uninstalled return.  A little messy but I really wanted the user to
 * see their settings and conditions on the dashboard right away.
 */
def refreshDashboard() {
    runIn(0, doRefreshDashboard)
}

/**
 * Main dashboard logic that reads all conditions and sets LEDs.
 *
 * I lost some time during development with mismatched child and parent code
 * so each refresh does a version match of parent and child.
 * Each condition gets a chance to add its color and index slot and stores
 * its priority along with that data.  Conditions only replace colors if
 * there is no previous color at that slot or if their priority is higher
 * than the current priority stored at the slot.
 *
 * In the end, all conditions have had their say and the leds map now
 * contains the intended color for each LED slot.  We first turn on the LEDs
 * that should be on and then after that, turn off the ones that should be
 * off.  This is to prevent a condition where the dimmer would temporarily
 * enter state where all LEDs are off and flash the current dimmer level
 * before setting LEDs again.
 *
 * TODO: Optimize by storing the led state between calls and only call
 * setStatusLeED if different from last time.  Not done since most of the
 * time, this function is called becuase something did change but that
 * could be a low priority condition that untimately did not change the
 * dashboard output.
 */
def doRefreshDashboard() {
    def children = getChildApps()
    def fail = children.find { it.getVersion() != getVersion() }
    if (fail)
        log.error "Version mismatch: parent v${getVersion()} != child v${fail.getVersion()}"

    logDebug "Refreshing ${children.size()} conditions..."
    def leds = [:]
    children*.addCondition(leds)

    logDebug "Setting LEDs to ${leds}"
    (1..7).each { if (leds[it]) dimmers.setStatusLED(it as String, leds[it].color) }
    (1..7).each { if (!leds[it]) dimmers.setStatusLED(it as String, "0") }
}

/**
 * Internal helper debug logging function
 */
private logDebug(msg) {
    if (debugEnable) log.debug msg
}
