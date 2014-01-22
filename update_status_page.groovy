import groovy.json.JsonSlurper
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

def projectRoot 
def contentDir
def templateLocation
def templateOutputLocation
def contentDestination 
def nagiosUrl
def nagiosCredentials
def repeatTime

def parseArgs = {

    if (args.length < 3) {
        println "Expected args: projectRootPath contentDestinationPath nagiosUrl [repeatTimeInMinutes]"
        println "args were: $args"
        System.exit 1
    }

    projectRoot = args[0]        // "F:\\Projects\\AODNstatus"
    contentDestination = args[1] // "F:\\Programs\\Apache Software Foundation\\Tomcat 7\\webapps\\status\\"
    nagiosUrl = args[2]          // "https://nagios.aodn.org.au/nagios3"
    nagiosCredentials = args[3]  // <username>:<password> Base64 encoded

    if (args.length > 4)
        repeatTime = Integer.parseInt(args[4])
}

def loadVariables = {
    contentDir = "$projectRoot\\content\\"
    templateLocation = "$projectRoot\\template.html"
    templateOutputLocation = "$contentDir\\index.html"
}

def configureSslToIgnoreCerts = {
    def nullTrustManager = [
        checkClientTrusted: { chain, authType -> },
        checkServerTrusted: { chain, authType -> },
        getAcceptedIssuers: { null }
    ]
    
    def nullHostnameVerifier = [
        verify: { hostname, session -> true }
    ]
    
    SSLContext sc = SSLContext.getInstance("SSL")
    sc.init(null, [nullTrustManager as X509TrustManager] as TrustManager[], null)
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory())
    HttpsURLConnection.setDefaultHostnameVerifier(nullHostnameVerifier as HostnameVerifier)
}

def getStatusInfo = {

    // Gather status info
    def statusInfoSource = "$nagiosUrl/cgi-bin/statusJson.php?host=all" // Requires authentication    

    configureSslToIgnoreCerts()

    println "Connecting to: $statusInfoSource"
    def statusInfoConnection = statusInfoSource.toURL().openConnection()
    statusInfoConnection.setRequestProperty("Authorization", "Basic $nagiosCredentials")
    statusInfoConnection.connect()
    
    def statusInfoText = statusInfoConnection.content.text
    println "Parsing result"
    def statusInfo = new JsonSlurper().parseText(statusInfoText)
   
    return statusInfo
}

def getProcessedStatusInfo = {

    def servicesByHost = [:]

    def allStatusInfo = getStatusInfo()
    def serviceInfo = allStatusInfo.services
    
    serviceInfo.each{
        
        def service = it.value
        service['name'] = it.key
        
        def services = servicesByHost[it.value.host_name] ?: []
        services << service
        
        servicesByHost[it.value.host_name] = services
    }
    
    /*servicesByHost.each{
        println it.key

        it.value.each{
            println " - $it"
        }
    }*/
    
    new File(contentDir + "//intermediateOutput.txt").write(servicesByHost.toString())
    
    """
    [
        {
            name: 'Data Access Services',
            status: 'ok',
            services: [
                {
                    name: 'AODN Ocean Portal',
                    description: 'Access to public data made available by the Australian Ocead Data Network (AODN) contributing organisations.',
                    okMessage: 'No problems'
                },
                {
                    name: 'IMOS Ocean Portal',
                    description: 'Access to all data collected by Integrated Marine Observing System (IMOS).',
                    okMessage: 'No problems'
                },
                {
                    name: 'Metadata Catalogue',
                    description: 'Metadata catalogue of data collections made available through the IMOS Ocean Portal.',
                    okMessage: 'No porblems',
                    notice: 'Upcoming maintenance: December 12th, 2014'
                },
                {
                    name: 'OceanCurrent',
                    description: 'Providing ocean surface currents and temperature information.',
                    okMessage: 'No problems'
                },
                {
                    name: 'Portal Help Forums',
                    description: 'Help guides and support forum for AODN and IMOS Portals, and IMOS data.',
                    okMessage: 'No problems'
                }
            ]
        },
        {
            name: 'Supporting Services',
            status: 'error',
            services: [
                {
                    name: 'Geoserver - Map Layers',
                    description: 'Responsible for producing map layers for the AODN Ocean Portal.',
                    okMessage: 'No problems'
                },
                {
                    name: 'GeoServer - Feature Service',
                    description: 'Processes requests for subsetted point data collections.',
                    warningMessage: 'Planned Outage'
                },
                {
                    name: 'AODAAC Aggregator',
                    description: 'Processess subsetting and aggregation of gridded data collections.',
                    errorMessage: 'Unplanned outage'
                },
                {
                    name: 'Database server',
                    description: 'Supports multiple services.',
                    okMessage: 'No problems'
                }
            ]
        }
    ]
    """
}

def getProcessedTemplate = {

    def template = new File(templateLocation).text
    def templateOutput = template
    
    templateOutput = templateOutput.replace("##LAST_UPDATED_MILLIS##", "${System.currentTimeMillis()}")
    templateOutput = templateOutput.replace("##SERVICE_STATUS_JSON##", getProcessedStatusInfo())
    
    def generatedFileHeader = "<!-- #### This file is generated by a script. Any changes you make will be overwritten. #### -->\n"
    
    return generatedFileHeader + templateOutput
}

def generateStatusFile = {
    
    new File(templateOutputLocation).write(getProcessedTemplate())
}

def copyAllFiles = {

    println "Copying files"
    println "From: $contentDir"
    println "To: $contentDestination"

    new AntBuilder().copy(todir: contentDestination) {
      fileset(dir: contentDir)
    }

    println "Files copied"
}

def doOnce(closure) {

    println "Executing once"

    closure()
}

def repeat(closure, repeatTime) {

    println "Executing every $repeatTime minutes"

    while(true) {

        try {
            closure()
        }
        catch (Exception e) {
            println "Execution failed"
            e.printStackTrace()
        }

        Thread.sleep 1000 * 60 * repeatTime
    }
}

def theProcessing = {
    generateStatusFile()
    copyAllFiles()
}

parseArgs()
loadVariables()

if (repeatTime) {
    repeat theProcessing, repeatTime
}
else {
    doOnce theProcessing
}