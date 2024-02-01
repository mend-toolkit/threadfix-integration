package org.mend.io;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import com.google.gson.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {

    // Mend Variables
    private static MendAPI _MendAPI = new MendAPI();
    private static ThreadFixAPI _ThreadFixAPI = new ThreadFixAPI();
    private static String _MendEnvironmentLoginURL = "";
    private static String _MendEnvironmentAPIBaseURL = "";

    private static String _MendUserName =  "";
    private static String _MendUserKey =  "";
    private static String _MendOrgToken =  "";
    private static String _MendProductToken = "";
    private static String _MendProjectToken = "";
    private static String _MendProductName = "";
    private static String _MendProjectName = "";
    private static Boolean _MendAllProjects = false;

    // ThreadFix Variables
    private static String _ThreadFix_BaseURL =  "";
    private static String _ThreadFix_BaseUploadURL =  "";
    private static String _ThreadFix_apiKey =  "";
    private static String _ThreadFix_teamName =  "";
    private static String _ThreadFix_projectName =  "";
    private static String _ThreadFix_Id =  "";

    private static boolean _Debug = false;

    public static void main(String[] args) throws IOException, InterruptedException {
        // This method is entry point for this application.
        System.out.println("Starting");
        try {
            //Mend Pre-Work
            //Validate authentication first, this is needed for the command line params method

            if (args.length > 1) {
                //Process incoming command line parameters
                ProcessCommandLineParams(args);

                if (_MendProductToken.equals("Not Found"))
                {
                    System.out.println("The system will now exit.");
                    return;
                }

                _ThreadFixAPI.SetDebug(_Debug);
                _MendAPI.SetDebug(_Debug);

                System.out.println("Gathering Information");
                JSONObject mendAuthenticateJSON = MendAuthenticate();

                //ThreadFix Pre-Work
                JSONObject threadFixApplicationListJSON = ThreadFixApplicationList();

                JSONObject teamDetailsByNameJSON = _ThreadFixAPI.GetTeamDetailsByName(_ThreadFix_BaseURL,_ThreadFix_teamName, _ThreadFix_apiKey);
                String teamId = _ThreadFixAPI.GetTeamId(teamDetailsByNameJSON);

                if (_ThreadFix_projectName.equals(""))
                {
                    _ThreadFix_projectName = _MendProjectName;
                }

                String threadFixApplicationId = _ThreadFix_Id;

                if (_ThreadFix_Id.isBlank())
                {
                    threadFixApplicationId = ThreadFixGetApplicationID(_ThreadFix_projectName, threadFixApplicationListJSON, teamId);
                }

                if (_MendAllProjects)
                {
                    System.out.println("Gathering Information - Multiple Projects");
                    ProcessMultipleProjects(threadFixApplicationId);
                } else {
                    System.out.println("Gathering Information - single Project");
                    ProcessSingleProject(threadFixApplicationId);
                }

            } else {
                ProcessCommandLineParams(args);
                return;
            }

            System.out.println("Done!");

        } catch (Exception ex)
        {
            //This is the global error handler.  Any errors that make it back to the top will be handled here.
            System.out.println("An error has occurred.  Error Details: " + ex.getMessage());
            System.out.println("The system will now exit.");
        }
    }

    private static void ProcessMultipleProjects(String threadFixApplicationId) throws IOException, InterruptedException {
        //Mend Additional Detail Pre-Work
        JSONObject mendProductListJSON = _MendAPI.GetProductListByOrganization(_MendEnvironmentAPIBaseURL, _MendOrgToken);

        JSONObject mendProductAlertsJSON = _MendAPI.GetProductVulnerabilitiesAlerts(_MendEnvironmentAPIBaseURL, _MendProductToken);
        JSONObject mendProductLibrariesJSON = _MendAPI.GetProductLibraries(_MendEnvironmentAPIBaseURL, _MendProductToken);

        ProcessPostPrework(mendProductAlertsJSON);

        // Validate that we have what we need to continue.  A "-1" application ID means that we could not find/create an application
        if (threadFixApplicationId != "-1")
        {
            System.out.println("Processing");
            ProcessThreadFix(threadFixApplicationId, mendProductAlertsJSON, mendProductLibrariesJSON);
        }
    }

    private static void ProcessSingleProject(String threadFixApplicationId) throws IOException, InterruptedException {
        //Mend Additional Detail Pre-Work
        JSONObject mendProjectAlertsJSON = _MendAPI.GetProjectVulnerabilitiesAlerts(_MendEnvironmentAPIBaseURL, _MendProjectToken);
        JSONObject mendProjectLibrariesJSON = _MendAPI.GetProjectLibraries(_MendEnvironmentAPIBaseURL, _MendProjectToken);
        ProcessPostPrework(mendProjectAlertsJSON);

        // Validate that we have what we need to continue.  A "-1" application ID means that we could not find/create an application
        if (threadFixApplicationId != "-1")
        {
            System.out.println("Processing");
            ProcessThreadFix(threadFixApplicationId, mendProjectAlertsJSON, mendProjectLibrariesJSON);
        }
    }

    private static void ProcessPostPrework(JSONObject mendProjectJSON)
    {
        //Set product and project name global variables
        if (_MendProductName == "")
        {
            _MendProductName = StripNonAlphaChars(MendGetProductName(mendProjectJSON),"");
        }

        if (_MendProjectName == "")
        {
            _MendProjectName = StripNonAlphaChars(MendGetProjectName(mendProjectJSON), "");
        }

        //Set product and project name global variables
        if (_MendProductName == "")
        {
            _MendProductName = "Product name not found";
        }

        if (_MendProjectName == "")
        {
            _MendProjectName = "Project name not found";
        }
    }

    private static void ProcessThreadFix(String threadFixApplicationId, JSONObject mendProjectJSON, JSONObject mendProjectLibrariesJSON) throws IOException {
        System.out.println("Build, Process and Upload to ThreadFix server.  Application ID: "+threadFixApplicationId);

        //Build ThreadFixVulnerabilityResultModel
        ThreadFixVulnerabilityResultModel threadFixVulnerabilityResult = BuildThreadFixVulnerabilityResult(mendProjectJSON, mendProjectLibrariesJSON);

        //Export model into JSON string
        String exportFileJSON = BuildUploadFile(threadFixVulnerabilityResult);

        //Write file
        String filename = BuildFilename();
        WriteFile(filename, exportFileJSON);

        //Upload the file to Threadfix
        _ThreadFixAPI.UploadScanFile(_ThreadFix_BaseUploadURL, _ThreadFix_apiKey, threadFixApplicationId, filename);

        System.out.println("Done uploading scan details");
    }

    private static void ProcessCommandLineParams(String[] args) throws IOException, InterruptedException {
        // This method takes the command line parameters entered in and set's the working variable details with what has been passed in.
        String currentParameter = "";

        System.out.println("Process CommandLine Params");
        if (args.length <= 1)
        {
            DisplayHelp();
        }

        try
        {
            for (int itemI = 0; itemI < args.length; itemI++) {
                String[] parameter = args[itemI].split("=");
                // assumption is there are only two elements per parameter string item
                String key = parameter[0].trim();
                String value = parameter[1].trim();

                currentParameter = key + " - " + value;

                switch (key.toLowerCase()) {
                    case "help":

                        break;

                    case "mendurl" , "murl":
                        if (!value.toLowerCase().contains("https://"))
                        {
                            value = "https://"+value;
                        }

                        _MendEnvironmentLoginURL = value + "/api/v2.0/login";
                        _MendEnvironmentAPIBaseURL = value.replace("https://","https://api-") + "/api";
                        break;

                    case "threadfixurl", "tfurl":
                        _ThreadFix_BaseURL = value + "/rest/latest";
                        _ThreadFix_BaseUploadURL = value+ "/rest/latest/applications/{appId}/upload";
                        break;

                    case "mendusername", "musername", "username", "mun":
                        _MendUserName = value;
                        break;

                    case "menduserkey" , "muserkey", "userkey", "muk":
                        _MendUserKey = value;
                        break;

                    case "mendorgtoken", "morgtoken", "morg", "mot", "apikey":
                        _MendOrgToken = value;
                        break;

                    case "mendproducttoken", "mproducttoken", "mprodt", "producttoken":
                        _MendProductToken = value;
                        break;

                    case "mendprojecttoken", "mprojecttoken", "mprojt", "projecttoken":
                        _MendProjectToken = value;
                        break;

                    case "mendproductname", "mproductname", "mprodn", "productname":
                        _MendProductName = value;
                        break;

                    case "mendprojectname", "mprojectname", "mprojn", "projectname":
                        _MendProjectName = value;
                        break;

                    case "threadfixteamname", "tfteamname", "tftn", "teamname":
                        _ThreadFix_teamName = value;
                        break;

                    case "threadfixprojectname":
                        _ThreadFix_projectName = value;
                        break;

                    case "threadfixapikey", "tfapikey":
                        _ThreadFix_apiKey = value;
                        break;

                    case "threadfixid":
                        _ThreadFix_Id = value;
                        break;

                    case "debug":
                        _Debug = Boolean.valueOf(value);
                        if (_Debug)
                        {
                            System.out.println("Debug Mode Enabled");
                        }
                        break;
                }
            }

            JSONObject mendAuthenticateJSON = MendAuthenticate();
            System.out.println("Login Org Scope: " + ReadValueFromJSON(mendAuthenticateJSON, "retVal", "orgName"));

            if (_MendProductToken.equals("") && !_MendProductName.equals(""))
            {
                JSONObject mendProductListJSON = _MendAPI.GetProductListByOrganization(_MendEnvironmentAPIBaseURL, _MendOrgToken);
                _MendProductToken = MendGetProductTokenIdByProductName(mendProductListJSON, _MendProductName);
                if (_MendProductToken.equals("Not Found"))

                {
                    return;
                }
            }

            //If we did not provide a project key and we have a project token, let's find the project token off of the project name
            if (_MendProjectToken == "" && _MendProjectName != "" && _MendProductName != "")
            {
                JSONObject mendProductListJSON = _MendAPI.GetProductListByOrganization(_MendEnvironmentAPIBaseURL, _MendOrgToken);
                _MendProjectToken = MendGetProjectTokenIdByProjectName(mendProductListJSON, _MendProductName, _MendProjectName);
            }

            //Ensure we have everything we need to continue.  If not, then we need to let the user know and exit.
            if (_MendProjectToken == "" &&( _MendProjectName == "" || _MendProductName == ""))
            {
                //throw new RuntimeException("Unable to continue.  You must provide a Mend Project Token or the Product and Project names in order to continue.");
            }

            if (_MendProjectToken.equals("") && _MendProjectName.equals("") && !_MendProductToken.equals(""))
            {
                System.out.println("Threadfix results will include ALL projects inside the selected product.");
                _MendAllProjects = true;
            }

            System.out.println("Debug: " + _Debug);
            if (_Debug)
            {
                System.out.println("\r\nMend / Threadfix Parameter Values:\r\n");
                System.out.println("Process all projects as one Threadfix application = " + _MendAllProjects.toString());
                System.out.println("mendURL, murl - URL to the mend server = " + _MendEnvironmentLoginURL );
                System.out.println("mendusername, musername, username, mun - Mend Username = " + _MendUserName);
                System.out.println("menduserkey, muserkey, userkey, muk - Mend User Key = " + _MendUserKey);
                System.out.println("apikey, mendorgtoken, morgtoken, morg, mot - Mend API/Org Token = " + _MendOrgToken );
                System.out.println("mendproducttoken, mproducttoken, mprodt - Mend Product Token = " + _MendProductToken);
                System.out.println("mendprojecttoken, mprojecttoken, mprojt - Mend Project Token = " + _MendProjectToken);
                System.out.println("mendproductname, mproductname, mprodn, productname - Mend Product Name = " + _MendProductName);
                System.out.println("mendprojectname, mprojectname, mprojn, projectname - Mend Project Name = " + _MendProjectName);

                System.out.println("threadfixprojectname = " + _ThreadFix_projectName);
                System.out.println("threadfixurl, tfurl - URL to ThreadFix server = " + _ThreadFix_BaseURL);
                System.out.println("threadfixapikey, tfapikey - ThreadFix API Key = " + _ThreadFix_apiKey);
                System.out.println("threadfixid - ThreadFix Id = " + _ThreadFix_Id);
                System.out.println("threadfixteamname, tfteamname, tftn, teamname - ThreadFix Team Name = " + _ThreadFix_teamName + "\r\n\r\n");
            }
        }

        catch (Exception ex)
        {
            System.out.println("\r\n\r\nCommand Line Parameter error: " + ex.getMessage());
            System.out.println("\r\nCommand Line Parameter issue: " + currentParameter);
            DisplayHelp();
        }
    }

    private static void DisplayHelp()
    {
        System.out.println("\r\n\r\nMend / Threadfix integration help:");
        System.out.println("\r\nParameters");
        System.out.println("mendURL, murl - URL to the mend server.  IE: https://saas.whitesourcesofware.com");
        System.out.println("mendusername, musername, username, mun - Mend Username");
        System.out.println("menduserkey, muserkey, userkey, muk - Mend User Key");
        System.out.println("apikey, mendorgtoken, morgtoken, morg, mot - Mend API/Org Token");
        System.out.println("mendproducttoken, mproducttoken, mprodt - Mend Product Token");
        System.out.println("mendprojecttoken, mprojecttoken, mprojt - Mend Project Token");
        System.out.println("mendproductname, mproductname, mprodn, productname - Mend Product Name");
        System.out.println("mendprojectname, mprojectname, mprojn, projectname - Mend Project Name");
        System.out.println("threadfixid - Thread Fix Application ID");
        System.out.println("threadfixprojectname - ThreadFix Project Name - auto set if not supplied");
        System.out.println("threadfixurl, tfurl - URL to ThreadFix server.  IE: http://192.168.1.22:8080/threadfix/rest/latest");
        System.out.println("threadfixapikey, tfapikey - ThreadFix API Key");
        System.out.println("threadfixteamname, tfteamname, tftn, teamname - ThreadFix Team Name\r\n");
        return;
    }

    private static JSONObject MendAuthenticate() throws IOException, InterruptedException {
        //For some reason in my development environment, the system sometimes does not successfully authenticate on the first attempt.
        //  Because of this, we will give it 3 attempts to successfully authenticate before giving up.
        int attempts = 0;

        while (attempts < 8)
        {
            try {
                if (attempts > 3)
                {
                    _MendEnvironmentLoginURL = _MendEnvironmentLoginURL.toLowerCase().replace("https://","https://api-");
                }


                JSONObject mendAuthenticateJSON = _MendAPI.Authenticate(_MendEnvironmentLoginURL, _MendUserName, _MendUserKey);
                String returnCode = ReadValueFromJSON(mendAuthenticateJSON,"message");
                if (returnCode != "No Data Found")
                {
                    attempts++;
                } else {
                    return mendAuthenticateJSON;
                }
            } catch (Exception ex) {
                //Eat the error in case of timeout
                attempts++;
            }
        }
        throw new RuntimeException("Authentication Timeout. Since the system was unable to authenticate, we are now going to exit.");
    }

    private static JSONObject ThreadFixApplicationList() throws IOException, InterruptedException {
        // This method returns the ThreadFix application list from the ThreadFix server.
        // Sometimes the ThreadFix API has gone to sleep.  This method is used since it's the first in many to be used.
        //  If the system was asleep, this method will wake it up and return the data that we need.

        int attempts = 0;
        JSONObject threadFixApplicationListJSON = new JSONObject();
        while (attempts < 10)
        {
            try {
                threadFixApplicationListJSON = _ThreadFixAPI.GetApplicationList(_ThreadFix_BaseURL,_ThreadFix_teamName, _ThreadFix_apiKey);
                String returnCode = ReadValueFromJSON(threadFixApplicationListJSON,"success");
                if (returnCode != "true")
                {
                    attempts++;
                } else {
                    return threadFixApplicationListJSON;
                }
            } catch (Exception ex) {
                //Eat the error, system is waking up since we just called it.
            }
            attempts++;
        }

        throw new RuntimeException("Threadfix ThreadFixApplicationList Timeout. Since the system was unable to retrieve data, we are now going to exit.");
    }

    private static ThreadFixVulnerabilityResultModel BuildThreadFixVulnerabilityResult(JSONObject mendProjectJSON, JSONObject mendProjectLibrariesJSON)
    {
        // This method builds the ThreadFix JSON that is sent to ThreadFix to add scan results
        ThreadFixVulnerabilityResultModel threadFixVulnerabilityResultModel = new ThreadFixVulnerabilityResultModel();
        threadFixVulnerabilityResultModel = BuildFileHeader(threadFixVulnerabilityResultModel);
        threadFixVulnerabilityResultModel = BuildFileFindings(threadFixVulnerabilityResultModel, mendProjectJSON, mendProjectLibrariesJSON);
        return threadFixVulnerabilityResultModel;
    }

    private static ThreadFixVulnerabilityResultModel BuildFileHeader(ThreadFixVulnerabilityResultModel threadFixVulnerabilityResultModel)
    {
        //Builds the threadfix header information
        try {
            // 'Zulu Time' format.  Setting the Created, Updated and Exported date to current ensures that all of the
            //      information sent to ThreadFix will be reviewed and not ignored.
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            String scanDate = simpleDateFormat.format(new Date());

            threadFixVulnerabilityResultModel.setCreated(scanDate);
            threadFixVulnerabilityResultModel.setUpdated(scanDate);
            threadFixVulnerabilityResultModel.setExported(scanDate);
            threadFixVulnerabilityResultModel.setCollectionType("DEPENDENCY");
            threadFixVulnerabilityResultModel.setSource("Mend");

        } catch (Exception ex) {
            return threadFixVulnerabilityResultModel;
        }

        //If we built a file, return true, otherwise return false
        return threadFixVulnerabilityResultModel;
    }

    private static ThreadFixVulnerabilityResultModel BuildFileFindingsForMultipleProjects(ThreadFixVulnerabilityResultModel threadFixVulnerabilityResultModel,
                                                                       JSONObject mendProductJSON, JSONObject mendProdctLibrariesJSON)
    {
        try {
            // Loop through each finding
            List<ThreadFixVulnerabilityAlertResultModel> vulnerabilityAlertResult = new ArrayList<ThreadFixVulnerabilityAlertResultModel>();

            JSONArray productListJson = mendProductJSON.getJSONArray("retVal");
            for (int productI = 0; productI < productListJson.length(); productI++) {
                JSONObject currentProductJsonObject = productListJson.getJSONObject(productI);
            }
            return threadFixVulnerabilityResultModel;
        } catch (Exception ex) {

        }
        return null;
    }

    private static ThreadFixVulnerabilityResultModel BuildFileFindings(ThreadFixVulnerabilityResultModel threadFixVulnerabilityResultModel,
                                                                       JSONObject mendProjectJSON, JSONObject mendProjectLibrariesJSON)
    {
        //Builds the findings array.  Part of the overall JSON payload that is sent to ThreadFix.
        try {
            // Loop through each finding
            List<ThreadFixVulnerabilityAlertResultModel> vulnerabilityAlertResult = new ArrayList<ThreadFixVulnerabilityAlertResultModel>();
            boolean includeResult = false;

            JSONArray productListJson = mendProjectJSON.getJSONArray("retVal");
            for (int productI = 0; productI < productListJson.length(); productI++) {
                JSONObject currentProductJsonObject = productListJson.getJSONObject(productI);

                String statusValue = ReadValueFromJSON(currentProductJsonObject,"alertInfo","status");

                includeResult = false;
                if (statusValue.toLowerCase().equals("active") || statusValue.toLowerCase().equals("inactive"))
                {
                    includeResult = true;
                }

                if (_Debug)
                {
                    System.out.println("Include Result - " + includeResult + " - " + statusValue);
                }

                String product = ReadValueFromJSON(currentProductJsonObject,"product","name");
                String project = ReadValueFromJSON(currentProductJsonObject,"project","name");

                System.out.println("Processing vulnerability record " + productI);

                DependencyDetailsModel dependencyDetails = new DependencyDetailsModel();
                dependencyDetails.setLibrary(ReadValueFromJSON(currentProductJsonObject,"component","name"));
                dependencyDetails.setDescription(ReadValueFromJSON(currentProductJsonObject,"component","description"));
                dependencyDetails.setIssueType(ReadValueFromJSON(currentProductJsonObject,"type"));
                dependencyDetails.setReference(ReadValueFromJSON(currentProductJsonObject,"name"));
                dependencyDetails.setVersion(ReadVersionFromJSON(mendProjectLibrariesJSON,ReadValueFromJSON(currentProductJsonObject,"component","uuid"), "version"));
                //dependencyDetails.setReferenceLink(ReadVersionFromJSON(mendProjectLibrariesJSON,ReadValueFromJSON(currentProductJsonObject,"component","uuid"), "version"));

                ThreadFixVulnerabilityAlertResultModel threadFixVulnerabilityAlertResult = new ThreadFixVulnerabilityAlertResultModel();

                // The following is the component UUID, might consider using uuid from the alert instead
                //threadFixVulnerabilityAlertResult.setNativeId(ReadValueFromJSON(currentProductJsonObject,"component","uuid"));

                //This UUID is from the alert, so it should be considered the correct value
                threadFixVulnerabilityAlertResult.setNativeId(ReadValueFromJSON(currentProductJsonObject,"uuid"));
                threadFixVulnerabilityAlertResult.setSeverity(SetSeverity(ReadValueFromJSON(currentProductJsonObject,"vulnerability","score")));

                //Existing AVM output's like this, so to keep it consistant, we'll do the same
                threadFixVulnerabilityAlertResult.setNativeSeverity(ReadValueFromJSON(currentProductJsonObject,"vulnerability","score"));

                //Probably should be this, but looking at existing AVM, we won't use this
                //threadFixVulnerabilityAlertResult.setNativeSeverity(SetSeverity(ReadValueFromJSON(currentProductJsonObject,"vulnerability","score")));

                threadFixVulnerabilityAlertResult.setCvssScore(Double.parseDouble(ReadValueFromJSON(currentProductJsonObject,"vulnerability","score")));
                threadFixVulnerabilityAlertResult.setWssIgnoredAlert(ReadIgnoredAlert(currentProductJsonObject));
                threadFixVulnerabilityAlertResult.setFalsePositive(threadFixVulnerabilityAlertResult.isWssIgnoredAlert());
                //threadFixVulnerabilityAlertResult.setTags(SetDefaultTags());
                threadFixVulnerabilityAlertResult.setTags(SetDefaultTags(product,project));

                //Short Term fix - The description data from the alert API call is missing.  Need to pull in additional data to fill in the summary details
                String summery = ReadValueFromJSON(currentProductJsonObject,"vulnerability","description");
                if (summery == "")
                {
                    String libraryUUID = ReadValueFromJSON(currentProductJsonObject,"component","uuid");
                    JSONObject mendProjectLibraryVulnerabilitiesJSON = _MendAPI.GetProjectLibraryVulnerabilities(_MendEnvironmentAPIBaseURL, _MendProjectToken, libraryUUID);
                    summery = ReadVulnerabilityDescriptionFromCVSS(mendProjectLibraryVulnerabilitiesJSON, dependencyDetails.getReference(),"description");
                }
                threadFixVulnerabilityAlertResult.setSummary(SetSummary(summery));
                threadFixVulnerabilityAlertResult.setDescription(summery);

                //Short Term Fix End - Un-comment both of these lines once developer adds in the missing description data
                //threadFixVulnerabilityAlertResult.setSummary(ReadValueFromJSON(currentProductJsonObject,"vulnerability","description"));
                //threadFixVulnerabilityAlertResult.setDescription(ReadValueFromJSON(currentProductJsonObject,"vulnerability","description"));

                threadFixVulnerabilityAlertResult.setScannerRecommendation(ReadValueFromJSON(currentProductJsonObject,"topFix","type"));
                threadFixVulnerabilityAlertResult.setDependencyDetails(dependencyDetails);
                if (includeResult)
                {
                    vulnerabilityAlertResult.add(threadFixVulnerabilityAlertResult);
                }
            }

            threadFixVulnerabilityResultModel.setVulnerabilityAlertResult(vulnerabilityAlertResult);
            threadFixVulnerabilityResultModel.setSource("WhiteSource");

        } catch (Exception ex) {
            return threadFixVulnerabilityResultModel;
        }

        //If we built a file, return true, otherwise return false
        return threadFixVulnerabilityResultModel;
    }

    private static String[] SetDefaultTags()
    {
        //The old AVM system sent this as the default tag.  Keeping the logic the same to support existing implementations
        return new String[] {_MendProjectName};
    }

    private static String[] SetDefaultTags(String product, String project)
    {
        //The old AVM system sent this as the default tag.  Keeping the logic the same to support existing implementations
        return new String[] {project};
    }

    private static String SetSummary(String summary)
    {
        // The summary can not exceed 130 chars.
        if (summary.length() > 125)
        {
            summary = summary.substring(0,124) + "...";
        }
        return summary;
    }

    private static String SetSeverity(String cvssScore)
    {
        double result = Double.valueOf(cvssScore);

        if (result < 4)
        {
            return "Low";
        } else if (result >= 4 && result < 7) {
            return "Medium";
        } else if (result >= 7 && result < 9) {
            return "High";
        } else if (result >= 9) {
            return "Critical";
        }
        return "Unknown";
    }

    private static boolean ReadIgnoredAlert(JSONObject currentProductJsonObject) {
        try {
            String value = ReadValueFromJSON(currentProductJsonObject,"alertInfo","status");
            if (value.toUpperCase().equals("IGNORED"))
            {
                return true;
            }

        } catch (Exception ex)
        {
            //Eat the error since we are still wanting to return false
        }
        return false;
    }

    private static String ReadVulnerabilityDescriptionFromCVSS(JSONObject mendProjectVulnerabilitiesJSON, String cve, String key)
    {
        try {
            JSONArray productListJson = mendProjectVulnerabilitiesJSON.getJSONArray("retVal");
            String version = "";

            for (int productI = 0; productI < productListJson.length(); productI++) {
                JSONObject currentProductJsonObject = productListJson.getJSONObject(productI);

                if (currentProductJsonObject.get("name").toString().toUpperCase().equals(cve.toUpperCase()))
                {
                    return currentProductJsonObject.get(key).toString();
                }
            }
        } catch (Exception ex) {
            //Eat the error
        }

        return "Blank or missing description";
    }

    private static String ReadVersionFromJSON(JSONObject mendProjectLibrariesJSON, String LibraryUUID, String key)
    {
        JSONArray productListJson = mendProjectLibrariesJSON.getJSONArray("retVal");
        String version = "";

        for (int productI = 0; productI < productListJson.length(); productI++) {
            JSONObject currentProductJsonObject = productListJson.getJSONObject(productI);

            if (currentProductJsonObject.get("uuid").toString().toUpperCase().equals(LibraryUUID.toUpperCase()))
            {
                return currentProductJsonObject.get(key).toString();
            }

        }
        return "";
    }

    private static String ReadValueFromJSON(JSONObject jsonObject, String sectionKey, String key)
    {
        try
        {
            return (jsonObject.getJSONObject(sectionKey)).get(key).toString();
        } catch (Exception ex)
        {
            //Eat the error
            return "No Data Found";
        }
    }

    private static String ReadValueFromJSON(JSONObject jsonObject, String key)
    {
        try
        {
            return jsonObject.get(key).toString();
        } catch (Exception ex)
        {
            //Eat the error
            return "No Data Found";
        }
    }

    private static String BuildFilename()
    {
        //This method will return a filename based on the product - project name
        //  IE: product - project_Threadfix_scan_1219.threadfix
        Calendar mCalendar = Calendar.getInstance();
        String month = mCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
        int day = mCalendar.get(Calendar.DATE);

        try {
            if (_MendAllProjects)
            {
                return _MendProductName + "-_ThreadFix_scan_"+month+day+".threadfix";
            } else {
                return _MendProductName + "-" + _MendProjectName + "_ThreadFix_scan_"+month+day+".threadfix";
            }

        } catch (Exception ex) {
            //Something in the product / project name caused issues, so continue, but with a hardcoded name
            return "Default_ThreadFix_scan_"+month+day+".threadfix";
        }
    }

    private static String BuildUploadFile(ThreadFixVulnerabilityResultModel threadFixVulnerabilityResultModel)
    {
        try {
            //This code will take an object and convert it over to a JSON formatted string
            Gson gson = new Gson();
            return gson.toJson(threadFixVulnerabilityResultModel);
        } catch (Exception ex) {
            return "";
        }
    }

    private static void WriteFile(String filename, String data) {
        try {
            System.out.println("Saving results: " + filename);
            Files.write(Paths.get(filename), data.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String ThreadFixGetApplicationID(String mendProjectName, JSONObject threadFixApplicationListJSON, String teamId) throws IOException {
        // This method will retrieve the Application ID which is required.  In the event that it does not yet exist,
        //    we will create it and then use it.

        // If teamId is missing, then no reason to try to find/create application
        if (teamId == "-1")
        {
            System.out.println("TheadFix TeamID is invalid.");
            return "-1";
        }

        //Determine if the application exists, if it does, we update it.  If it doesn't, then we create it
        String threadFixApplicationId = MendProjectExistsInThreadFix(mendProjectName, threadFixApplicationListJSON);

        if (threadFixApplicationId == "-1")
        {
            //Creates the application in threadFix
            threadFixApplicationId = _ThreadFixAPI.CreateApplication(_ThreadFix_BaseURL, _ThreadFix_apiKey, teamId, mendProjectName, "");
        }

        if (threadFixApplicationId == "-1")
        {
            System.out.println("TheadFix Find/Create Application Failure.");
        }

        return threadFixApplicationId;
    }

    private static String MendProjectExistsInThreadFix(String mendProjectName, JSONObject threadFixApplicationListJSON )
    {
        JSONArray threadFixApplications = threadFixApplicationListJSON.getJSONArray("object");
        for (int appI = 0; appI < threadFixApplications.length(); appI++) {
            JSONObject currentApplicationObject = threadFixApplications.getJSONObject(appI);

            if (currentApplicationObject.get("name").toString().toLowerCase().equals(mendProjectName.toLowerCase()))
            {
                return currentApplicationObject.get("id").toString();
            }
        }

        return "-1";
    }

    private static String MendGetProductTokenIdByProductName(JSONObject mendProductListJSON, String productName) throws IOException {
        if (_Debug)
        {
            System.out.println("Searching for "+ productName);
        }
        JSONArray productListJson = mendProductListJSON.getJSONArray("retVal");
        for (int productI = 0; productI < productListJson.length(); productI++) {
            JSONObject currentProductJsonObject = productListJson.getJSONObject(productI);

            String jsonProductName = currentProductJsonObject.get("name").toString().toLowerCase();

            if (_Debug)
            {
                System.out.println("Product Name: " + jsonProductName);
            }

            if (jsonProductName.equals(productName.toLowerCase())) {
                if (_Debug)
                {
                    System.out.println("Found Product Name: " + jsonProductName);
                }
                return currentProductJsonObject.get("uuid").toString();
            }
        }
        System.out.println("Mend could not find the provided Product Name:" + productName);
        return "Not Found";
    }
    private static String MendGetProjectTokenIdByProjectName(JSONObject mendProductListJSON, String productName,
                                                             String projectName) throws IOException {
        JSONArray productListJson = mendProductListJSON.getJSONArray("retVal");
        for (int productI = 0; productI < productListJson.length(); productI++) {
            JSONObject currentProductJsonObject = productListJson.getJSONObject(productI);

            if (currentProductJsonObject.get("name").toString().toLowerCase().equals(productName.toLowerCase())) {

                JSONObject projectListJSON = _MendAPI.GetProjectListByProductToken(_MendEnvironmentAPIBaseURL, currentProductJsonObject.get("uuid").toString());
                JSONArray projectList = projectListJSON.getJSONArray("retVal");

                for (int projectI = 0; projectI < projectList.length(); projectI++) {
                    JSONObject currentProjectJsonObject = projectList.getJSONObject(projectI);

                    if (currentProjectJsonObject.get("name").toString().toLowerCase().equals(projectName.toLowerCase())) {
                        return currentProjectJsonObject.get("uuid").toString();
                    }
                }
            }
        }
        return "";
    }

    private static String MendGetProjectName(JSONObject mendProjectJSON)
    {
        JSONArray projectJSON = mendProjectJSON.getJSONArray("retVal");
        if (projectJSON.length() > 0)
        {
            return (projectJSON.getJSONObject(0)).getJSONObject("project").get("name").toString();
        }

        return "";
    }

    private static String MendGetProductName(JSONObject mendProjectJSON)
    {
        JSONArray projectJSON = mendProjectJSON.getJSONArray("retVal");
        if (projectJSON.length() > 0)
        {
            return (projectJSON.getJSONObject(0)).getJSONObject("product").get("name").toString();
        }

        return "";
    }

    private static String StripNonAlphaChars(String value, String replacementValue)
    {
        return value.replaceAll("[^a-zA-Z0-9]", replacementValue);
    }

}