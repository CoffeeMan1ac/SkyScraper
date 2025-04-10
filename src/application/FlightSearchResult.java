package application;

public class FlightSearchResult {
    private final String summary;
    private final Flight flight;

    public FlightSearchResult(String summary, Flight flight) {
        this.summary = summary;
        this.flight = flight;
    }

    public String getSummary() {
        return summary;
    }

    public Flight getFlight() {
        return flight;
    }

    @Override
    public String toString() {
        return summary; // This is what will be displayed in the dropdown
    }

    
}
