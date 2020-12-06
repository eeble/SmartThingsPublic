definition(
    name: "Fast Auto Lock Door",
    namespace: "Lock Auto Fast",
    author: "eeble",
    description: "Automatically locks a specific door after X seconds when closed  and unlocks it when open after X seconds.",
    category: "Safety & Security",
    iconUrl: "http://www.gharexpert.com/mid/4142010105208.jpg",
    iconX2Url: "http://www.gharexpert.com/mid/4142010105208.jpg",
    pausable: true
)

preferences{
    page name: "mainPage", install: true, uninstall: true
}

def mainPage() {
    dynamicPage(name: "mainPage") {
        section("Select the door lock:") {
            input "lock1", "capability.lock", required: true
        }
        section("Select the door contact sensor:") {
            input "contact", "capability.contactSensor", required: true
        }
        section("Automatically lock the door when closed...") {
            input "secondsLock", "number", title: "Delay (in seconds):", required: true
        }
        section("Automatically unlock the door when open...") {
            input "secondsUnlock", "number", title: "Delay (in seconds):", required: true
        }
        if (location.contactBookEnabled || phoneNumber) {
            section("Notifications") {
                input("recipients", "contact", title: "Send notifications to", required: false) {
                    input "phoneNumber", "phone", title: "Warn with text message (optional)", description: "Phone Number", required: false
                }
            }
        }
        section([mobileOnly:true]) {
            label title: "Assign a name", required: false
            mode title: "Set for specific mode(s)"
        }
    }
}

def installed(){
    initialize()
}

def updated(){
    unsubscribe()
    unschedule()
    initialize()
}

def initialize(){
    log.debug "Settings: ${settings}"
    subscribe(lock1, "lock", doorHandler, [filterEvents: false])
    subscribe(lock1, "unlock", doorHandler, [filterEvents: false])  
    subscribe(contact, "contact.open", doorHandler)
    subscribe(contact, "contact.closed", doorHandler)
}

def lockDoor(){
    log.debug "Locking the door."
    lock1.lock()
    if(location.contactBookEnabled) {
        if ( recipients ) {
            log.debug ( "Sending Push Notification..." ) 
            sendNotificationToContacts( "${lock1} locked after ${contact} was closed for ${secondsLock} minutes!", recipients)
        }
    }
    if (phoneNumber) {
        log.debug("Sending text message...")
        sendSms( phoneNumber, "${lock1} locked after ${contact} was closed for ${secondsLock} minutes!")
    }
}

def unlockDoor(){
    log.debug "Unlocking the door."
    lock1.unlock()
    if(location.contactBookEnabled) {
        if ( recipients ) {
            log.debug ( "Sending Push Notification..." ) 
            sendNotificationToContacts( "${lock1} unlocked after ${contact} was opened for ${secondsUnlock} seconds!", recipients)
        }
    }
    if ( phoneNumber ) {
        log.debug("Sending text message...")
        sendSms( phoneNumber, "${lock1} unlocked after ${contact} was opened for ${secondsUnlock} seconds!")
    }
}

def doorHandler(evt){
    if ((contact.latestValue("contact") == "open") && (evt.value == "locked")) { // If the door is open and a person locks the door then...  
        //def delay = (secondsUnlock) // runIn uses seconds
        runIn( secondsUnlock, unlockDoor )   // ...schedule (in minutes) to unlock...  We don't want the door to be closed while the lock is engaged. 
    }
    else if ((contact.latestValue("contact") == "open") && (evt.value == "unlocked")) { // If the door is open and a person unlocks it then...
        unschedule( unlockDoor ) // ...we don't need to unlock it later.
    }
    else if ((contact.latestValue("contact") == "closed") && (evt.value == "locked")) { // If the door is closed and a person manually locks it then...
        unschedule( lockDoor ) // ...we don't need to lock it later.
    }   
    else if ((contact.latestValue("contact") == "closed") && (evt.value == "unlocked")) { // If the door is closed and a person unlocks it then...
       //def delay = (secondsLock) // runIn uses seconds
        runIn( (secondsLock), lockDoor ) // ...schedule (in minutes) to lock.
    }
    else if ((lock1.latestValue("lock") == "unlocked") && (evt.value == "open")) { // If a person opens an unlocked door...
        unschedule( lockDoor ) // ...we don't need to lock it later.
    }
    else if ((lock1.latestValue("lock") == "unlocked") && (evt.value == "closed")) { // If a person closes an unlocked door...
        //def delay = (secondsLock) // runIn uses seconds
        runIn( (secondsLock), lockDoor ) // ...schedule (in minutes) to lock.
    }
    else { //Opening or Closing door when locked (in case you have a handle lock)
        log.debug "Unlocking the door."
        lock1.unlock()
        if(location.contactBookEnabled) {
            if ( recipients ) {
                log.debug ( "Sending Push Notification..." ) 
                sendNotificationToContacts( "${lock1} unlocked after ${contact} was opened or closed when ${lock1} was locked!", recipients)
            }
        }
        if ( phoneNumber ) {
            log.debug("Sending text message...")
            sendSms( phoneNumber, "${lock1} unlocked after ${contact} was opened or closed when ${lock1} was locked!")
        }
    }
}