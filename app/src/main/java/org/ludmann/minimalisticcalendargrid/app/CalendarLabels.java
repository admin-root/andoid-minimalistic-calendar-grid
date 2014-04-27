package org.ludmann.minimalisticcalendargrid.app;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Helper class to get the calendar labels.
 *
 * @author Cornelius Ludmann
 */
public class CalendarLabels {


    private CalendarLabels() {

    }


    public static String[] getMonthNames() {
        Map<String, Integer> monthNamesMap = Calendar.getInstance().getDisplayNames(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
        String[] monthNames = new String[monthNamesMap.size()];
        for (String monthName : monthNamesMap.keySet()) {
            monthNames[monthNamesMap.get(monthName)] = monthName;
        }
        return monthNames;
    }

    public static String[] getWeekdayNames() {
        Map<String, Integer> weekdayNamesMap = Calendar.getInstance().getDisplayNames(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault());
        String[] weekdayNames = new String[weekdayNamesMap.size()];
        for (String weekdayName : weekdayNamesMap.keySet()) {
            weekdayNames[weekdayNamesMap.get(weekdayName) - 1] = weekdayName;
        }
        int firstDay = Calendar.getInstance().getFirstDayOfWeek();
        Collections.rotate(Arrays.asList(weekdayNames), weekdayNames.length - firstDay + 1);
        return weekdayNames;
    }

    /**
     * This methods returns the first day of the month which means the first 'first day of week' (e. g. monday) on the previous month or, if the first day of the month is the 'first day of week', the first day of month.
     *
     * @param month The month number.
     * @param year  The year.
     * @return The first day of the month to display.
     */
    public static Calendar getFirstDayOfMonth(int month, int year) {
        Calendar calendar = Calendar.getInstance();
        // due to the timestamp in the database are UTC:
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.set(year, month, 1, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int firstDayOfWeek = calendar.getFirstDayOfWeek();
        if (dayOfWeek != firstDayOfWeek) {
            int d = firstDayOfWeek - dayOfWeek;
            if (d < 0) {
                calendar.add(Calendar.DAY_OF_MONTH, d);
            } else if (d > 0) {
                calendar.add(Calendar.DAY_OF_MONTH, d - 7);
            }
        }
        //System.out.println("FIST DAY OF MONTH " + (month + 1) + "/" + year + ": " + calendar.get(Calendar.DAY_OF_MONTH) + "." + (calendar.get(Calendar.MONTH) + 1) + "." + calendar.get(Calendar.YEAR));
        return calendar;
    }

    public static Calendar getLastDayOfMonth(int month, int year) {
        Calendar calendar = Calendar.getInstance();
        // due to the timestamp in the database are UTC:
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.set(year, month, 1, 23, 59, 59);
        calendar.set(Calendar.MILLISECOND, calendar.getActualMaximum(Calendar.MILLISECOND));
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int firstDayOfWeek = calendar.getFirstDayOfWeek();

        int d = firstDayOfWeek - dayOfWeek;
        //System.out.println("d: " + d);
        if (d <= 0) {
            calendar.add(Calendar.DAY_OF_MONTH, d + 6);
        } else if (d > 0) {
            calendar.add(Calendar.DAY_OF_MONTH, d - 1);
        }

        //System.out.println("LAST DAY OF MONTH " + (month + 1) + "/" + year + ": " + calendar.get(Calendar.DAY_OF_MONTH) + "." + (calendar.get(Calendar.MONTH) + 1) + "." + calendar.get(Calendar.YEAR));
        return calendar;
    }
}
