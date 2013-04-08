# [Callas pdfaPilot](http://www.callassoftware.com/callas/doku.php/en:products:pdfapilotserver:history) Converter for [Alfresco](http://www.alfresco.com)
The standard document converters that ship with Alfresco uses OpenOffice (or LibreOffice) for conversion of some documents to PDF. When it comes to converting Microsoft Office documents to PDF or PDF/A, with OpenOffice the end result is sometimes not as good as the original. This is especially true for conversion of the newer XML based formats (like .docx, .xlsx, pptx etc). Callas pdfaPilot is a product that does this by using native Microsoft Office for the conversion process and also by using it's own advanced PDF/A technology for creating valid PDF and PDF/A documents.

This extension to Alfresco is implemented as a subsystem in Alfresco and can easily be switched on and off (preferably with the JMX control mechanism, available through VisualVM). It uses the satellite and dispatcher setup that pdfaPilot has, and can therefore be run against a remote Windows server (if native Microsoft Office conversion is needed) from an Alfresco server which has the pdfaPilot CLI client.

## Getting started

### Dependencies

#### Alfresco

The extension is created with [Alfresco Maven SDK](https://arti
facts.alfresco.com/nexus/content/repositories/alfresco-docs/alfresco-lifecycle-aggregator/latest/index.html) and compiles against Alfresco Community 4.2.c. It's also been tested with:

* Alfresco Community 4.0.e and higher
* Alfresco Enterprise 3.4.8 and higher

##### Callas pdfaPilot

Callas pdfaPilot CLI 4.1.176 is recommended, although it may work with earlier versions (not tested). The satellite and dispatcher setup is required (look in the Callas docs for instruction) as this extension is implemented against that kind of setup.

### Building

The project use [Maven](http://maven.apache.org) (at least 3.0.x) for building. The default configuration expects the pdfaPilot dispatcher to run on a specific server and port and those values will most likely have to be configured in the Maven command for it to execute successfully. The actual path to the pdfaPilot CLI executable will most likely also have to be configured.

Get the source, enter the directory and execute

`mvn -Dpdfapilot.endpoint.host=<the IP address of the dispatcher> -Dpdfapilot.exe=<the path to the pdfaPilot executable> -Dpdfapilot.endpoint.port=<the port of the dispatcher> install`

to build and install the artifact in your local Maven repository.

### Configuration

The different configuration values that can used is entered in Alfresco's alfresco-global.properties file.

Key | Value
----|------
pdfapilot.endpoint.host|hostname or IP of pdfaPilot dispatcher 
pdfapilot.endpoint.port|port of pdfaPilot dispatcher
pdfapilot.exe|path of pdfaPilot CLI executable
pdfapilot.enabled|true or false

When the project has been built include

```xml
<dependency>
  <groupId>org.redpill-linpro.alfresco.repo.content.transform</groupId>
  <artifactId>pdfa-pilot-converter</artifactId>
  <version>1.0.2</version>
</dependency>  
```

in your Maven pom file for your Alfresco repository AMP project.