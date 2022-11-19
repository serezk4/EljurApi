package com.serezka.eljur;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Log4j2
public class EljurApi {
    // default eljur request date format
    SimpleDateFormat API_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

    String eljurDevKey;                 // dev key to use api
    String eljurVendor;                 // school vendor
    String eljurAuthorizationToken;     // user authorization token to accept data

    String mainUrl;                     // eljur requests main url

    private static final Gson gson = new Gson();

    public EljurApi(String eljurDevKey, String eljurVendor, String eljurAuthorizationToken) {
        this.eljurDevKey = eljurDevKey;
        this.eljurVendor = eljurVendor;
        this.eljurAuthorizationToken = eljurAuthorizationToken;

        this.mainUrl = String.format("https://api.eljur.ru/api/{method}?devkey=%s&auth_token=%s&vendor=%s", eljurDevKey, eljurAuthorizationToken, eljurVendor);
    }

    private String generateLink(String methodName, Map<String, String> addons) {
        StringBuilder resultUrl = new StringBuilder(mainUrl.replace("{method}", methodName));
        for (Map.Entry<String, String> addon : addons.entrySet())
            resultUrl.append(String.format("&%s=%s", addon.getKey(), addon.getValue()));
        return resultUrl.toString();
    }

    // methods

    // get marks method

    /**
     * @param fromDate - | between witch \
     * @param toDate -   |                \ dates return marks
     * @return success -> marks for selected dates in format Map<[student name], Map<[lesson]>, List<[marks]>>> | error -> empty map
     */
    public Map<String, Map<Marks.Lesson, List<Marks.Mark>>> requestMarks(Calendar fromDate, Calendar toDate)  {
        // generate link
        String requestMarksUrl = generateLink(
                "getmarks",
                Map.of("days", API_DATE_FORMAT.format(fromDate.getTimeInMillis()) + "-" + API_DATE_FORMAT.format(toDate.getTimeInMillis())));

        // parse data
        URL     apiRequest  ;
        String  apiResponse ;

        try {
            apiRequest      = new URL(requestMarksUrl);
            apiResponse  = ApiUtils.parseUrl(apiRequest);
        } catch (IOException e) {
            log.warn(e.getMessage());
            return Collections.emptyMap();
        }

        // get data from url and deserialize to object
        Marks.Response response = gson.fromJson(apiResponse, Marks.Root.class).response;
        if (response.error != null) {
            log.warn("response error: {}", response.error);
            return Collections.emptyMap();
        }

        // format
        Map<String, Map<Marks.Lesson, List<Marks.Mark>>> marks = new HashMap<>();
        response.result.students.forEach((s, student) -> {
            Map<Marks.Lesson, List<Marks.Mark>> tempStudentMarks = new HashMap<>();
            student.lessons.forEach(lesson -> tempStudentMarks.put(lesson, lesson.marks));
            marks.put(student.name, tempStudentMarks);
        });

        return marks;
    }

    // get homework request

    /**
     * @param schoolClass - selected school class name
     * @param fromDate - | between witch \
     * @param toDate   - |                \ dates return marks
     * @return success -> homework for selected dates in format Map<[day], [day data]> | error -> empty map
     */
    public Map<String, Hometasks.Day> requestHomework(String schoolClass, Calendar fromDate, Calendar toDate) {
        String requestHomeworkUrl = generateLink("gethomework", Map.of("class", schoolClass, "days",
                API_DATE_FORMAT.format(fromDate.getTime()) + "-" + API_DATE_FORMAT.format(toDate.getTime())));

        // get data from url
        URL apiRequest;
        String apiResponse;

        try {
            apiRequest = new URL(requestHomeworkUrl);
            apiResponse = ApiUtils.parseUrl(apiRequest);
        } catch (IOException e) {
            log.warn(e.getMessage());
            return Collections.emptyMap();
        }

        // deserialize to object
        Hometasks.Response response = gson.fromJson(apiResponse, Hometasks.Root.class).response;
        if (response.error != null) {
            log.warn("response error: " + response.error);
            return Collections.emptyMap();
        }

        return response.result.days;
    }

    /**
     * @param eljurLogin    - eljur account username
     * @param eljurPassword - eljur account password
     * @return success -> authorization token data | error -> null
     */
    public Authorization.TokenInfo requestAuthorizationToken(String eljurLogin, String eljurPassword) {
        String requestAuthorizationTokenUrl = generateLink("auth", Map.of("login", eljurLogin, "password", eljurPassword));

        // get data from url
        URL apiRequest;
        String apiResponse;

        try {
            apiRequest = new URL(requestAuthorizationTokenUrl);
            apiResponse = ApiUtils.parseUrl(apiRequest);
        } catch (IOException e) {
            log.warn(e.getMessage());
            return null;
        }

        // deserialize to object
        Authorization.Response response = gson.fromJson(apiResponse, Authorization.Root.class).response;
        if (response.error != null) {
            log.warn(response.error);
            return null;
        }

        return response.tokenInfo;
    }

    // TODO: 20.11.2022
    // можно добавить методы для каждого пункта из официально api: [https://eljur.ru/api/]
    // TODO: 20.11.2022

    // utils
    private static class ApiUtils {
        private static String parseUrl(URL url) throws IOException {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.warn("response code: {}", responseCode);
                return null;
            }

            StringBuilder responseText = new StringBuilder();
            InputStream inputStream = connection.getInputStream();

            byte[] buffer = new byte[1024];
            for (int temp; (temp = inputStream.read(buffer)) != -1; )
                responseText.append(new String(buffer, 0, temp));

            return responseText.toString();
        }
    }

    // classes to parse json
    public static class Marks {
        @FieldDefaults(level = AccessLevel.PRIVATE)
        @Getter
        public static class Root {
            Response response;
        }

        @FieldDefaults(level = AccessLevel.PRIVATE)
        @Getter
        public static class Response {
            int state;
            Object error;
            Result result;
        }

        @FieldDefaults(level = AccessLevel.PRIVATE)
        @Getter
        public static class Result {
            Map<String, Student> students;
            String errorCode;
        }

        @FieldDefaults(level = AccessLevel.PRIVATE)
        @Getter
        public static class Student {
            String name;
            String title;
            List<Lesson> lessons;
        }

        @FieldDefaults(level = AccessLevel.PRIVATE)
        @Getter
        public static class Lesson {
            String average;
            int averageConvert;
            String name;

            public List<Mark> marks;
        }

        @FieldDefaults(level = AccessLevel.PRIVATE)
        @Getter
        public static class Mark {
            String value;

            String weight;
            float weight_float;
            String countas;
            boolean count;

            Object mtype;

            String comment;
            String lesson_comment;
            String date;
            int convert;
        }
    }

    public static class Hometasks {
        // formatted

        @FieldDefaults(level = AccessLevel.PRIVATE)
        @Getter
        public static class Root {
             Response response;
        }

        @FieldDefaults(level = AccessLevel.PRIVATE)
        @Getter
        public static class Response {
             int state;
             Object error;
             Result result;
        }

        @FieldDefaults(level = AccessLevel.PRIVATE)
        @Getter
        public static class Result {
             Map<String, Day> days;
        }

        @FieldDefaults(level = AccessLevel.PRIVATE)
        @Getter
        public static class Day {
             String name;
             String title;
             List<Item> items;
        }

        @FieldDefaults(level = AccessLevel.PRIVATE)
        @Getter
        public static class Item {
             String name;
             boolean individual_exists;
             String grp;

             Map<Integer, Homework> homework;
             Files files;
        }

        @FieldDefaults(level = AccessLevel.PRIVATE)
        @Getter
        public static class Homework {
             int id;
             String value;
        }

        @FieldDefaults(level = AccessLevel.PRIVATE)
        @Getter
        public static class Files {
            public List<File> file;
        }

        @FieldDefaults(level = AccessLevel.PRIVATE)
        @Getter
        public static class File {
            public int toid;
            public String filename;
            public String link;
        }
    }

    public static class Authorization {
        public static class TokenInfo {
            public String token;
            public String expires;

            public String errorCode;
            public String errorText;
        }
        public static class Response {
            public int state;
            public Object error;
            @SerializedName("result")
            public TokenInfo tokenInfo;
        }
        public static class Root {
            public Response response;
        }
    }
}
