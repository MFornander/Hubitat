/**
 *  ****************  WD200 Status ********************
 *
 *  Copyright 2020 Mattias Fornander (@mfornander)
 *
 *  TODO: Documentation
 * 
 *  Versions:
 *  1.0.0 - 2020-05-xx - Initial release.
 */

def setVersion(){
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
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
    log.info "There are ${childApps.size()} child apps:"
    childApps.each {child ->
        log.info "Child app: ${child.label}"
    }
}

def mainPage() {
    dynamicPage(name: "mainPage") {
    	installCheck()
		if (state.appInstalled == 'COMPLETE') {
            section() {
                paragraph "<h2>${state.name} v${state.version}<h2>"
                label title: "App Name (optional)", required: false
                input "dimmers", "capability.switchLevel", title: "Target Dimmers", required: true, multiple: true
				app name: "anyOpenApp", appName: "WD200 Status Child", namespace: "MFornander", title: "Add LED status condition", multiple: true
                input "debugEnabled", "bool", title: "Enable debug log"
            }
            section("Instructions", hideable: true, hidden: true) {
				paragraph "TODO"
			}
		}
	}
}

def installCheck() {
    setVersion()
	state.appInstalled = app.getInstallationState() 
	if (state.appInstalled != 'COMPLETE') {
		section {
            paragraph "Please hit 'Done' to install '${app.label}'"
        }
  	}
  	else {
        log.info "${app.label} Installed OK"
  	}
}
