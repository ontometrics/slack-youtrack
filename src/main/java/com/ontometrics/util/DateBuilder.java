package com.ontometrics.util;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Provides a fluent builder interface for constructing Dates and DateTimes.
 */
public class DateBuilder {

    private Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    /**
     * Provides means of starting from a given date, then changing some subset
     * of the fields
     *
     * @param date
     *            the starting point date
     * @return this, for chaining
     */
    public DateBuilder start(Date date) {
        calendar.setTime(date);
        return this;
    }

    /**
     * So to build the first of the year, you would set this to 1.
     *
     * @param dayOfMonth
     *            a number between 1 and 31 (depending on the calendar month of
     *            course)
     * @return this, for chaining
     */
    public DateBuilder day(int dayOfMonth) {
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        return this;
    }

    /**
     * Provide the month of the date you are building, a number between 1 and
     * 12.
     *
     * @param month
     *            the value desired in the resulting date, e.g. 5 for May
     * @return this, for chaining
     */
    public DateBuilder month(int month) {
        calendar.set(Calendar.MONTH, month);
        return this;
    }

    /**
     * Provide the year of the date you are building, e.g. 1980.
     *
     * @param year
     *            the value desired in the resulting date
     * @return this, for chaining
     */
    public DateBuilder year(int year) {
        calendar.set(Calendar.YEAR, year);
        return this;
    }

    /**
     * Provide the hour, in this case of the day. Note the JDK calls this hour
     * of the day.
     *
     * @param hour
     *            the value desired in the resulting date, e.g. 14 for 2 pm
     * @return this, for chaining
     */
    public DateBuilder hour(int hour) {
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        return this;
    }

    /**
     * Provide the minutes within the hour of the datetime being constructed
     *
     * @param minutes
     *            a number between 0 and 59, e.g. if building 12:49, this would
     *            be 49.
     * @return this, for chaining
     */
    public DateBuilder minutes(int minutes) {
        calendar.set(Calendar.MINUTE, minutes);
        return this;
    }

    /**
     * Provide the seconds within the hour of the datetime being constructed
     *
     * @param seconds
     *            a number between 0 and 59
     * @return this, for chaining
     */
    public DateBuilder seconds(int seconds) {
        calendar.set(Calendar.SECOND, seconds);
        return this;
    }

    /**
     * Adds specified amount of minutes to the built date
     * @param minutes minutes
     *
     * @return this, for chaining
     */
    public DateBuilder addMinutes(int minutes) {
        calendar.add(Calendar.MINUTE, minutes);
        return this;
    }


    /**
     * Sets specified timestamp
     * @param time timestamp
     *
     * @return this, for chaining
     */
    public DateBuilder time(Date time) {
        calendar.setTime(time);
        return this;
    }

    /**
     * Provides access to the final product.
     *
     * @return the constructed date with all the desired values
     */
    public Date build() {
        return calendar.getTime();
    }

    public Calendar buildCalendar() {
        return calendar;
    }
}
