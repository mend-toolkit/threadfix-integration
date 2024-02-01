package org.mend.io;
import java.io.*;
import okhttp3.*;
import org.json.*;


import java.io.IOException;

public class MendAPI {
    private JSONObject _AuthenticationJSON;
    private static String _Token = "";
    private static boolean _Debug = false;
    public static void SetDebug(boolean debug)
    {
        _Debug = debug;
    }

    public JSONObject Authenticate(String mendEnvironmentLoginURL, String userName, String userKey) throws IOException, InterruptedException {
        if (_Debug) {
            System.out.println("Mend: API Authenticate");
        }

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create("{\r\n    \"username\" : \""+userName+"\",\r\n    \"userKey\" : \""+userKey+"\"\r\n}\r\n", mediaType);
        Request request = new Request.Builder()
                .url(mendEnvironmentLoginURL)
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();
        Thread.sleep(500);
        JSONObject authenticationJSON = new JSONObject(response.body().string());

        _AuthenticationJSON = authenticationJSON;
        try {
            _Token = _AuthenticationJSON.getJSONObject("retVal").get("jwtToken").toString();
        } catch (Exception ex)
        {
            //Eat the error
        }
        return authenticationJSON;
    }

    public JSONObject GetProductVulnerabilitiesAlerts(String mendEnvironmentAPIBaseURL, String productKey) throws IOException {
        if (_Debug) {
            System.out.println("Mend: API Get Product Vulnerabilities Alerts");
        }

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();

        Response response = client.newCall(BuildRequestGet(mendEnvironmentAPIBaseURL,"/v2.0/products/"+productKey+"/alerts/security?pageSize=5000")).execute();
        JSONObject projectJSON = new JSONObject(response.body().string());
        return WipeOutTopFixData(projectJSON);
    }

    public JSONObject GetProductLibraries(String mendEnvironmentAPIBaseURL, String productKey) throws IOException {
        if (_Debug) {
            System.out.println("Mend: API Get Product Libraries");
        }

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();

        Response response = client.newCall(BuildRequestGet(mendEnvironmentAPIBaseURL,"/v2.0/products/"+productKey+"/libraries?pageSize=5000")).execute();

        JSONObject projectJSON = new JSONObject(response.body().string());

        return projectJSON;
    }

    public JSONObject GetProjectVulnerabilitiesAlerts(String mendEnvironmentAPIBaseURL, String projectKey) throws IOException {
        if (_Debug) {
            System.out.println("Mend: API Get Project Vulnerabilities Alerts");
        }

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();

        Response response = client.newCall(BuildRequestGet(mendEnvironmentAPIBaseURL,"/v2.0/projects/"+projectKey+"/alerts/security?pageSize=5000")).execute();

        JSONObject projectJSON = new JSONObject(response.body().string());

        return WipeOutTopFixData(projectJSON);

    }

    public JSONObject GetProjectLibraries(String mendEnvironmentAPIBaseURL, String projectKey) throws IOException {
        if (_Debug) {
            System.out.println("Mend: API Get Project Libraries");
        }

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();

        Response response = client.newCall(BuildRequestGet(mendEnvironmentAPIBaseURL,"/v2.0/projects/"+projectKey+"/libraries?pageSize=5000")).execute();

        JSONObject projectJSON = new JSONObject(response.body().string());

        return projectJSON;
    }

    public JSONObject GetProjectLibraryVulnerabilities(String mendEnvironmentAPIBaseURL, String projectKey, String libraryKey) throws IOException {
        if (_Debug) {
            System.out.println("Mend: API Get Project Library Vulnerabilities - " + libraryKey);
        }

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();

        Response response = client.newCall(BuildRequestGet(mendEnvironmentAPIBaseURL,"/v2.0/projects/"+projectKey+"/libraries/"+libraryKey+"/vulnerabilities?pageSize=5000")).execute();

        JSONObject projectJSON = new JSONObject(response.body().string());

        return projectJSON;
    }

    public JSONObject GetProductListByOrganization(String mendEnvironmentAPIBaseURL, String mendOrgToken) throws IOException {
        if (_Debug) {
            System.out.println("Mend: API Get Product List by Org");
        }

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();

        Response response = client.newCall(BuildRequestGet(mendEnvironmentAPIBaseURL,"/v2.0/orgs/"+mendOrgToken+"/products?pageSize=5000")).execute();

        JSONObject productListJSON = new JSONObject(response.body().string());

        return productListJSON;
    }

    public JSONObject GetLibraryDetailsByLibraryId(String mendEnvironmentAPIBaseURL, String mendOrgToken, String libraryUUID) throws IOException {
        if (_Debug) {
            System.out.println("Mend: API Get Library Details by Library Id");
        }

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();

        Response response = client.newCall(BuildRequestGet(mendEnvironmentAPIBaseURL,"/v2.0/orgs/"+mendOrgToken+"/libraries/"+libraryUUID)).execute();

        JSONObject libraryDetailsJSON = new JSONObject(response.body().string());

        return libraryDetailsJSON;
    }

    public JSONObject GetOrganizationProductEntities(String mendEnvironmentAPIBaseURL, String mendOrgToken) throws IOException {
        if (_Debug) {
            System.out.println("Mend: API Get Organization Product Entities");
        }

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();

        Response response = client.newCall(BuildRequestGet(mendEnvironmentAPIBaseURL,"/v2.0/orgs/"+mendOrgToken+"/products")).execute();

        JSONObject productListJSON = new JSONObject(response.body().string());

        return productListJSON;
    }

    public JSONObject GetLibraryVulnerability(String mendEnvironmentAPIBaseURL, String mendOrgToken, String vulnerability) throws IOException {
        if (_Debug) {
            System.out.println("Mend: API Get Library Vulnerabilities");
        }

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();

        Response response = client.newCall(BuildRequestGet(mendEnvironmentAPIBaseURL,"/v2.0/orgs/"+mendOrgToken+"/vulnerabilities/"+vulnerability+"/Libraries" )).execute();

        JSONObject vulnerabilityResultJSON = new JSONObject(response.body().string());

        return vulnerabilityResultJSON;
    }

    public JSONObject GetProjectListByProductToken(String mendEnvironmentAPIBaseURL, String productToken) throws IOException {
        if (_Debug) {
            System.out.println("Mend: API Get Projects List by Product Token");
        }

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();

        Response response = client.newCall(BuildRequestGet(mendEnvironmentAPIBaseURL,"/v2.0/products/"+productToken+"/projects?pageSize=5000")).execute();

        JSONObject projectListJSON = new JSONObject(response.body().string());

        return projectListJSON;
    }

    private static Request BuildRequestGet(String baseURL, String urlSuffix)
    {
        Request request = new Request.Builder()
                .url(baseURL + urlSuffix)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer "+_Token)
                .build();

        return request;
    }

    private static Request BuildRequestPost(String baseURL, String urlSuffix)
    {
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\r\n\r\n}");
        Request request = new Request.Builder()
                .url(baseURL + urlSuffix)
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer "+_Token)
                .build();

        return request;
    }

    private static Request BuildRequestPost(String baseURL, String urlSuffix, String apiKey, String mediaTypeContent)
    {
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaTypeContent, mediaType);
        Request request = new Request.Builder()
                .url(baseURL + urlSuffix)
                .method("POST", body)
                .addHeader("Authorization", "APIKEY " + apiKey)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Cookie", "JSESSIONID=A849E4EACB1EA098C64C377845830ABA; XSRF-TOKEN=9b2bd1dd-ea11-45f2-aa1c-0b1602146478")
                .build();

        return request;
    }


    private JSONObject WipeOutTopFixData(JSONObject jsonObject)
    {
        if (_Debug) {
            System.out.println("Mend: Trim extra data - un-needed");
        }

        JSONArray productListJson = jsonObject.getJSONArray("retVal");
        for (int productI = 0; productI < productListJson.length(); productI++) {
            JSONObject currentProductJsonObject = productListJson.getJSONObject(productI);
            currentProductJsonObject.getJSONObject("topFix").remove("extraData");
        }
        return jsonObject;
    }

}
