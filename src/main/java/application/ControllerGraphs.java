package application;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;

import javafx.collections.FXCollections;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ControllerGraphs {

    @FXML Label sqlLabel;
    @FXML BarChart<Number, String> barChart;
    @FXML Label debugLabel;
    @FXML javafx.scene.control.ScrollPane graphScrollPane;

    /** Origin city that produced this chart. Saved so a bar click can drill in
     *  to the table of flights for the origin→destination pair. */
    private String originCity;

    /** Cached main scene to swap back into instead of reloading Main.fxml —
     *  reloading triggers Controller.initialize → buildMap, which rebuilds
     *  ~300 airport circles and runs an O(n²) overlap-relaxation. Set by the
     *  caller before this controller's UI is shown. */
    private Parent backTarget;
    /** Esc handler that was in effect before we took over — restored on back
     *  so Esc on the returned-to scene behaves correctly. */
    private Runnable previousEsc;

    public void setBackTarget(Parent backTarget) {
        this.backTarget = backTarget;
    }

    @FXML
    public void initialize() {
        previousEsc = Main.getEscHandler();
        Main.setEscHandler(this::escapeToMain);
    }

    private void escapeToMain() {
        goBack(barChart);
    }

    private void goBack(Node sourceForSwap) {
        if (backTarget != null) {
            System.out.println("[Esc] graph: returning to cached main");
            Main.swapCenter(sourceForSwap, backTarget);
            if (previousEsc != null) Main.setEscHandler(previousEsc);
            return;
        }
        try {
            System.out.println("[Esc] graph: reloading main");
            Parent newRoot = FXMLLoader.load(getClass().getResource("/Main.fxml"));
            Main.swapCenter(sourceForSwap, newRoot);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // Load and display destination counts per user selected origin city
    public void displayInput(String cityName) {
        this.originCity = cityName;
        sqlLabel.setText("Origin city: " + cityName);

        Task<Void> dbTask = new Task<>() {
        	@Override
        	protected Void call() {
                try {
                	// Print statements for debug purposes
                	System.out.println("Started preloading...");
                	MemoryLoader.importCSVToMemory();
                	System.out.println("Preloading complete.");

                	MemoryLoader.queryCityDestCounts(cityName);
                    System.out.println("Query finished.");

                } catch (Exception e) {
                    System.out.println("Exception caught in call():");
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();

                ArrayList<String> destinations = MemoryLoader.getDestList();
                ArrayList<Integer> counts = MemoryLoader.getDestCountList();

                debugLabel.setText("Found " + destinations.size() + " destinations.");

                XYChart.Series<Number, String> series = new XYChart.Series<>();
                for (int i = 0; i < destinations.size(); i++) {
                    XYChart.Data<Number, String> d = new XYChart.Data<>(counts.get(i), destinations.get(i));
                    series.getData().add(d);
                }

                // Use the ScrollPane's actual viewport height so the chart
                // fills the page as before. If at that viewport the font would
                // need to drop below ~3 px to fit every destination, grow the
                // chart's pref height instead so font can stay at 4 px and
                // the ScrollPane scrolls.
                int n = Math.max(1, destinations.size());
                javafx.geometry.Bounds vb = graphScrollPane.getViewportBounds();
                double viewportH = (vb != null && vb.getHeight() > 100) ? vb.getHeight() : 580.0;
                // Plot area is smaller than the chart by the axis/title chrome;
                // ~80 px is a reliable estimate for this scene's layout.
                double plotH = Math.max(100.0, viewportH - 80.0);
                double slotInPlot = plotH / n;
                double idealFont = (slotInPlot - 2.0) / 1.2;

                double fontSize;
                boolean grow;
                if (idealFont >= 3.0) {
                    fontSize = Math.min(14.0, idealFont);
                    grow = false;
                } else {
                    // Keep font slightly above the unreadable floor.
                    fontSize = 4.0;
                    grow = true;
                }

                javafx.scene.chart.CategoryAxis yAxis = (javafx.scene.chart.CategoryAxis) barChart.getYAxis();
                javafx.scene.text.Font tickFont = javafx.scene.text.Font.font(fontSize);

                // Configure the axis FULLY before adding data: turn off
                // auto-ranging, set categories explicitly, then synchronous
                // CSS + layout pass. This is what makes the first scene visit
                // render correctly.
                yAxis.setAutoRanging(false);
                yAxis.setCategories(javafx.collections.FXCollections.observableArrayList(destinations));
                yAxis.setTickLabelGap(1);
                yAxis.setTickLabelFont(tickFont);

                barChart.prefHeightProperty().unbind();
                if (grow) {
                    // Explicit pref height that fits every label at the target
                    // font; ScrollPane will scroll.
                    double neededPlot = n * (fontSize * 1.2 + 2);
                    barChart.setPrefHeight(neededPlot + 80);
                } else {
                    // Fill the ScrollPane viewport — track it via a binding so
                    // chart resizes if the window does.
                    barChart.prefHeightProperty().bind(
                        javafx.beans.binding.Bindings.createDoubleBinding(
                            () -> {
                                javafx.geometry.Bounds b = graphScrollPane.getViewportBounds();
                                return b != null ? b.getHeight() : 0.0;
                            },
                            graphScrollPane.viewportBoundsProperty()));
                }
                barChart.applyCss();
                barChart.layout();

                barChart.getData().clear();
                barChart.getData().add(series);

                javafx.application.Platform.runLater(() -> {
                    yAxis.setTickLabelGap(1);
                    yAxis.setTickLabelFont(tickFont);
                    yAxis.requestAxisLayout();
                });

                // Hover tooltip on each bar — shows full destination + count
                // even when the axis label is shrunk to a few pixels.
                for (XYChart.Data<Number, String> d : series.getData()) {
                    attachBarTooltip(d);
                }
            }

            private void attachBarTooltip(XYChart.Data<Number, String> d) {
                Runnable install = () -> {
                    Node bar = d.getNode();
                    if (bar != null) {
                        javafx.scene.control.Tooltip t =
                                new javafx.scene.control.Tooltip(d.getYValue() + " — " + d.getXValue() + " flights · click to view");
                        t.setShowDelay(javafx.util.Duration.millis(60));
                        javafx.scene.control.Tooltip.install(bar, t);
                        bar.setCursor(Cursor.HAND);
                        bar.setOnMouseClicked(e -> openResultsForDestination(d.getYValue()));
                    }
                };
                if (d.getNode() != null) {
                    install.run();
                } else {
                    d.nodeProperty().addListener((obs, oldN, newN) -> {
                        if (newN != null) install.run();
                    });
                }
            }


            @Override
            protected void failed() {
                getException().printStackTrace();
            }
        };

        new Thread(dbTask).start();
    }

    /** Swaps to a results table containing every flight on the
     *  originCity → destCity pair. Same table as the main-search results. */
    private void openResultsForDestination(String destCity) {
        if (originCity == null || destCity == null) return;
        List<Flight> matches = MemoryLoader.getAllFlights().stream()
                .filter(f -> originCity.equals(f.originCity) && destCity.equals(f.destCity))
                .collect(Collectors.toList());
        try {
            // Capture the current graph node so Back returns the user to the
            // same chart instance — preserves the rendered bars without
            // re-running the heavy query.
            Parent currentGraph = Main.getShellCenter(barChart);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FlightResults.fxml"));
            Parent root = loader.load();
            FlightResultsController controller = loader.getController();
            controller.setFlights(FXCollections.observableArrayList(matches));
            if (currentGraph != null) controller.setBackTarget(currentGraph);
            Main.swapCenter(barChart, root);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // Scene switching
    public void switchToMain(ActionEvent event) throws IOException {
        goBack((Node) event.getSource());
    }
}
