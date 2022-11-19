package com.serezka.eljur;

import com.google.gson.Gson;
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
     * @param fromDate - | between witch dates return marks
     * @param toDate -   |
     * @return marks for selected dates
     * @throws IOException - when connect to server may throw error
     */
    public Map<String, Map<MarksRequest.Lesson, List<MarksRequest.Mark>>> requestMarks(Calendar fromDate, Calendar toDate) throws IOException {
        // generate link
        String requestMarksUrl = generateLink(
                "getmarks",
                Map.of("days", API_DATE_FORMAT.format(fromDate.getTime()) + "-" + API_DATE_FORMAT.format(toDate.getTime())));

        // parse data
        Gson gson = new Gson();

        // get data from url
        URL url = new URL(requestMarksUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            log.warn("response code: {}", responseCode);
            return Collections.emptyMap();
        }

        // read data
        StringBuilder responseText = new StringBuilder();
        InputStream inputStream = connection.getInputStream();

        byte[] buffer = new byte[1024];
        for (int temp; (temp = inputStream.read(buffer)) != -1; )
            responseText.append(new String(buffer, 0, temp));

        // deserialize to object
        MarksRequest.Response response = gson.fromJson(responseText.toString(), MarksRequest.Root.class).response;
        if (response.error != null) {
            log.warn("response error: {}", response.error);
            return Collections.emptyMap();
        }

        // format
        Map<String, Map<MarksRequest.Lesson, List<MarksRequest.Mark>>> marks = new HashMap<>();
        response.result.students.forEach((s, student) -> {
            Map<MarksRequest.Lesson, List<MarksRequest.Mark>> tempStudentMarks = new HashMap<>();
            student.lessons.forEach(lesson -> tempStudentMarks.put(lesson, lesson.marks));
            marks.put(student.name, tempStudentMarks);
        });

        return marks;
    }

    public static class MarksRequest {
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

    // get homework request
    public Object requestHomework() {



        return null; // todo
    }
}
