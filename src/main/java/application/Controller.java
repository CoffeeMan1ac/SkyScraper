package application;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.RangeSlider;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.util.StringConverter;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.util.Duration;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.controlsfx.control.textfield.TextFields;


public class Controller {
	
    private Stage stage;
    private Scene scene;
    private Parent root;

    private String formatFlightSummary(Flight f) {
        return String.format("%s | %s%s | %s → %s", 
            f.flDate, f.mktCarrier, f.flightNum, f.originCity, f.destCity);
    }
    
    // UI components
    @FXML private ComboBox<String> carrierComboBox;
    @FXML private ComboBox<String> originCityComboBox;
    @FXML private ComboBox<String> destinationCityComboBox;
    @FXML private Button timeButton;
    @FXML private CheckBox option1;
    @FXML private CheckBox option2;

    private RangeSlider rangeSlider;
    private RangeSlider rangeSlider2;
    private PopOver timePopOver;
    @FXML ComboBox<String> flightSearchBox;
    @FXML TextField flightSearchField;
    @FXML TextField flightNumberField;
    @FXML private Pane mapContainer;
    @FXML private WebView heatmapWebView;
    @FXML private ToggleButton mapHeatmapToggle;
    @FXML ComboBox<String> cityComboBox;
    @FXML private AnchorPane mainPane;

    private boolean heatmapGenerated = false;
    
    // Clickable city dots on the map
    @FXML
    public void handleCityCircleClick(MouseEvent event) throws IOException {
        String cityName = (String) ((Node) event.getSource()).getUserData();
        cityComboBox.getEditor().setText(cityName);
        switchToGraphsBUTTON();
    }
    
    @FXML
    public void initialize() {
        MemoryLoader.importCSVToMemory();

        Platform.runLater(() -> {
            Stage stage = (Stage) mainPane.getScene().getWindow();
            stage.setTitle("SkyScraper — " + MemoryLoader.getAllFlights().size() + " flights");
        });

        Set<String> uniqueCities = MemoryLoader.getAllFlights().stream()
                .map(f -> f.originCity)
                .collect(Collectors.toSet());

        Set<String> uniqueDestCities = MemoryLoader.getAllFlights().stream()
                .map(f -> f.destCity)
                .collect(Collectors.toSet());

        Set<String> uniqueCarriers = MemoryLoader.getAllFlights().stream()
                .map(f -> f.mktCarrier)
                .collect(Collectors.toSet());
        	
        // Autocomplete flight search field
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

        autoCompletion.setOnAutoCompleted(event -> {
            Flight selectedFlight = event.getCompletion().getFlight();
            showFlightDetails(selectedFlight);
        });
        	
        // Set dropdown values
        ObservableList<String> cityList = FXCollections.observableArrayList(uniqueCities);
        cityComboBox.setItems(cityList);
        cityComboBox.setEditable(true);
        TextFields.bindAutoCompletion(cityComboBox.getEditor(), cityList);

        carrierComboBox.setItems(withBlank(uniqueCarriers));
        originCityComboBox.setItems(withBlank(uniqueCities));
        destinationCityComboBox.setItems(withBlank(uniqueDestCities));

        rangeSlider = buildTimeRangeSlider();
        rangeSlider2 = buildTimeRangeSlider();

        VBox popoverContent = new VBox(10,
                new Label("Departure"), rangeSlider,
                new Label("Arrival"), rangeSlider2);
        popoverContent.setPadding(new Insets(15));
        timePopOver = new PopOver(popoverContent);
        timePopOver.setArrowLocation(PopOver.ArrowLocation.TOP_CENTER);
        timePopOver.setDetachable(false);
        
        // Tooltips for city map dots
        for (Node node : mapContainer.getChildren()) {
            if (node instanceof Circle) {
                Circle circle = (Circle) node;
                String cityName = (String) circle.getUserData();
                Tooltip tooltip = new Tooltip(cityName);
                tooltip.setShowDelay(Duration.ZERO);
                Tooltip.install(circle, tooltip);
            }
        }
    }


    @FXML
    public void handleSearchFlights(ActionEvent event) throws IOException {
        double depLow = rangeSlider.getLowValue();
        double depHigh = rangeSlider.getHighValue();
        double arrLow = rangeSlider2.getLowValue();
        double arrHigh = rangeSlider2.getHighValue();

        String selectedCarrier = carrierComboBox.getValue();
        String selectedOrigin = originCityComboBox.getValue();
        String selectedDest = destinationCityComboBox.getValue();

        boolean showCancelled = option1.isSelected();
        boolean showDiverted = option2.isSelected();

        // Full custom search
        ObservableList<Flight> filteredFlights = FXCollections.observableArrayList(
            MemoryLoader.getAllFlights().stream()
                .filter(f -> {
                	int depMinutes = parseDoubleTime(f.crsDepTime);
                	int arrMinutes = parseDoubleTime(f.crsArrTime);

                    boolean matchDep = depMinutes >= depLow && depMinutes <= depHigh;
                    boolean matchArr = arrMinutes >= arrLow && arrMinutes <= arrHigh;

                    boolean matchCarrier = (selectedCarrier == null || selectedCarrier.isEmpty()) || f.mktCarrier.equals(selectedCarrier);
                    boolean matchOrigin = (selectedOrigin == null || selectedOrigin.isEmpty()) || f.originCity.equals(selectedOrigin);
                    boolean matchDest = (selectedDest == null || selectedDest.isEmpty()) || f.destCity.equals(selectedDest);

                    boolean matchCancelled = !showCancelled || f.cancelled.equals("1.00");
                    boolean matchDiverted = !showDiverted || f.diverted.equals("1.00");

                    return matchDep && matchArr && matchCarrier && matchOrigin && matchDest && matchCancelled && matchDiverted;
                })
                .collect(Collectors.toList())
        );
        
        

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/FlightResults.fxml"));
        Parent resultsRoot = loader.load();

        FlightResultsController resultsController = loader.getController();
        resultsController.setFlights(filteredFlights);

        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(resultsRoot);
        scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
    
    // Convert time string
    private int parseDoubleTime(String timeString) {
        if (timeString == null || timeString.isEmpty()) return -1;
        try {
            double time = Double.parseDouble(timeString);
            int hours = (int) time / 100;
            int minutes = (int) time % 100;
            return hours * 60 + minutes;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    
    // Scene switching
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
    
    public void switchToGraphsBUTTON() throws IOException {
        String selectedCity = cityComboBox.getEditor().getText();
        if (selectedCity == null || selectedCity.isEmpty()) return;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/GraphsScene.fxml"));
        root = loader.load();

        ControllerGraphs controllerGraphs = loader.getController();
        controllerGraphs.displayInput(selectedCity);

        // Correct casting here:
        stage = (Stage) cityComboBox.getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
    
    private static ObservableList<String> withBlank(Set<String> values) {
        ObservableList<String> list = FXCollections.observableArrayList();
        list.add("");
        list.addAll(values);
        return list;
    }

    private RangeSlider buildTimeRangeSlider() {
        RangeSlider s = new RangeSlider(0, 1440, 0, 1440);
        s.setMajorTickUnit(240);
        s.setMinorTickCount(3);
        s.setBlockIncrement(30);
        s.setShowTickLabels(true);
        s.setShowTickMarks(true);
        s.setSnapToTicks(true);
        s.setPrefWidth(300);
        s.setLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number minutes) {
                int m = minutes.intValue();
                return String.format("%02d:%02d", m / 60, m % 60);
            }
            @Override
            public Number fromString(String time) {
                String[] parts = time.split(":");
                return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
            }
        });
        return s;
    }

    @FXML
    public void openTimePopover() {
        timePopOver.show(timeButton);
    }

    @FXML
    public void toggleMapHeatmap() {
        boolean showHeatmap = mapHeatmapToggle.isSelected();
        mapContainer.setVisible(!showHeatmap);
        heatmapWebView.setVisible(showHeatmap);
        mapHeatmapToggle.setText(showHeatmap ? "🔥" : "📍");
        if (showHeatmap && !heatmapGenerated) {
            HeatmapViewer.generate(heatmapWebView);
            heatmapGenerated = true;
        }
    }


    private void showFlightDetails(Flight flight) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FlightDetails.fxml"));
            Parent popupRoot = loader.load();

            FlightDetailsController controller = loader.getController();
            controller.setFlight(flight);

            Stage popupStage = new Stage();
            popupStage.initOwner(mainPane.getScene().getWindow());
            controller.setStage(popupStage);
            popupStage.setTitle("Flight Details");
            popupStage.setScene(new Scene(popupRoot));
            popupStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
        
     
    }

}
