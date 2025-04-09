package application;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ResultsController {

    @FXML
    private ListView<String> resultsList;

    public void displayResults(List<Flight> flights) {
        ObservableList<String> flightSummaries = FXCollections.observableArrayList();
        
        for (Flight flight : flights) {
            String summary = String.format("%s | %s%s | %s → %s", 
                                          flight.flDate, flight.mktCarrier, flight.flightNum, 
                                          flight.originCity, flight.destCity);
            flightSummaries.add(summary);
        }
        
        resultsList.setItems(flightSummaries);
    }
}
