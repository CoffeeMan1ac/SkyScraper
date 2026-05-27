package application;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

// Class which represents a single flight
public class Flight {

    /** BTS exports use M/D/YYYY for the date portion of FL_DATE,
     *  optionally followed by a time-of-day tail we ignore. */
    private static final DateTimeFormatter FL_DATE_FMT =
            DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US);

    public String flDate;
    public String mktCarrier;
    public String flightNum;
    public String origin;
    public String originCity;
    public String originState;
    public String originWac;
    public String dest;
    public String destCity;
    public String destState;
    public String destWac;
    public String crsDepTime;
    public String depTime;
    public String crsArrTime;
    public String arrTime;
    public String cancelled;
    public String diverted;
    public String distance;

    // Constructor, which assigns values from the String array to the instance variables
    public Flight(String[] data) {
        this.flDate = data[0];
        this.mktCarrier = data[1];
        this.flightNum = data[2];
        this.origin = data[3];
        this.originCity = data[4];
        this.originState = data[5];
        this.originWac = data[6];
        this.dest = data[7];
        this.destCity = data[8];
        this.destState = data[9];
        this.destWac = data[10];
        this.crsDepTime = data[11];
        this.depTime = data[12];
        this.crsArrTime = data[13];
        this.arrTime = data[14];
        this.cancelled = data[15];
        this.diverted = data[16];
        this.distance = data[17];
    }
    
    // Getter methods for instance variables
    public String getFlDate() { return flDate; }
    public String getMktCarrier() { return mktCarrier; }
    public String getFlightNum() { return flightNum; }
    public String getOriginCity() { return originCity; }
    public String getDestCity() { return destCity; }
    public String getCrsDepTime() { return crsDepTime; }
    public String getCrsArrTime() { return crsArrTime; }
    public String getCancelled() { return cancelled; }
    public String getDiverted() { return diverted; }

    public boolean isCancelled() { return flagIsTrue(cancelled); }
    public boolean isDiverted() { return flagIsTrue(diverted); }

    /** Returns the date portion of FL_DATE as a {@link LocalDate}, or null when
     *  the field is missing or unparseable. Strips any "HH:mm" tail. */
    public LocalDate parsedDate() {
        if (flDate == null || flDate.isEmpty()) return null;
        int sp = flDate.indexOf(' ');
        String datePart = sp >= 0 ? flDate.substring(0, sp) : flDate;
        try {
            return LocalDate.parse(datePart, FL_DATE_FMT);
        } catch (Exception e) {
            return null;
        }
    }

    /** BTS-style boolean flags arrive as "1"/"0" in some exports and
     *  "1.00"/"0.00" in others. Parse as a number so both work. */
    private static boolean flagIsTrue(String s) {
        if (s == null || s.isEmpty()) return false;
        try {
            return Double.parseDouble(s.trim()) >= 0.5;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
