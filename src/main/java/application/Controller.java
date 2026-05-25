package application;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import com.opencsv.CSVReader;
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
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
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
    @FXML private javafx.scene.layout.StackPane mapArea;
    @FXML private Pane mapContainer;
    @FXML private TextField dotSearchField;
    @FXML private javafx.scene.layout.HBox dotSearchRow;
    @FXML private ToggleButton mapHeatmapToggle;
    @FXML private Label mapDescriptionLabel;
    @FXML private javafx.scene.layout.HBox heatmapLegend;
    @FXML private javafx.scene.shape.Rectangle legendBar;
    @FXML private Node flightDetails;
    @FXML private FlightDetailsController flightDetailsController;
    @FXML ComboBox<String> cityComboBox;
    @FXML private AnchorPane mainPane;

    // State for the dot-search feature
    private final java.util.List<Airport> airports = new java.util.ArrayList<>();
    private final java.util.List<Label> searchLabels = new java.util.ArrayList<>();
    private final java.util.Map<String, SVGPath> stateNodes = new java.util.LinkedHashMap<>();
    private javafx.scene.Group mapContent;
    private final javafx.scene.transform.Scale mapScale = new javafx.scene.transform.Scale(1, 1, 0, 0);
    private final javafx.scene.transform.Translate mapTranslate = new javafx.scene.transform.Translate(0, 0);
    private javafx.animation.Timeline zoomAnim;
    private double canvasW = 763, canvasH = 449;

    private static final class Airport {
        final String iata, city, state;
        final double x, y;
        final Circle circle;
        Airport(String iata, String city, String state, double x, double y, Circle circle) {
            this.iata = iata;
            this.city = city;
            this.state = state;
            this.x = x;
            this.y = y;
            this.circle = circle;
        }
    }
    
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
        	
        // Autocomplete flight search field. The converter controls what lands
        // in the text field after a pick; FlightSearchResult.toString() controls
        // what shows in the dropdown row — so we get just "AA5" in the field
        // and the full summary in the list.
        StringConverter<FlightSearchResult> flightToFieldText = new StringConverter<>() {
            @Override
            public String toString(FlightSearchResult fsr) {
                if (fsr == null) return "";
                Flight f = fsr.getFlight();
                return f.mktCarrier + f.flightNum;
            }
            @Override
            public FlightSearchResult fromString(String s) { return null; }
        };
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
            }, flightToFieldText);

        autoCompletion.setOnAutoCompleted(event -> {
            Flight selectedFlight = event.getCompletion().getFlight();
            showFlightDetails(selectedFlight);
        });
        autoCompletion.setVisibleRowCount(15);

        flightDetailsController.setOnClose(this::hideFlightDetails);
        	
        // Set dropdown values
        ObservableList<String> cityList = FXCollections.observableArrayList(uniqueCities);
        cityComboBox.setItems(cityList);
        cityComboBox.setEditable(true);
        TextFields.bindAutoCompletion(cityComboBox.getEditor(), cityList);

        carrierComboBox.setItems(withAny("Any carrier", uniqueCarriers));
        carrierComboBox.setValue("Any carrier");
        originCityComboBox.setItems(withAny("Any origin", uniqueCities));
        originCityComboBox.setValue("Any origin");
        destinationCityComboBox.setItems(withAny("Any destination", uniqueDestCities));
        destinationCityComboBox.setValue("Any destination");

        rangeSlider = buildTimeRangeSlider();
        rangeSlider2 = buildTimeRangeSlider();

        Button resetTimeButton = new Button("Reset");
        resetTimeButton.setOnAction(e -> {
            rangeSlider.setLowValue(0);
            rangeSlider.setHighValue(1440);
            rangeSlider2.setLowValue(0);
            rangeSlider2.setHighValue(1440);
            updateTimeButtonLabel();
        });

        VBox popoverContent = new VBox(10,
                new Label("Departure"), rangeSlider,
                new Label("Arrival"), rangeSlider2,
                resetTimeButton);
        popoverContent.setPadding(new Insets(15));
        timePopOver = new PopOver(popoverContent);
        timePopOver.setArrowLocation(PopOver.ArrowLocation.TOP_CENTER);
        timePopOver.setDetachable(false);

        javafx.beans.value.ChangeListener<Boolean> onDragEnd =
                (obs, wasChanging, isChanging) -> { if (!isChanging) updateTimeButtonLabel(); };
        rangeSlider.lowValueChangingProperty().addListener(onDragEnd);
        rangeSlider.highValueChangingProperty().addListener(onDragEnd);
        rangeSlider2.lowValueChangingProperty().addListener(onDragEnd);
        rangeSlider2.highValueChangingProperty().addListener(onDragEnd);
        
        buildMap();
        dotSearchField.textProperty().addListener((obs, oldV, newV) -> applyDotSearch(newV));

        // Heatmap legend gradient — matches heatmapColor()'s green→yellow→red ramp.
        legendBar.setFill(new javafx.scene.paint.LinearGradient(
                0, 0, 1, 0, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0,   javafx.scene.paint.Color.hsb(120, 0.6, 0.85)),
                new javafx.scene.paint.Stop(0.5, javafx.scene.paint.Color.hsb(60,  0.6, 0.85)),
                new javafx.scene.paint.Stop(1,   javafx.scene.paint.Color.hsb(0,   0.6, 0.85))));

        // Hide the airport-search row and the map description whenever the
        // flight-details panel is showing.
        dotSearchRow.visibleProperty().bind(flightDetails.visibleProperty().not());
        dotSearchRow.managedProperty().bind(dotSearchRow.visibleProperty());
        mapDescriptionLabel.visibleProperty().bind(flightDetails.visibleProperty().not());
        mapDescriptionLabel.managedProperty().bind(mapDescriptionLabel.visibleProperty());
    }

    private void buildMap() {
        mapContainer.getChildren().clear();
        airports.clear();
        searchLabels.clear();

        // Airports that appear in the currently loaded flight data.
        java.util.Set<String> activeIatas = new java.util.HashSet<>();
        for (Flight f : MemoryLoader.getAllFlights()) {
            if (f.origin != null) activeIatas.add(f.origin);
            if (f.dest != null) activeIatas.add(f.dest);
        }

        // Canvas grows with dot count above a threshold, capped at ~1.44× the base.
        double scale = 1.0;
        if (activeIatas.size() > 150) {
            scale = Math.min(1.44, Math.sqrt(activeIatas.size() / 150.0));
        }
        canvasW = Math.round(763.0 * scale);
        canvasH = Math.round(449.0 * scale);
        setMapAreaSize(canvasW, canvasH);
        mapContainer.setClip(new javafx.scene.shape.Rectangle(canvasW, canvasH));

        // Map content (states + dots) lives inside a Group so we can transform
        // for the search-zoom; labels go directly on mapContainer so their
        // text size doesn't scale with the zoom.
        mapContent = new javafx.scene.Group();
        // Order: list-last is innermost — scale first, then translate.
        mapContent.getTransforms().setAll(mapTranslate, mapScale);
        mapScale.setX(1);
        mapScale.setY(1);
        mapTranslate.setX(0);
        mapTranslate.setY(0);
        mapContainer.getChildren().add(mapContent);

        AlbersUsa projection = new AlbersUsa(canvasW, canvasH);

        stateNodes.clear();
        try (InputStream is = getClass().getResourceAsStream("/us-states-10m.json")) {
            if (is == null) throw new IOException("Missing resource: /us-states-10m.json");
            java.util.Map<String, String> statePaths = TopoJsonStates.toStatePaths(is, projection);
            for (java.util.Map.Entry<String, String> e : statePaths.entrySet()) {
                SVGPath sp = new SVGPath();
                sp.setContent(e.getValue());
                sp.setFill(Color.web("#f5f1e6"));
                sp.setStroke(Color.web("#888888"));
                sp.setStrokeWidth(0.6);
                mapContent.getChildren().add(sp);
                stateNodes.put(e.getKey(), sp);
            }
        } catch (IOException e) {
            System.err.println("Failed to load state outlines: " + e.getMessage());
        }

        InputStream csv = getClass().getResourceAsStream("/us-airports.csv");
        if (csv == null) {
            System.err.println("Missing resource: /us-airports.csv");
            return;
        }
        try (CSVReader reader = new CSVReader(new InputStreamReader(csv, StandardCharsets.UTF_8))) {
            reader.readNext(); // header
            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length < 5) continue;
                String iata = row[0];
                if (!activeIatas.contains(iata)) continue;

                double lat, lng;
                try {
                    lat = Double.parseDouble(row[1]);
                    lng = Double.parseDouble(row[2]);
                } catch (NumberFormatException ex) {
                    continue;
                }
                String city = row[3];
                String state = row[4].startsWith("US-") ? row[4].substring(3) : row[4];

                double[] xy = projection.project(lng, lat);
                if (xy == null) continue;

                Circle c = new Circle(xy[0], xy[1], 5);
                c.setFill(Color.RED);
                c.setStroke(Color.BLACK);
                c.setStrokeWidth(0.6);
                c.setUserData(city + ", " + state);
                c.setOnMouseClicked(this::handleCityCircleClickSafe);
                Tooltip tip = new Tooltip(iata + " — " + city + ", " + state);
                tip.setShowDelay(Duration.millis(60));
                Tooltip.install(c, tip);
                mapContent.getChildren().add(c);
                airports.add(new Airport(iata, city, state, xy[0], xy[1], c));
            }
            System.out.println("Map: " + airports.size() + " airport dots from " + activeIatas.size() + " active IATAs.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Filters map dots by IATA / city / state prefix; greens matches, zooms to their bbox, shows labels. */
    private void applyDotSearch(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase();

        // Clear previous overlay labels first.
        mapContainer.getChildren().removeAll(searchLabels);
        searchLabels.clear();

        if (query.isEmpty()) {
            for (Airport a : airports) a.circle.setFill(Color.RED);
            animateZoom(1, 0, 0);
            return;
        }

        java.util.List<Airport> matched = new java.util.ArrayList<>();
        for (Airport a : airports) {
            boolean m = a.iata.toLowerCase().startsWith(query)
                    || a.city.toLowerCase().startsWith(query)
                    || a.state.toLowerCase().startsWith(query);
            a.circle.setFill(m ? Color.LIMEGREEN : Color.RED);
            if (m) matched.add(a);
        }

        if (matched.isEmpty()) {
            animateZoom(1, 0, 0);
            return;
        }

        // Bbox of matched in canvas coords.
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (Airport a : matched) {
            if (a.x < minX) minX = a.x;
            if (a.y < minY) minY = a.y;
            if (a.x > maxX) maxX = a.x;
            if (a.y > maxY) maxY = a.y;
        }
        // Pad single-point bboxes so the zoom stays sane.
        double bboxW = Math.max(40, maxX - minX);
        double bboxH = Math.max(40, maxY - minY);
        double margin = 0.85;
        double s = Math.min(6.0, Math.min(canvasW * margin / bboxW, canvasH * margin / bboxH));
        double bboxCX = (minX + maxX) / 2;
        double bboxCY = (minY + maxY) / 2;
        double tx = canvasW / 2 - s * bboxCX;
        double ty = canvasH / 2 - s * bboxCY;

        animateZoom(s, tx, ty);

        // Labels above matched dots — cap so we don't drown the canvas in text.
        int LABEL_CAP = 25;
        if (matched.size() <= LABEL_CAP) {
            for (Airport a : matched) {
                Label lab = new Label(a.iata);
                lab.setStyle("-fx-background-color: rgba(255,255,255,0.9); "
                        + "-fx-padding: 1 4 1 4; -fx-font-size: 10; -fx-text-fill: black;");
                lab.setMouseTransparent(true);
                lab.setLayoutX(s * a.x + tx + 6);
                lab.setLayoutY(s * a.y + ty - 16);
                searchLabels.add(lab);
                mapContainer.getChildren().add(lab);
            }
        }
    }

    private void setMapAreaSize(double w, double h) {
        mapArea.setMinSize(w, h);
        mapArea.setPrefSize(w, h);
        mapArea.setMaxSize(w, h);
        mapContainer.setMinSize(w, h);
        mapContainer.setPrefSize(w, h);
        mapContainer.setMaxSize(w, h);
        if (flightDetails instanceof javafx.scene.layout.Region) {
            javafx.scene.layout.Region r = (javafx.scene.layout.Region) flightDetails;
            r.setMinSize(w, h);
            r.setPrefSize(w, h);
            r.setMaxSize(w, h);
        }
    }

    private void animateZoom(double targetScale, double targetTx, double targetTy) {
        if (zoomAnim != null) zoomAnim.stop();
        zoomAnim = new javafx.animation.Timeline(new javafx.animation.KeyFrame(
                Duration.millis(180),
                new javafx.animation.KeyValue(mapScale.xProperty(), targetScale, javafx.animation.Interpolator.EASE_OUT),
                new javafx.animation.KeyValue(mapScale.yProperty(), targetScale, javafx.animation.Interpolator.EASE_OUT),
                new javafx.animation.KeyValue(mapTranslate.xProperty(), targetTx, javafx.animation.Interpolator.EASE_OUT),
                new javafx.animation.KeyValue(mapTranslate.yProperty(), targetTy, javafx.animation.Interpolator.EASE_OUT)));
        zoomAnim.play();
    }

    private void handleCityCircleClickSafe(MouseEvent event) {
        try {
            handleCityCircleClick(event);
        } catch (IOException e) {
            e.printStackTrace();
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

                    boolean matchCarrier = isAny(selectedCarrier) || f.mktCarrier.equals(selectedCarrier);
                    boolean matchOrigin = isAny(selectedOrigin) || f.originCity.equals(selectedOrigin);
                    boolean matchDest = isAny(selectedDest) || f.destCity.equals(selectedDest);

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

        ((Node) event.getSource()).getScene().setRoot(resultsRoot);
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
        Parent newRoot = FXMLLoader.load(getClass().getResource("/Main.fxml"));
        ((Node) event.getSource()).getScene().setRoot(newRoot);
    }

    public void switchToGraphs(ActionEvent event) throws IOException {
        String selectedCity = cityComboBox.getEditor().getText();
        if (selectedCity == null || selectedCity.isEmpty()) return;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/GraphsScene.fxml"));
        Parent newRoot = loader.load();

        ControllerGraphs controllerGraphs = loader.getController();
        controllerGraphs.displayInput(selectedCity);

        ((Node) event.getSource()).getScene().setRoot(newRoot);
    }

    public void switchToGraphsBUTTON() throws IOException {
        String selectedCity = cityComboBox.getEditor().getText();
        if (selectedCity == null || selectedCity.isEmpty()) return;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/GraphsScene.fxml"));
        Parent newRoot = loader.load();

        ControllerGraphs controllerGraphs = loader.getController();
        controllerGraphs.displayInput(selectedCity);

        cityComboBox.getScene().setRoot(newRoot);
    }
    
    private static ObservableList<String> withAny(String anyLabel, Set<String> values) {
        ObservableList<String> list = FXCollections.observableArrayList();
        list.add(anyLabel);
        list.addAll(values);
        return list;
    }

    private static boolean isAny(String value) {
        return value == null || value.isEmpty() || value.startsWith("Any ");
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

    private void updateTimeButtonLabel() {
        boolean depChanged = rangeSlider.getLowValue() > 0 || rangeSlider.getHighValue() < 1440;
        boolean arrChanged = rangeSlider2.getLowValue() > 0 || rangeSlider2.getHighValue() < 1440;
        if (!depChanged && !arrChanged) {
            timeButton.setText("Time");
        } else {
            timeButton.setText("D " + formatHours(rangeSlider) + " A " + formatHours(rangeSlider2));
        }
    }

    private static String formatHours(RangeSlider s) {
        int low = (int) s.getLowValue() / 60;
        int high = (int) s.getHighValue() / 60;
        return String.format("%02d–%02d", low, high);
    }

    @FXML
    public void toggleMapHeatmap() {
        boolean showHeatmap = mapHeatmapToggle.isSelected();
        flightDetails.setVisible(false);
        mapHeatmapToggle.setText(showHeatmap ? "Map" : "Heatmap");
        mapDescriptionLabel.setText(showHeatmap
                ? "Flight density by origin state"
                : "Click an airport to view its destinations");
        heatmapLegend.setVisible(showHeatmap);
        if (showHeatmap) {
            applyHeatmapColors();
        } else {
            resetStateColors();
        }
    }

    private void applyHeatmapColors() {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (Flight f : MemoryLoader.getAllFlights()) {
            if (f.originState == null) continue;
            String s = f.originState.trim().replace("\"", "");
            if (s.isEmpty()) continue;
            counts.merge(s, 1, Integer::sum);
        }
        if (counts.isEmpty()) return;
        int min = counts.values().stream().min(Integer::compareTo).orElse(0);
        int max = counts.values().stream().max(Integer::compareTo).orElse(0);
        for (java.util.Map.Entry<String, SVGPath> entry : stateNodes.entrySet()) {
            Integer count = counts.get(entry.getKey());
            entry.getValue().setFill(count == null
                    ? Color.web("#d6d0c4")
                    : heatmapColor(count, min, max));
        }
    }

    private void resetStateColors() {
        for (SVGPath sp : stateNodes.values()) {
            sp.setFill(Color.web("#f5f1e6"));
        }
    }

    private static Color heatmapColor(int count, int min, int max) {
        if (max == min) return Color.web("#d6d0c4");
        double t = (double) (count - min) / (max - min);
        // Warm green → yellow → red via HSB: hue 120° (green) down to 0° (red).
        double hue = 120.0 * (1.0 - t);
        return Color.hsb(hue, 0.6, 0.85);
    }


    private void showFlightDetails(Flight flight) {
        flightDetailsController.setFlight(flight);
        flightDetails.setVisible(true);
    }

    private void hideFlightDetails() {
        flightDetails.setVisible(false);
    }

}
