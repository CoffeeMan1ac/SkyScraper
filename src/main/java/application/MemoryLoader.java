package application;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;
import javafx.application.Platform;
import javafx.concurrent.Task;

public class MemoryLoader {
    private static final String CSV_PATH = "flights_sample.csv";
    public static int queryLimit = 100;

    private static final Preferences PREFS = Preferences.userNodeForPackage(MemoryLoader.class);
    private static final String PREF_RECENT_PREFIX = "recentDataset";
    private static final int MAX_RECENT = 5;

    private static final List<Flight> allFlights = new ArrayList<>();
    private static boolean dataLoaded = false;
    private static File lastSourceFile;
    private static DatasetStats stats = DatasetStats.EMPTY;

    /**
     * One-shot scan over the loaded dataset for "is this field even meaningful?"
     * signals — e.g. BTS exports stamp every FL_DATE with a constant ` 00:00`
     * tail because the source schema reserves room for a time-of-day that's
     * never populated. Computed once per load, read by the details panel to
     * decide whether to hide redundant fields.
     */
    public static final class DatasetStats {
        static final DatasetStats EMPTY = new DatasetStats(false, null, null, null);

        /** True when the time-of-day portion of FL_DATE is identical across every row (so it carries no information). */
        public final boolean dateHasNoTimeOfDay;
        /** The constant time-tail when {@link #dateHasNoTimeOfDay} is true (e.g. "00:00" or ""). */
        public final String constantDateTimeTail;
        /** Earliest FL_DATE seen, or null if none parsed. Used to seed the date-filter UI. */
        public final java.time.LocalDate minDate;
        /** Latest FL_DATE seen, or null if none parsed. */
        public final java.time.LocalDate maxDate;

        DatasetStats(boolean dateHasNoTimeOfDay, String constantDateTimeTail,
                     java.time.LocalDate minDate, java.time.LocalDate maxDate) {
            this.dateHasNoTimeOfDay = dateHasNoTimeOfDay;
            this.constantDateTimeTail = constantDateTimeTail;
            this.minDate = minDate;
            this.maxDate = maxDate;
        }

        static DatasetStats compute(List<Flight> flights) {
            if (flights.isEmpty()) return EMPTY;
            String firstTail = extractDateTail(flights.get(0).flDate);
            boolean tailConstant = true;
            java.time.LocalDate min = null, max = null;
            for (Flight f : flights) {
                String tail = extractDateTail(f.flDate);
                if (tailConstant && !Objects.equals(firstTail, tail)) tailConstant = false;

                java.time.LocalDate d = f.parsedDate();
                if (d != null) {
                    if (min == null || d.isBefore(min)) min = d;
                    if (max == null || d.isAfter(max)) max = d;
                }
            }
            return new DatasetStats(tailConstant, tailConstant ? firstTail : null, min, max);
        }

        private static String extractDateTail(String flDate) {
            if (flDate == null) return null;
            int sp = flDate.indexOf(' ');
            return sp >= 0 ? flDate.substring(sp + 1).trim() : "";
        }
    }

    public static DatasetStats getStats() { return stats; }

    private static ArrayList<String> destArray = new ArrayList<>();
    private static ArrayList<Integer> destCountArray = new ArrayList<>();

    public static void importCSVToMemory() {
    	// Declaration of the instance variables
        if (dataLoaded) {
            System.out.println("Data already loaded into memory.");
            return;
        }

        try (CSVReader reader = new CSVReader(new FileReader(CSV_PATH))) {
            String[] header = reader.readNext(); // Skip header

            int originIndex = -1;
            int destIndex = -1;

            for (int i = 0; i < header.length; i++) {
                if (header[i].equals("ORIGIN_CITY_NAME")) originIndex = i;
                if (header[i].equals("DEST_CITY_NAME")) destIndex = i;
            }

            if (originIndex == -1 || destIndex == -1) {
                throw new RuntimeException("Missing required columns.");
            }

            // Read and store each flight
            String[] row;
            int count = 0;
            while ((row = reader.readNext()) != null) {
                allFlights.add(new Flight(row));
                count++;
            }

            dataLoaded = true;
            lastSourceFile = new File(CSV_PATH);
            stats = DatasetStats.compute(allFlights);
            System.out.println("Loaded " + count + " flights into memory.");

        } catch (Exception e) {
            Logger.getLogger(MemoryLoader.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**
     * Async load of an arbitrary CSV. Parses on a background thread; on success
     * the new flight list is installed on the FX thread so FX-thread readers see
     * a consistent snapshot. Caller wires task.setOnSucceeded / setOnFailed.
     */
    public static Task<Integer> loadAsync(File csvFile) {
        return new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                List<Flight> loaded = new ArrayList<>();
                try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
                    reader.readNext(); // skip header
                    String[] row;
                    while ((row = reader.readNext()) != null) {
                        loaded.add(new Flight(row));
                    }
                }
                Platform.runLater(() -> {
                    allFlights.clear();
                    allFlights.addAll(loaded);
                    destArray.clear();
                    destCountArray.clear();
                    dataLoaded = true;
                    lastSourceFile = csvFile;
                    stats = DatasetStats.compute(allFlights);
                });
                return loaded.size();
            }
        };
    }

    /**
     * Startup entry point. Walks the recent-datasets list (most-recent first),
     * loading the first one that parses; on total miss, falls back to the
     * bundled sample; if that's also unavailable, leaves the app in an
     * empty-but-functional state. Idempotent across scene swaps via {@code dataLoaded}.
     */
    public static void importCSVOnStartup() {
        if (dataLoaded) return;

        for (File f : getRecentDatasets()) {
            try {
                importCSVFromFile(f);
                return;
            } catch (Exception e) {
                System.err.println("Failed to load recent dataset " + f + ": " + e.getMessage());
            }
        }

        importCSVToMemory();
        // Empty-state guarantee: even if the bundled sample was missing, the
        // app proceeds with an empty flight list rather than retrying forever.
        dataLoaded = true;
    }

    /** Synchronous variant of {@link #loadAsync} for startup paths. Throws on parse failure. */
    private static void importCSVFromFile(File csvFile) throws Exception {
        List<Flight> loaded = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            reader.readNext(); // header
            String[] row;
            while ((row = reader.readNext()) != null) {
                loaded.add(new Flight(row));
            }
        }
        allFlights.clear();
        allFlights.addAll(loaded);
        destArray.clear();
        destCountArray.clear();
        dataLoaded = true;
        lastSourceFile = csvFile;
        stats = DatasetStats.compute(allFlights);
        System.out.println("Loaded " + loaded.size() + " flights from " + csvFile.getName());
    }

    /**
     * Returns up to {@link #MAX_RECENT} previously-opened datasets,
     * most-recently-used first. Stale entries (files that no longer exist) are
     * filtered out of the returned list but kept in storage in case the user
     * restores them.
     */
    public static List<File> getRecentDatasets() {
        List<File> out = new ArrayList<>();
        for (int i = 0; i < MAX_RECENT; i++) {
            String p = PREFS.get(PREF_RECENT_PREFIX + i, null);
            if (p == null || p.isEmpty()) continue;
            File f = new File(p);
            if (f.isFile()) out.add(f);
        }
        return out;
    }

    /**
     * Prepends {@code f} to the recent-datasets list, de-duping by absolute path
     * and trimming to {@link #MAX_RECENT}.
     */
    public static void addRecentDataset(File f) {
        if (f == null) return;
        String newPath = f.getAbsolutePath();

        List<String> paths = new ArrayList<>();
        for (int i = 0; i < MAX_RECENT; i++) {
            String p = PREFS.get(PREF_RECENT_PREFIX + i, null);
            if (p != null && !p.isEmpty()) paths.add(p);
        }
        paths.removeIf(p -> p.equals(newPath));
        paths.add(0, newPath);
        if (paths.size() > MAX_RECENT) paths = new ArrayList<>(paths.subList(0, MAX_RECENT));

        for (int i = 0; i < MAX_RECENT; i++) {
            if (i < paths.size()) PREFS.put(PREF_RECENT_PREFIX + i, paths.get(i));
            else PREFS.remove(PREF_RECENT_PREFIX + i);
        }
    }

    public static File getLastSourceFile() {
        return lastSourceFile;
    }
    // Destination counts for each destination
    public static void queryCityDestCounts(String cityName) {
        destArray.clear();
        destCountArray.clear();

        Map<String, Long> grouped = allFlights.stream()
            .filter(f -> f.originCity.equals(cityName))
            .collect(Collectors.groupingBy(f -> f.destCity, Collectors.counting()));

        grouped.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(queryLimit)
            .forEach(entry -> {
                destArray.add(entry.getKey());
                destCountArray.add(entry.getValue().intValue());
            });
    }

    // Getter methods for arraylists and lists
    public static ArrayList<String> getDestList() {
        return destArray;
    }

    public static ArrayList<Integer> getDestCountList() {
        return destCountArray;
    }
    
    public static List<Flight> getAllFlights() {
        return allFlights;
    }
}
