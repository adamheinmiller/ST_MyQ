/**
 *  MyQ Garage Door
 *
 *  Copyright 2014 Adam Heinmiller
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 
 
/*  

Installation:

Create the Device Type:
	1.  Create a new device type, the name can be anything
	2.  Paste this code into the new device and save
	3.  Publish the device type, "For Me"

Add a new Device:
	1.  Create a new device, name it something appropriate
	2.  Give it a unique Device Network ID
	3.  Select "MyQ Garage Door" as the Type (should be at the bottom)
	4.  Make sure "Published" is selected as the Version
	5.  Select the Location, Hub, etc.
	6.  Click Create
	
Setup your Garage Door:
	1.  Get your Username, Password and Door Name used in the MyQ mobile app
	2.  Edit your new device's Preferences and enter the information above
	
If everything worked correctly, the door should retrieve the current status.  If you see "Unknown" there is probably an issue with your username and password; use the logs to capture error information.  If you see "Door not Found" your garage door name is not correct.

*/ 
 
   
import groovy.time.TimeCategory 


preferences 
{
    input("username", "text", title: "Username", description: "MyQ username (email address)")
    input("password", "password", title: "Password", description: "MyQ password")
    input("door_name", "text", title: "Door Name", description: "MyQ Garage Door name")
    
}

metadata 
{
	definition (name: "MyQ Garage Door", author: "Adam Heinmiller") 
    {
		capability "Polling"
        capability "Switch"
        capability "Refresh"
        
        attribute "doorStatus", "string"
        
        command "open"
        command "close"
        command "login"
        command "getDevice"
        command "getDoorStatus"
        command "openDoor"
        command "closeDoor"
	}

	simulator 
    {
		// TODO: define status and reply messages here
	}

	tiles
    {    

		standardTile("sDoorToggle", "device.doorStatus", width: 1, height: 1, canChangeIcon: false) 
        {
			state "unknown", label: 'Unknown', icon: "st.unknown.unknown.unknown", action: "refresh.refresh", backgroundColor: "#afafaf"
            state "door_not_found", label:'Not Found', backgroundColor: "#CC1821"            

			state "stopped", label: 'Stopped', icon: "st.contact.contact.open", action: "close", backgroundColor: "#ffdd00"
			state "closed", label: 'Closed', icon:"st.doors.garage.garage-closed", action: "open", backgroundColor: "#ffffff"
            state "closing", label: 'Closing', icon:"st.doors.garage.garage-closing", backgroundColor: "#ffdd00"
			state "open", label: 'Open', icon:"st.doors.garage.garage-open", action: "close", backgroundColor: "#ffdd00"
            state "opening", label: 'Opening', icon:"st.doors.garage.garage-opening", backgroundColor: "#ffdd00"
        }


        standardTile("sRefresh", "device.switch", inactiveLabel: false, decoration: "flat") 
        {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		standardTile("sLogin", "device.switch", inactiveLabel: false, decoration: "flat") 
        {
			state "default", label:'Login', action:"login"
		}

		standardTile("sGetDeviceInfo", "device.switch", inactiveLabel: false, decoration: "flat") 
        {
			state "default", label:'GetDevices', action:"getDevice"
		}

		standardTile("sGetDoorStatus", "device.switch", inactiveLabel: false, decoration: "flat") 
        {
			state "default", label:'GetStatus', action:"getDoorStatus"
		}

		standardTile("sOpenDoor", "device.switch", inactiveLabel: false, decoration: "flat") 
        {
			state "default", label:'OpenDoor', action:"open"
		}
        
		standardTile("sCloseDoor", "device.switch", inactiveLabel: false, decoration: "flat") 
        {
			state "default", label:'CloseDoor', action:"close"
		}

		
        def debugDetailTiles = [] // + ["sLogin", "sGetDeviceInfo", "sGetDoorStatus", "sOpenDoor", "sCloseDoor"]
        		
        main(["sDoorToggle"])
        details(["sDoorToggle", "sRefresh"] + debugDetailTiles)
    }

}

// parse events into attributes
def parse(String description) 
{}


def installed() {

	log.debug "Installing MyQ Garage Door"

	state.Login = [ BrandID: "Chamberlain", Expiration: 0 ]
    state.DeviceID = 0
}


def updated() {

	log.debug "Updating MyQ Garage Door"
    
	state.Login.Expiration = 0
    state.DeviceID = 0
    
    checkLogin()
}


// handle commands
def poll() 
{
	log.debug "Polling"
    
    refresh()
}

def on()
{
	open()
}

def off()
{
	close()
}


def refresh()
{
	log.debug "Refreshing Door State"
    

	checkLogin()
    
    getDoorStatus() { status ->
    

    	def dDoorStatus = translateDoorStatus(status)
        
    	//sendEvent(name: "doorStatus", value: dDoorStatus, isStateChange: true, display: true)
    	sendEvent(name: "doorStatus", value: dDoorStatus)
    
    	log.debug "Door Status: $dDoorStatus"
    }
}

def open()
{
	log.debug "Opening Door"
	
    
    checkLogin()
    
    
    def dInitStatus
    def dCurrentStatus = "opening"
    
    getDoorStatus() { dInitStatus = translateDoorStatus(it) }
                   
	if (dInitStatus == "opening" || dInitStatus == "open") { return }


	sendEvent(name: "doorStatus", value: "opening", display: true) //, isStateChange: true, display: true)

    
    openDoor()


	while (dCurrentStatus == "opening")
    {
		sleepForDuration(4500) {
        
        	getDoorStatus() { dCurrentStatus = translateDoorStatus(it) }
        }
    }
    
	    
	sendEvent(name: "doorStatus", value: dCurrentStatus)//, isStateChange: true, display: true)
    
}

def close()
{
	log.debug "Closing Door"
    

	checkLogin()
    
        
	def dInitStatus
    def dCurrentStatus = "closing"
    
    getDoorStatus() { dInitStatus = translateDoorStatus(it) }
                   
	if (dInitStatus == "closing" || dInitStatus == "closed") { return }


	sendEvent(name: "doorStatus", value: "closing", display: true)//, isStateChange: true, display: true)


    closeDoor()
    
    
    sleepForDuration(5000)
    
	while (dCurrentStatus == "closing")
    {
		sleepForDuration(4500) {
        
        	getDoorStatus() { dCurrentStatus = translateDoorStatus(it) }
        }
    }

	sendEvent(name: "doorStatus", value: dCurrentStatus)//, isStateChange: true, display: true)
}


def checkLogin()
{
	//log.debug "Checking Login Credentials"

	if (state.Login.Expiration <= new Date().getTime())
    {
    	login()        
    }
    
    
    if (state.DeviceID == 0)
    {    	
    	getDevice()
    }
}


def login()
{
	log.debug "Logging In to Webservice"


	def loginQParams = [
    
		username: settings.username,
        password: settings.password,
        culture: "en"
    ]

    callApiGet("api/user/validatewithculture", [], loginQParams) { response ->
    	
        use(TimeCategory) {

            state.Login = [

                BrandID: response.data.BrandName,
                UserID: response.data.UserId,
                SecToken: response.data.SecurityToken,
                Expiration: (new Date()).getTime() + 3600000
            ]
		}
        
		log.debug "Sec Token: $state.Login.SecToken"
    }
}

def getDevice()
{
	log.debug "Getting MyQ Devices"
    
    def loginQParams = [

		securityToken: state.Login.SecToken
    ]
    
	
    callApiGet("api/userdevicedetails/get", [], loginQParams) { response ->
        
        def garageDevices = response.getData().Devices.findAll{ it.MyQDeviceTypeId == 2 }


		if (garageDevices.isEmpty() == true) {

			log.debug "Door ID:  No Door Found!"
	
            sendEvent(name: "doorStatus", value: "door_not_found", isStateChange: true, display: true)
            return
        }


		garageDevices.each{ pDevice ->
        
        	def doorAttrib = pDevice.Attributes.find{ it.Name == "desc" }
        
        	if (doorAttrib.Value.toLowerCase() == settings.door_name.toLowerCase()) {
            
            	log.debug "Door ID: $pDevice.DeviceId"
                
				state.DeviceID = pDevice.DeviceId                         
            }
            else {
                log.debug "Door ID:  No Door Found!"

                sendEvent(name: "doorStatus", value: "door_not_found", isStateChange: true, display: true)
                return
            }
        }
        
		
    }
    
}


def getDoorStatus(callback)
{
	
    def loginQParams = [

		securityToken: state.Login.SecToken,
        devId: state.DeviceID,
        name: "doorstate"
    ]


	callApiGet("api/deviceattribute/getdeviceattribute", [], loginQParams) { response ->
    
    
    	def doorState = response.data.AttributeValue
        
        callback(doorState)
        
    }
}


def openDoor()
{ 	
    def loginQParams = [
		
        AttributeValue: "1",
        AttributeName: "desireddoorstate"
    ]


	callApiPut("api/deviceattribute/putdeviceattribute", [], loginQParams) { response ->
        
        // if error, do something?
	}
}


def closeDoor()
{ 	
    def loginQParams = [
		
        AttributeValue: "0",
        AttributeName: "desireddoorstate"
    ]


	callApiPut("api/deviceattribute/putdeviceattribute", [], loginQParams) { response ->

        // if error, do something?
	}
}

def translateDoorStatus(status)
{
	def dReturn = "unknown"
              
	if (status == "2") dReturn = "closed"
	else if (status == "1") dReturn = "open"
	else if (status == "4") dReturn = "opening"
	else if (status == "5") dReturn = "closing"
    else if (status == "3") dReturn = "stopped"

	return dReturn
}

def sleepForDuration(duration, callback = {})
{
	// I'm sorry!

	def dTotalSleep = 0
    
    while (dTotalSleep <= duration)
    {    
    	def dStart = new Date().getTime()
        
		try { httpGet("http://australia.gov.au/404") { } } catch (e) { }
        
        dTotalSleep += (new Date().getTime() - dStart)
    }

	callback(dTotalSleep)
}


def callApiPut(apipath, headers = [], queryParams = [], callback = {})
{
	def baseURL = "https://myqexternal.myqdevice.com/"
    
	def finalHeaders = [
    
    	"User-Agent": "${state.Login.BrandID}/1332 (iPhone; iOS 7.1.1; Scale/2.00)"
            
    ] + headers


    def finalQParams = [
    
    	ApplicationId: "NWknvuBd7LoFHfXmKNMBcgajXtZEgKUh4V7WNzMidrpUUluDpVYVZx+xT4PCM5Kx",
        DeviceId: state.DeviceID,
    	securityToken: state.Login.SecToken
        
    ] + queryParams
    
    
    def finalParams = [ 
    
    	uri: baseURL, 
        path: apipath, 
        headers: finalHeaders,
        contentType: "application/json; charset=utf-8",
        body: finalQParams
	]
    
    
	//log.debug finalParams
    
    
    try
    {
    	httpPut(finalParams) { response ->
        
        	if (response.data.ErrorMessage) {
            
            	log.debug "API Error: $response.data"
            }
            
            callback(response)
        }
        
    }
    catch (Error e)
    {
    	state.LastRequestErrored = true
        
     	sendEvent(name: "doorStatus", value: "unknown")
    }
    finally
    {
    }

}


def callApiGet(apipath, headers = [], queryParams = [], callback = {})
{
	def baseURL = "https://myqexternal.myqdevice.com/"
    
    
    def finalHeaders = [
    
    	"User-Agent": "${state.Login.BrandID}/1332 (iPhone; iOS 7.1.1; Scale/2.00)"
            
    ] + headers
    
    
    def finalQParams = [
    
    	appId: "NWknvuBd7LoFHfXmKNMBcgajXtZEgKUh4V7WNzMidrpUUluDpVYVZx+xT4PCM5Kx",
        filterOn: "true"
    
    ] + queryParams
    
    
    def finalParams = [ 
    
    	uri: baseURL, 
        path: apipath, 
        headers: finalHeaders,
        query: finalQParams
	]
    
    
    //log.debug finalParams
    
    
    try
    {
    	httpGet(finalParams) { response ->
        
        	if (response.data.ErrorMessage) {
            
            	log.debug "API Error: $response.data"
            }
            
            callback(response)
        }
    }
    catch (Error e)
    {
    	state.LastRequestErrored = true
        
     	sendEvent(name: "doorStatus", value: "unknown", isStateChange: true, display: true)
    }
    finally
    {
    }
    

}
