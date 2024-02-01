package org.mend.io;

import java.io.*;
import okhttp3.*;
import org.json.*;

public class ThreadFixAPI {
    private static boolean _Debug = false;
    public static void SetDebug(boolean debug)
    {
        _Debug = debug;
    }

    //Application Area of API
    public static JSONObject GetApplicationList(String baseURL, String team, String apiKey) throws IOException {
        if (_Debug) {
            System.out.println("Threadfix: API Get Applications List");
        }

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();

        Response response = client.newCall(BuildRequestGet(baseURL, "/applications?team=" + team, apiKey)).execute();

        return new JSONObject(response.body().string());
    }

    //Team area of API
    public static JSONObject GetTeamDetailsByName(String baseURL, String team, String apiKey) throws IOException {
        if (_Debug) {
            System.out.println("Threadfix: API Get Team Details by Name");
        }

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();

        Response response = client.newCall(BuildRequestGet(baseURL, "/teams/lookup?name=" + team, apiKey)).execute();

        return new JSONObject(response.body().string());
    }

    public static String CreateApplication(String baseURL, String apiKey, String teamId, String appName, String appURL) throws IOException {
        if (_Debug) {
            System.out.println("Threadfix: API Create Application");
        }
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();

        Request request = BuildRequestPost(baseURL, "/teams/"+teamId+"/applications/new", apiKey, "name="+appName+"&url="+appURL);
        try (Response response = client.newCall(request).execute())
        {
            if (response.code() == 200)
            {
                JSONObject createApplicationJSON = new JSONObject(response.body().string());
                return (String)((JSONObject)createApplicationJSON.get("object")).get("id").toString();
            }
        };

        return "-1";
    }

    public static JSONObject UploadScanFile(String baseURL, String apiKey, String applicationId, String filename) throws IOException {
        if (_Debug) {
            System.out.println("Threadfix: API Upload Scan File");
        }

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();

        baseURL = baseURL.replace("{appId}",applicationId);

        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file",filename,
                        RequestBody.create(new File(filename), MediaType.parse("application/octet-stream")))
                .build();
        Request request = new Request.Builder()
                .url(baseURL)
                .method("POST", body)
                .addHeader("Authorization", "APIKEY " + apiKey)
                .addHeader("Accept", "application/json")
                .build();
        Response response = client.newCall(request).execute();

        if (_Debug) {
            System.out.println("Threadfix: API Upload Scan File Completed\r\n\r\n"+ response);
        }

        return new JSONObject();
        //return new JSONObject(response.body().string());
    }

    private static Request BuildRequestGet(String baseURL, String urlSuffix, String apiKey)
    {
        Request request = new Request.Builder()
                .url(baseURL + urlSuffix)
                .addHeader("Authorization", "APIKEY " + apiKey)
                .addHeader("Accept", "application/json")
                .addHeader("Cookie", "JSESSIONID=A849E4EACB1EA098C64C377845830ABA; XSRF-TOKEN=9b2bd1dd-ea11-45f2-aa1c-0b1602146478")
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

    static String GetTeamId(JSONObject teamDetailsByNameJSON)
    {
        try {
            return teamDetailsByNameJSON.getJSONObject("object").get("id").toString();
        } catch (JSONException e) {
            return "-1";
        }
    }

}
