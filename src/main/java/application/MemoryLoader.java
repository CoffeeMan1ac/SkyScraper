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
    private static final String PREF_LAST_DATASET = "lastDatasetPath";

    private static final List<Flight> allFlights = new ArrayList<>();
    private static boolean dataLoaded = false;
    private static File lastSourceFile;

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
                });
                return loaded.size();
            }
        };
    }

    /**
     * Startup entry point. Tries the last-opened dataset (from Preferences);
     * on miss or failure, falls back to the bundled sample; if that's also
     * unavailable, leaves the app in an empty-but-functional state. Idempotent
     * across scene swaps via {@code dataLoaded}.
     */
    public static void importCSVOnStartup() {
        if (dataLoaded) return;

        String saved = PREFS.get(PREF_LAST_DATASET, null);
        if (saved != null && !saved.isEmpty()) {
            File f = new File(saved);
            if (f.isFile()) {
                try {
                    importCSVFromFile(f);
                    return;
                } catch (Exception e) {
                    System.err.println("Failed to load saved dataset " + saved + ": " + e.getMessage());
                }
            } else {
                System.err.println("Saved dataset no longer exists: " + saved);
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
        System.out.println("Loaded " + loaded.size() + " flights from " + csvFile.getName());
    }

    public static void saveLastDatasetPath(File f) {
        if (f == null) {
            PREFS.remove(PREF_LAST_DATASET);
        } else {
            PREFS.put(PREF_LAST_DATASET, f.getAbsolutePath());
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
