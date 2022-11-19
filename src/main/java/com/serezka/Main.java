package com.serezka;

import com.serezka.eljur.EljurApi;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

@Log4j2
public class Main {
    public static void main(String[] args) throws IOException {
        String eljurDevKey =    "";
        String eljurAuthKey =   "";
        String eljurVendor =    "";

        EljurApi eljurApi = new EljurApi(eljurDevKey, eljurVendor, eljurAuthKey);

        Calendar fromDate = GregorianCalendar.getInstance();
        fromDate.add(Calendar.DAY_OF_MONTH, -14);

        Calendar toDate = GregorianCalendar.getInstance();

        System.out.println(eljurApi.requestMarks(fromDate, toDate).keySet().toString());

        System.out.println(eljurApi.requestMarks(fromDate, toDate)
                .values()
                .iterator().next()
                .values().iterator().next().stream().map(EljurApi.Marks.Mark::getValue).toList());

        System.out.println();

        eljurApi.requestHomework("11Т", fromDate, toDate).values().stream().map(EljurApi.Hometasks.Day::getItems)
                .forEach(items -> {
                    items.stream().map(EljurApi.Hometasks.Item::getHomework).forEach(hw -> {
                       hw.values().stream().map(EljurApi.Hometasks.Homework::getValue).forEach(System.out::println);
                    });
                });

        System.out.println(eljurApi.requestAuthorizationToken("\\login//", "\\pass//").token);



    }
}