package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class FlightResultsController {

	// UI components
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
        flightsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
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

    private final int pageSize = 100;
    private int currentPage = 0;

    // Updates table with current page
    private void updateVisibleFlights() {
        int fromIndex = currentPage * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, allQueryResults.size());

        if (fromIndex >= allQueryResults.size()) {
            visibleFlights.clear();
        } else {
            visibleFlights.setAll(allQueryResults.subList(fromIndex, toIndex));
        }

        flightsTable.setItems(visibleFlights);
    }

    // Next page
    @FXML
    private void handleNextPage() {
        if ((currentPage + 1) * pageSize < allQueryResults.size()) {
            currentPage++;
            updateVisibleFlights();
        }
    }

    // Previous page
    @FXML
    private void handlePreviousPage() {
        if (currentPage > 0) {
            currentPage--;
            updateVisibleFlights();
        }
    }

    // Load queried flights into table
    public void setFlights(List<Flight> filteredFlights) {
        allQueryResults = filteredFlights;
        currentPage = 0;
        updateVisibleFlights();
    }
    
    // Scene switching
    @FXML
    private void switchToMainFromResults(ActionEvent event) throws IOException {
        Parent newRoot = FXMLLoader.load(getClass().getResource("/Main.fxml"));
        ((Node) event.getSource()).getScene().setRoot(newRoot);
    }
    
}
