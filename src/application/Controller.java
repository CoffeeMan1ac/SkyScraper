package application;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import org.controlsfx.control.textfield.AutoCompletionBinding;
import javafx.scene.control.ComboBox;
import javafx.collections.transformation.FilteredList;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import java.util.regex.*;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;


import org.controlsfx.control.textfield.AutoCompletionBinding;
import application.FlightSearchResult;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.controlsfx.control.textfield.TextFields;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Controller {

    private Stage stage;
    private Scene scene;
    private Parent root;

    private String formatFlightSummary(Flight f) {
        return String.format("%s | %s%s | %s → %s", 
            f.flDate, f.mktCarrier, f.flightNum, f.originCity, f.destCity);
    }

    
    private static final Pattern FLIGHT_PATTERN = Pattern.compile("^[A-Z]{2}\\d+$");
    
    @FXML
    ComboBox<String> flightSearchBox;
    
    @FXML
    TextField flightSearchField;
    
    @FXML
    Label importInfoLabel;
    
    @FXML
    TextField flightNumberField;
    
    @FXML
    ComboBox<String> cityComboBox;

    public void initialize() {
        MemoryLoader.importCSVToMemory();
        importInfoLabel.setText("Successfully imported " + MemoryLoader.getAllFlights().size() + " flights!");
        Set<String> uniqueCities = MemoryLoader.getAllFlights().stream()
                .map(f -> f.originCity)
                .collect(Collectors.toSet());

        ObservableList<String> cityList = FXCollections.observableArrayList(uniqueCities);
        cityComboBox.setItems(cityList);
        cityComboBox.setEditable(true);

        // ✅ ControlsFX AutoComplete
        TextFields.bindAutoCompletion(cityComboBox.getEditor(), cityList);
        
     // Flight Search (carrier + flight number)
        AutoCompletionBinding<FlightSearchResult> autoCompletion =
        	    TextFields.bindAutoCompletion(flightSearchField, param -> {
        	        String input = param.getUserText().toUpperCase();
        	        if (!input.matches("^[A-Z]{2}\\d+$")) {
        	            return FXCollections.observableArrayList();
        	        }
        	        return MemoryLoader.getAllFlights().stream()
        	                .filter(f -> (f.mktCarrier + f.flightNum).equalsIgnoreCase(input))
        	                .map(f -> new FlightSearchResult(formatFlightSummary(f), f))
        	                .collect(Collectors.toList());
        	    });

        	// When user selects → show details
        	autoCompletion.setOnAutoCompleted(event -> {
        	    FlightSearchResult selected = event.getCompletion();
        	    showFlightDetails(selected.getFlight());
        	});

        	//SQLPart.printAllUniqueOriginAndDestAirportCodes();


    }

    private void showFlightDetailsFromSummary(String summary) {
        try {
            String[] parts = summary.split("\\|")[1].trim().split("(?<=\\D)(?=\\d)"); // Split carrier + number
            String carrier = parts[0];
            String number = parts[1];

            Flight matchedFlight = MemoryLoader.getAllFlights().stream()
                    .filter(f -> f.mktCarrier.equals(carrier) && f.flightNum.equals(number))
                    .findFirst()
                    .orElse(null);

            if (matchedFlight == null) {
                System.out.println("Flight not found.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FlightDetails.fxml"));
            Parent popupRoot = loader.load();

            FlightDetailsController controller = loader.getController();
            controller.setFlight(matchedFlight);

            Stage popupStage = new Stage();
            controller.setStage(popupStage);
            popupStage.setTitle("Flight Details");
            popupStage.setScene(new Scene(popupRoot));
            popupStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
//    public void searchFlight(ActionEvent event) {
//        String flightNum = flightNumberField.getText();
//        if (flightNum == null || flightNum.isEmpty()) {
//            System.out.println("Please enter a flight number.");
//            return;
//        }
//
//        Flight matchedFlight = SQLPart.getAllFlights().stream()
//                .filter(f -> f.flightNum.equals(flightNum))
//                .findFirst()
//                .orElse(null);
//
//        if (matchedFlight == null) {
//            System.out.println("Flight not found.");
//            return;
//        }
//
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FlightDetails.fxml"));
//            Parent popupRoot = loader.load();
//
//            FlightDetailsController controller = loader.getController();
//            controller.setFlight(matchedFlight);  // 🟢 This is where data is passed
//
//            Stage popupStage = new Stage();
//            controller.setStage(popupStage);
//            popupStage.setTitle("Flight Details");
//            popupStage.setScene(new Scene(popupRoot));
//            popupStage.show();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }



    
    public void switchToMain(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("/Main.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    public void switchToGraphs(ActionEvent event) throws IOException {
        String selectedCity = cityComboBox.getEditor().getText();
        if (selectedCity == null || selectedCity.isEmpty()) return;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/GraphsScene.fxml"));
        root = loader.load();

        ControllerGraphs controllerGraphs = loader.getController();
        controllerGraphs.displayInput(selectedCity);

        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
    private void showFlightDetails(Flight flight) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FlightDetails.fxml"));
            Parent popupRoot = loader.load();

            FlightDetailsController controller = loader.getController();
            controller.setFlight(flight);

            Stage popupStage = new Stage();
            controller.setStage(popupStage);
            popupStage.setTitle("Flight Details");
            popupStage.setScene(new Scene(popupRoot));
            popupStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
