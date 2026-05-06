package com.tommustbe12.housing.cookies;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.Locale;

public final class WeekKey {
    private WeekKey() {}

    public static String currentWeekKey() {
        LocalDate now = LocalDate.now(ZoneId.systemDefault());
        WeekFields wf = WeekFields.of(Locale.US);
        int week = now.get(wf.weekOfWeekBasedYear());
        int year = now.get(wf.weekBasedYear());
        return year + "-W" + week;
    }
}
