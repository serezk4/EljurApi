package com.serezka;

import com.serezka.eljur.EljurApi;
import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

@Log4j2
public class Main {
    public static void main(String[] args) throws IOException {
        String eljurDevKey =    "";
        String eljurAuthKey =   "";
        String eljurVendor =    "";

        EljurApi eljurApi = new EljurApi(eljurDevKey, eljurVendor, eljurAuthKey);

        Calendar fromDate = GregorianCalendar.getInstance();
        fromDate.add(Calendar.DAY_OF_MONTH, -7);

        Calendar toDate = GregorianCalendar.getInstance();

        System.out.println(eljurApi.requestMarks(fromDate, toDate)
                .values()
                .iterator().next()
                .values().iterator().next()
                .get(0).getMtype());



    }
}