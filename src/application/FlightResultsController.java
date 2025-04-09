package application;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;

public class FlightResultsController {

    @FXML private TableView<Flight> flightsTable;

    @FXML private TableColumn<Flight, String> colDate;
    @FXML private TableColumn<Flight, String> colCarrier;
    @FXML private TableColumn<Flight, String> colFlightNum;
    @FXML private TableColumn<Flight, String> colOrigin;
    @FXML private TableColumn<Flight, String> colDest;
    @FXML private TableColumn<Flight, String> colDepTime;
    @FXML private TableColumn<Flight, String> colArrTime;
    @FXML private TableColumn<Flight, String> colCancelled;
    @FXML private TableColumn<Flight, String> colDiverted;

    public void initialize() {
        colDate.setCellValueFactory(new PropertyValueFactory<>("flDate"));
        colCarrier.setCellValueFactory(new PropertyValueFactory<>("mktCarrier"));
        colFlightNum.setCellValueFactory(new PropertyValueFactory<>("flightNum"));
        colOrigin.setCellValueFactory(new PropertyValueFactory<>("originCity"));
        colDest.setCellValueFactory(new PropertyValueFactory<>("destCity"));
        colDepTime.setCellValueFactory(new PropertyValueFactory<>("crsDepTime"));
        colArrTime.setCellValueFactory(new PropertyValueFactory<>("crsArrTime"));
        colCancelled.setCellValueFactory(new PropertyValueFactory<>("cancelled"));
        colDiverted.setCellValueFactory(new PropertyValueFactory<>("diverted"));
    }
    
    private List<Flight> allQueryResults = new ArrayList<>();
    private final ObservableList<Flight> visibleFlights = FXCollections.observableArrayList();

    private final int pageSize = 100; // or 500, your choice
    private int currentPage = 0;

    private void updateVisibleFlights() {
        int fromIndex = currentPage * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, allQueryResults.size());

        if (fromIndex >= allQueryResults.size()) {
            visibleFlights.clear(); // no data to show
        } else {
            visibleFlights.setAll(allQueryResults.subList(fromIndex, toIndex));
        }

        flightsTable.setItems(visibleFlights);
    }

    
    @FXML
    private void handleNextPage() {
        if ((currentPage + 1) * pageSize < allQueryResults.size()) {
            currentPage++;
            updateVisibleFlights();
        }
    }

    @FXML
    private void handlePreviousPage() {
        if (currentPage > 0) {
            currentPage--;
            updateVisibleFlights();
        }
    }

    
    public void setFlights(List<Flight> filteredFlights) {
        allQueryResults = filteredFlights;
        currentPage = 0;
        updateVisibleFlights();
    }
}
