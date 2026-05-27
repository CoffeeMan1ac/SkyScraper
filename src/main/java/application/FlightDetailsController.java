package application;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;


public class FlightDetailsController {

    /** Carrier code → friendly airline name. Covers the carriers in the
     *  bundled sample plus the common regional carriers from BTS T-100 exports.
     *  Unknown codes fall back to displaying the code itself. */
    private static final Map<String, String> AIRLINE_NAMES = Map.ofEntries(
            Map.entry("AA", "American Airlines"),
            Map.entry("AS", "Alaska Airlines"),
            Map.entry("B6", "JetBlue Airways"),
            Map.entry("DL", "Delta Air Lines"),
            Map.entry("F9", "Frontier Airlines"),
            Map.entry("G4", "Allegiant Air"),
            Map.entry("HA", "Hawaiian Airlines"),
            Map.entry("NK", "Spirit Airlines"),
            Map.entry("UA", "United Airlines"),
            Map.entry("WN", "Southwest Airlines"),
            Map.entry("OO", "SkyWest Airlines"),
            Map.entry("YX", "Republic Airways"),
            Map.entry("MQ", "Envoy Air"),
            Map.entry("OH", "PSA Airlines"),
            Map.entry("9E", "Endeavor Air"),
            Map.entry("YV", "Mesa Airlines"),
            Map.entry("ZW", "Air Wisconsin"),
            Map.entry("QX", "Horizon Air"));

    private static final DateTimeFormatter SOURCE_DATE = DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US);
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("EEE yyyy-MM-dd", Locale.US);

    @FXML private ImageView airlineLogo;
    @FXML private Label airlineFallback;
    @FXML private Label airlineNameLabel;
    @FXML private Label flightIdLabel;
    @FXML private Label dateLabel;

    @FXML private Label originIataLabel;
    @FXML private Label originCityLabel;
    @FXML private Label destIataLabel;
    @FXML private Label destCityLabel;
    @FXML private Label distanceLabel;

    @FXML private Label depSchedLabel;
    @FXML private Label depActualLabel;
    @FXML private Label depDeltaLabel;
    @FXML private Label arrSchedLabel;
    @FXML private Label arrActualLabel;
    @FXML private Label arrDeltaLabel;

    @FXML private HBox badgeRow;
    @FXML private Label cancelledBadge;
    @FXML private Label divertedBadge;

    @FXML private Label datasetNote;

    private Runnable onClose;

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public void setFlight(Flight flight) {
        // Header
        String airlineName = AIRLINE_NAMES.getOrDefault(flight.mktCarrier, flight.mktCarrier);
        airlineNameLabel.setText(airlineName);
        flightIdLabel.setText(flight.mktCarrier + " " + flight.flightNum);
        dateLabel.setText(formatDate(flight.flDate));
        loadAirlineLogo(flight.mktCarrier);

        // Route
        originIataLabel.setText(blankIfNull(flight.origin));
        originCityLabel.setText(cityState(flight.originCity, flight.originState));
        destIataLabel.setText(blankIfNull(flight.dest));
        destCityLabel.setText(cityState(flight.destCity, flight.destState));
        distanceLabel.setText(formatDistance(flight.distance));

        // Timing
        String depSched = formatTime(flight.crsDepTime);
        String depActual = formatTime(flight.depTime);
        String arrSched = formatTime(flight.crsArrTime);
        String arrActual = formatTime(flight.arrTime);
        depSchedLabel.setText(orDash(depSched));
        depActualLabel.setText(orDash(depActual));
        arrSchedLabel.setText(orDash(arrSched));
        arrActualLabel.setText(orDash(arrActual));
        applyDelta(depDeltaLabel, flight.crsDepTime, flight.depTime);
        applyDelta(arrDeltaLabel, flight.crsArrTime, flight.arrTime);

        // Badges — boolean flags are stored as "0"/"1" (sample dataset) or
        // "0.00"/"1.00" (some older BTS exports). Treat anything starting
        // with "1" as true.
        boolean cancelled = isTrueFlag(flight.cancelled);
        boolean diverted = isTrueFlag(flight.diverted);
        setBadgeVisible(cancelledBadge, cancelled);
        setBadgeVisible(divertedBadge, diverted);
        boolean anyBadge = cancelled || diverted;
        badgeRow.setVisible(anyBadge);
        badgeRow.setManaged(anyBadge);

        // Dataset-quality note (currently only the constant-time-tail signal).
        MemoryLoader.DatasetStats stats = MemoryLoader.getStats();
        boolean showNote = stats.dateHasNoTimeOfDay
                && stats.constantDateTimeTail != null
                && !stats.constantDateTimeTail.isEmpty();
        datasetNote.setText(showNote
                ? "Time-of-day not recorded in this dataset (FL_DATE tail is constant \""
                        + stats.constantDateTimeTail + "\")"
                : "");
        datasetNote.setVisible(showNote);
        datasetNote.setManaged(showNote);
    }

    /** Computes actual − scheduled in minutes and writes it into {@code label}
     *  with green for early/on-time and red for late. Blanks the label when
     *  either side is missing (e.g. cancelled rows). */
    private static void applyDelta(Label label, String sched, String actual) {
        Integer s = parseHHMMToMinutes(sched);
        Integer a = parseHHMMToMinutes(actual);
        if (s == null || a == null) {
            label.setText("—");
            label.setStyle("-fx-text-fill: black;");
            return;
        }
        int diff = a - s;
        // Wrap-around when an early-morning actual chases a late-night scheduled
        // (or vice versa); BTS encodes both in HHMM with no date, so we have to
        // pick the smaller-magnitude interpretation.
        if (diff < -720) diff += 1440;
        else if (diff > 720) diff -= 1440;

        String text;
        String colour;
        if (diff == 0) {
            text = "on time";
            colour = "#0a7a23";
        } else if (diff < 0) {
            text = diff + " min";
            colour = "#0a7a23";
        } else {
            text = "+" + diff + " min";
            colour = "#b00020";
        }
        label.setText(text);
        label.setStyle("-fx-text-fill: " + colour + "; -fx-font-weight: bold;");
    }

    private static String formatDate(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        // BTS dumps "MM/dd/yyyy HH:mm"; strip the time tail before parsing the date.
        int sp = raw.indexOf(' ');
        String datePart = sp >= 0 ? raw.substring(0, sp) : raw;
        String timePart = sp >= 0 ? raw.substring(sp + 1).trim() : "";
        try {
            LocalDate d = LocalDate.parse(datePart, SOURCE_DATE);
            String formatted = d.format(DISPLAY_DATE);
            // If the dataset *does* carry a real time-of-day (not midnight, not
            // constant) keep it visible so we don't silently drop information.
            MemoryLoader.DatasetStats stats = MemoryLoader.getStats();
            boolean meaningful = !timePart.isEmpty()
                    && !"00:00".equals(timePart)
                    && !stats.dateHasNoTimeOfDay;
            return meaningful ? formatted + " " + timePart : formatted;
        } catch (DateTimeParseException e) {
            return raw;
        }
    }

    private static String formatTime(String hhmm) {
        if (hhmm == null || hhmm.isEmpty()) return null;
        try {
            int t = (int) Math.round(Double.parseDouble(hhmm));
            int h = t / 100;
            int m = t % 100;
            if (h < 0 || h > 23 || m < 0 || m > 59) return null;
            return String.format("%02d:%02d", h, m);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseHHMMToMinutes(String hhmm) {
        if (hhmm == null || hhmm.isEmpty()) return null;
        try {
            int t = (int) Math.round(Double.parseDouble(hhmm));
            int h = t / 100;
            int m = t % 100;
            if (h < 0 || h > 23 || m < 0 || m > 59) return null;
            return h * 60 + m;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String formatDistance(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        try {
            int miles = (int) Math.round(Double.parseDouble(raw));
            return String.format(Locale.US, "%,d mi", miles);
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    private static String cityState(String city, String state) {
        String c = blankIfNull(city);
        String s = blankIfNull(state);
        if (c.isEmpty()) return s;
        if (s.isEmpty()) return c;
        // BTS CITY_NAME already arrives as "New York, NY" in the sample, so
        // appending ", NY" again would duplicate. Detect that.
        if (c.endsWith(", " + s)) return c;
        return c + ", " + s;
    }

    private static String blankIfNull(String s) { return s == null ? "" : s; }
    private static String orDash(String s) { return (s == null || s.isEmpty()) ? "—" : s; }

    private static boolean isTrueFlag(String raw) {
        if (raw == null) return false;
        String t = raw.trim();
        return !t.isEmpty() && t.charAt(0) == '1';
    }

    private static void setBadgeVisible(Label badge, boolean show) {
        badge.setVisible(show);
        badge.setManaged(show);
    }

    private void loadAirlineLogo(String airlineCode) {
        String imageUrl = String.format("https://images.kiwi.com/airlines/64/%s.png", airlineCode);

        airlineFallback.setText(airlineCode);
        airlineLogo.setVisible(true);
        airlineFallback.setVisible(false);

        Image image = new Image(imageUrl, true);
        image.errorProperty().addListener((obs, wasError, isError) -> {
            if (Boolean.TRUE.equals(isError)) {
                airlineLogo.setVisible(false);
                airlineFallback.setVisible(true);
            }
        });
        airlineLogo.setImage(image);
    }

    @FXML
    private void closeFlightDetails() {
        if (onClose != null) onClose.run();
    }
}
