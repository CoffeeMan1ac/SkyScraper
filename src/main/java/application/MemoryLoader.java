package application;

import java.io.FileReader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;

public class MemoryLoader {
    private static final String CSV_PATH = "flights_sample.csv";
    public static int queryLimit = 100;

    private static final List<Flight> allFlights = new ArrayList<>();
    private static boolean dataLoaded = false;

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
            System.out.println("Loaded " + count + " flights into memory.");

        } catch (Exception e) {
            Logger.getLogger(MemoryLoader.class.getName()).log(Level.SEVERE, null, e);
        }
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
