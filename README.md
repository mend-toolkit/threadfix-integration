# Usage Example
Regarding accessing API's, it's best to use a Mend Service User.  The MendURL auto appends the additional API URL, so you only need to provide the prefix portion: https://saas.whitesourcesoftware.com

There are two approaches - you can either specify the Threadfix Application ID or provide a Threadfix ProjectName.  The "ProjectName" will lookup the Application ID and actually use that.  If the application does not exist, it will create the application for you and then use it.

Basic Example:
```shell
java -jar ThreadFix.jar mendurl="https://{Mend URL}" username="{Username}" userkey="{UserKey}" \
apikey="{Mend Api Key}" productname="{Mend Product Name}" teamname="Mend" \
threadfixapikey="{tf-apikey}" threadfixurl="http://{Your Server IP Address}:8080/threadfix" threadfixid={application id}
```
```shell
java -jar ThreadFix.jar mendurl="https://{Mend URL}" username="{Username}" userkey="{UserKey}" \
apikey="{Mend Api Key}" productname="{Mend Product Name}" projectname="{Mend Project Name}" teamname="Mend" \
threadfixapikey="{tf-apikey}" threadfixurl="http://{Your Server IP Address}:8080/threadfix" threadfixprojectname="{Threadfix ProjectName}"
```
