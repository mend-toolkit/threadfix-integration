[![Logo](https://resources.mend.io/mend-sig/logo/mend-dark-logo-horizontal.png)](https://www.mend.io/)
[![License](https://img.shields.io/badge/License-Apache%202.0-yellowgreen.svg)](https://opensource.org/licenses/Apache-2.0)

## Supported Operating Systems
- **Linux (Bash):**	CentOS, Debian, Ubuntu, RedHat
- **Windows (PowerShell):**	10, 2012, 2016

## Pre-requisites 
* JDK 17

# Usage Example
Regarding accessing API's, it's best to use a Mend Service User.  The MendURL auto appends the additional API URL, so you only need to provide the prefix portion: https://saas.whitesourcesoftware.com

There are two approaches - you can either specify the Threadfix Application ID or provide a Threadfix ProjectName.  Specifying "ProjectName" will lookup the Application ID.  If the application does not exist, it will create the application in Threadfix and populate with data.

Threadfix Application ID  Example:
```shell
java -jar ThreadFix.jar mendurl="https://{Mend URL}" \
username="{Username}" userkey="{UserKey}" \
apikey="{Mend Api Key}" productname="{Mend Product Name}" \
teamname="Mend" threadfixapikey="{tf-apikey}" \
threadfixurl="http://{Your Server IP Address}:8080/threadfix" \
threadfixid={application id}
```
Threadfix Project Name Example:
```shell
java -jar ThreadFix.jar mendurl="https://{Mend URL}" \
username="{Username}" userkey="{UserKey}" \
apikey="{Mend Api Key}" productname="{Mend Product Name}" projectname="{Mend Project Name}"\
teamname="Mend" threadfixapikey="{tf-apikey}" \
threadfixurl="http://{Your Server IP Address}:8080/threadfix" \
threadfixprojectname="{Threadfix ProjectName}"
```
