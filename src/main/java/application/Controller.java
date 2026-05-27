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
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitMenuButton;
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
import javafx.scene.control.DatePicker;
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
    @FXML private Button dateButton;
    @FXML private Button timeButton;
    @FXML private CheckBox option1;
    @FXML private CheckBox option2;
    @FXML private SplitMenuButton openDatasetButton;

    private RangeSlider rangeSlider;
    private RangeSlider rangeSlider2;
    private PopOver timePopOver;
    private DatePicker datePickerFrom;
    private DatePicker datePickerTo;
    private PopOver datePopOver;
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
    private volatile boolean ignoreFlightFilterOnce = false;

    /** Persists the heatmap-toggle state across Controller instances, so the
     *  toggle stays on after a scene switch and back. */
    private static boolean heatmapWasOn = false;
    private final javafx.scene.transform.Scale mapScale = new javafx.scene.transform.Scale(1, 1, 0, 0);
    private final javafx.scene.transform.Translate mapTranslate = new javafx.scene.transform.Translate(0, 0);
    private javafx.animation.Timeline zoomAnim;
    private double canvasW = 763, canvasH = 449;

    private static final class Airport {
        final String iata, city, state;
        final double anchorX, anchorY;   // where the airport actually is
        double x, y;                     // current (post-relaxation) position
        final Circle circle;
        Airport(String iata, String city, String state, double x, double y, Circle circle) {
            this.iata = iata;
            this.city = city;
            this.state = state;
            this.anchorX = x;
            this.anchorY = y;
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
        MemoryLoader.importCSVOnStartup();

        Platform.runLater(() -> {
            Stage stage = (Stage) mainPane.getScene().getWindow();
            int n = MemoryLoader.getAllFlights().size();
            java.io.File src = MemoryLoader.getLastSourceFile();
            String suffix = (src != null) ? " · " + src.getName() : "";
            stage.setTitle("SkyScraper — " + n + " flights" + suffix);
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
        	
        // Flight # search uses a hand-rolled Popup + ListView so we can
        // size the dropdown to the search bar — ControlsFX's autocomplete
        // popup is inside a non-exported impl package and its internal Scene
        // doesn't see application.css, so neither programmatic nor CSS
        // approaches reach it on modular Java.
        setupFlightAutocomplete();

        // FlightDetails.fxml no longer carries visible="false" (so it can also
        // be loaded standalone for the results-row drill-in). Hide it here for
        // the embedded Main path; showFlightDetails flips it on.
        flightDetails.setVisible(false);
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

        // Date filter — popover with from/to DatePickers. Prompt text seeded
        // from the dataset's min/max so the user sees the available span;
        // dayCellFactory disables out-of-range cells (standard JavaFX idiom).
        datePickerFrom = new DatePicker();
        datePickerTo = new DatePicker();
        MemoryLoader.DatasetStats dStats = MemoryLoader.getStats();
        if (dStats.minDate != null) datePickerFrom.setPromptText(dStats.minDate.toString());
        if (dStats.maxDate != null) datePickerTo.setPromptText(dStats.maxDate.toString());
        if (dStats.minDate != null || dStats.maxDate != null) {
            javafx.util.Callback<DatePicker, javafx.scene.control.DateCell> cellFactory = picker ->
                    new javafx.scene.control.DateCell() {
                        @Override
                        public void updateItem(java.time.LocalDate item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item == null || empty) return;
                            boolean tooEarly = dStats.minDate != null && item.isBefore(dStats.minDate);
                            boolean tooLate = dStats.maxDate != null && item.isAfter(dStats.maxDate);
                            if (tooEarly || tooLate) {
                                setDisable(true);
                                setStyle("-fx-background-color: #e0e0e0;");
                            }
                        }
                    };
            datePickerFrom.setDayCellFactory(cellFactory);
            datePickerTo.setDayCellFactory(cellFactory);
        }

        // Open the picker on the dataset's range, not on "today" — saves the
        // user from paging back years through months they can't pick anyway.
        // Equal-to-range counts as "no filter" downstream.
        datePickerFrom.setValue(dStats.minDate);
        datePickerTo.setValue(dStats.maxDate);

        Button resetDateButton = new Button("Reset");
        resetDateButton.setOnAction(e -> {
            datePickerFrom.setValue(dStats.minDate);
            datePickerTo.setValue(dStats.maxDate);
            updateDateButtonLabel();
        });

        VBox dateContent = new VBox(10,
                new Label("From"), datePickerFrom,
                new Label("To"), datePickerTo,
                resetDateButton);
        dateContent.setPadding(new Insets(15));
        datePopOver = new PopOver(dateContent);
        datePopOver.setArrowLocation(PopOver.ArrowLocation.TOP_CENTER);
        datePopOver.setDetachable(false);

        datePickerFrom.valueProperty().addListener((obs, o, n) -> updateDateButtonLabel());
        datePickerTo.valueProperty().addListener((obs, o, n) -> updateDateButtonLabel());

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

        // Restore heatmap state from before the last scene switch.
        if (heatmapWasOn) {
            mapHeatmapToggle.setSelected(true);
            toggleMapHeatmap();
        }

        populateRecentDatasetsMenu();
    }

    /** Builds the SplitMenuButton dropdown: recent datasets (excluding the current one), then a "Sample" reset entry. */
    private void populateRecentDatasetsMenu() {
        openDatasetButton.getItems().clear();

        java.io.File current = MemoryLoader.getLastSourceFile();
        String currentAbs = (current != null) ? current.getAbsolutePath() : null;

        for (java.io.File f : MemoryLoader.getRecentDatasets()) {
            if (currentAbs != null && f.getAbsolutePath().equals(currentAbs)) continue;
            MenuItem item = new MenuItem(truncate(f.getName(), 40));
            item.setMnemonicParsing(false); // Underscores in filenames are literal, not Alt-shortcuts.
            item.setOnAction(e -> Main.loadDataset((Stage) mainPane.getScene().getWindow(), f));
            openDatasetButton.getItems().add(item);
        }

        if (!openDatasetButton.getItems().isEmpty()) {
            openDatasetButton.getItems().add(new SeparatorMenuItem());
        }

        MenuItem sampleItem = new MenuItem("Sample dataset");
        sampleItem.setMnemonicParsing(false);
        sampleItem.setOnAction(e -> Main.loadBundledSample((Stage) mainPane.getScene().getWindow()));
        openDatasetButton.getItems().add(sampleItem);
    }

    /** Caps a filename at {@code max} characters, preserving the extension when present so the cap looks like {@code prefix….ext}. */
    private static String truncate(String name, int max) {
        if (name == null || name.length() <= max) return name;
        int dot = name.lastIndexOf('.');
        int extLen = (dot > 0) ? name.length() - dot : -1;
        if (extLen > 0 && extLen <= 8 && (max - extLen - 1) >= 5) {
            return name.substring(0, max - extLen - 1) + "…" + name.substring(dot);
        }
        return name.substring(0, max - 1) + "…";
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
        // Baseline is 1.15 — gives a slightly bigger map even at low dot counts.
        double scale = 1.15;
        if (activeIatas.size() > 150) {
            scale = Math.min(1.44, 1.15 * Math.sqrt(activeIatas.size() / 150.0));
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
                // OurAirports iso_region is "US-CA" for states; territories use
                // "PR-U-A" / "VI-U-A" / etc. Reduce both to the 2-letter code.
                String region = row[4];
                String state;
                if (region.startsWith("US-")) {
                    state = region.substring(3);
                } else if (region.contains("-")) {
                    state = region.substring(0, region.indexOf('-'));
                } else {
                    state = region;
                }

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

        relaxOverlaps(5.0);
    }

    /**
     * Iteratively pushes overlapping dots apart while pulling each toward its
     * anchor (the actual airport position). Each iteration applies both forces;
     * loop exits when the largest single push drops below a small threshold or
     * after a max iteration count. O(n²) per iter — fine for ~hundreds of dots.
     */
    private void relaxOverlaps(double radius) {
        final double minDist = 2 * radius;
        final double pullStrength = 0.2;
        final int maxIter = 100;
        final double convergedThreshold = 0.05;

        for (int iter = 0; iter < maxIter; iter++) {
            // Spring pull toward anchor.
            for (Airport a : airports) {
                a.x += (a.anchorX - a.x) * pullStrength;
                a.y += (a.anchorY - a.y) * pullStrength;
            }

            // Push overlapping pairs apart.
            double maxMove = 0;
            int n = airports.size();
            for (int i = 0; i < n; i++) {
                Airport ai = airports.get(i);
                for (int j = i + 1; j < n; j++) {
                    Airport aj = airports.get(j);
                    double dx = aj.x - ai.x;
                    double dy = aj.y - ai.y;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist >= minDist) continue;
                    double ux, uy;
                    if (dist < 1e-6) { ux = 1; uy = 0; }
                    else             { ux = dx / dist; uy = dy / dist; }
                    double push = (minDist - dist) / 2.0;
                    ai.x -= ux * push;
                    ai.y -= uy * push;
                    aj.x += ux * push;
                    aj.y += uy * push;
                    if (push > maxMove) maxMove = push;
                }
            }

            if (maxMove < convergedThreshold) break;
        }

        // Apply relaxed positions to the circles.
        for (Airport a : airports) {
            a.circle.setCenterX(a.x);
            a.circle.setCenterY(a.y);
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

    private javafx.stage.Popup flightAutocompletePopup;
    private javafx.scene.control.ListView<Flight> flightAutocompleteList;

    private void setupFlightAutocomplete() {
        flightAutocompleteList = new javafx.scene.control.ListView<>();
        flightAutocompleteList.setCellFactory(lv -> new javafx.scene.control.ListCell<Flight>() {
            @Override
            protected void updateItem(Flight f, boolean empty) {
                super.updateItem(f, empty);
                setText(empty || f == null ? null : formatFlightSummary(f));
            }
        });
        flightAutocompleteList.setFocusTraversable(false);

        flightAutocompletePopup = new javafx.stage.Popup();
        flightAutocompletePopup.setAutoHide(true);
        flightAutocompletePopup.setHideOnEscape(true);
        flightAutocompletePopup.getContent().add(flightAutocompleteList);

        flightAutocompleteList.setOnMouseClicked(e -> {
            Flight picked = flightAutocompleteList.getSelectionModel().getSelectedItem();
            if (picked != null) selectFlight(picked);
        });

        flightSearchField.textProperty().addListener((obs, oldV, newV) -> {
            if (ignoreFlightFilterOnce) {
                ignoreFlightFilterOnce = false;
                flightAutocompletePopup.hide();
                return;
            }
            updateFlightSuggestions(newV);
        });

        flightSearchField.focusedProperty().addListener((obs, was, isF) -> {
            if (!isF) Platform.runLater(flightAutocompletePopup::hide);
        });
    }

    private void updateFlightSuggestions(String text) {
        String input = text == null ? "" : text.toUpperCase();
        if (!input.matches("^[A-Z]{2}\\d+$")) {
            flightAutocompletePopup.hide();
            return;
        }
        java.util.List<Flight> matches = MemoryLoader.getAllFlights().stream()
                .filter(f -> (f.mktCarrier + f.flightNum).equalsIgnoreCase(input))
                .limit(20)
                .collect(Collectors.toList());
        if (matches.isEmpty()) {
            flightAutocompletePopup.hide();
            return;
        }
        flightAutocompleteList.getItems().setAll(matches);
        // Force the list width to match the search field; this is the whole
        // point of the rewrite.
        double w = flightSearchField.getWidth();
        flightAutocompleteList.setPrefWidth(w);
        flightAutocompleteList.setMinWidth(w);
        flightAutocompleteList.setMaxWidth(w);
        flightAutocompleteList.setPrefHeight(Math.min(220, matches.size() * 24 + 4));
        javafx.geometry.Bounds b = flightSearchField.localToScreen(flightSearchField.getBoundsInLocal());
        if (b == null) return;
        if (flightAutocompletePopup.isShowing()) {
            flightAutocompletePopup.setX(b.getMinX());
            flightAutocompletePopup.setY(b.getMaxY());
        } else {
            flightAutocompletePopup.show(flightSearchField, b.getMinX(), b.getMaxY());
        }
    }

    private void selectFlight(Flight f) {
        showFlightDetails(f);
        String flightNum = f.mktCarrier + f.flightNum;
        Platform.runLater(() -> {
            ignoreFlightFilterOnce = true;
            flightSearchField.setText(flightNum);
            flightSearchField.positionCaret(flightNum.length());
            flightAutocompletePopup.hide();
        });
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

        java.time.LocalDate dateFrom = datePickerFrom.getValue();
        java.time.LocalDate dateTo = datePickerTo.getValue();
        MemoryLoader.DatasetStats datasetStats = MemoryLoader.getStats();
        boolean atFullRange = java.util.Objects.equals(dateFrom, datasetStats.minDate)
                           && java.util.Objects.equals(dateTo, datasetStats.maxDate);
        boolean dateFilterActive = (dateFrom != null || dateTo != null) && !atFullRange;

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

                    boolean matchCancelled = !showCancelled || f.isCancelled();
                    boolean matchDiverted = !showDiverted || f.isDiverted();

                    boolean matchDate = true;
                    if (dateFilterActive) {
                        java.time.LocalDate d = f.parsedDate();
                        if (d == null) matchDate = false;
                        else if (dateFrom != null && d.isBefore(dateFrom)) matchDate = false;
                        else if (dateTo != null && d.isAfter(dateTo)) matchDate = false;
                    }

                    return matchDep && matchArr && matchCarrier && matchOrigin && matchDest && matchCancelled && matchDiverted && matchDate;
                })
                .collect(Collectors.toList())
        );
        
        

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/FlightResults.fxml"));
        Parent resultsRoot = loader.load();

        FlightResultsController resultsController = loader.getController();
        resultsController.setFlights(filteredFlights);

        Main.swapCenter((Node) event.getSource(), resultsRoot);
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
        Main.swapCenter((Node) event.getSource(), newRoot);
    }

    public void switchToGraphs(ActionEvent event) throws IOException {
        String selectedCity = cityComboBox.getEditor().getText();
        if (selectedCity == null || selectedCity.isEmpty()) return;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/GraphsScene.fxml"));
        Parent newRoot = loader.load();

        ControllerGraphs controllerGraphs = loader.getController();
        controllerGraphs.displayInput(selectedCity);

        Main.swapCenter((Node) event.getSource(), newRoot);
    }

    public void switchToGraphsBUTTON() throws IOException {
        String selectedCity = cityComboBox.getEditor().getText();
        if (selectedCity == null || selectedCity.isEmpty()) return;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/GraphsScene.fxml"));
        Parent newRoot = loader.load();

        ControllerGraphs controllerGraphs = loader.getController();
        controllerGraphs.displayInput(selectedCity);

        Main.swapCenter(cityComboBox, newRoot);
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

    @FXML
    public void openDatePopover() {
        datePopOver.show(dateButton);
    }

    private void updateDateButtonLabel() {
        java.time.LocalDate from = datePickerFrom.getValue();
        java.time.LocalDate to = datePickerTo.getValue();
        MemoryLoader.DatasetStats s = MemoryLoader.getStats();
        boolean atFullRange = java.util.Objects.equals(from, s.minDate)
                           && java.util.Objects.equals(to, s.maxDate);
        if ((from == null && to == null) || atFullRange) {
            dateButton.setText("Date");
            return;
        }
        String fs = from != null ? from.toString() : "…";
        String ts = to != null ? to.toString() : "…";
        dateButton.setText(fs + " – " + ts);
    }

    @FXML
    public void openDataset() {
        Stage stage = (Stage) mainPane.getScene().getWindow();
        Main.onOpenDataset(stage);
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
        heatmapWasOn = showHeatmap;
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
