package application;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;

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

public class ControllerGraphs {

    @FXML Label sqlLabel;
    @FXML BarChart<Number, String> barChart;
    @FXML Label debugLabel;
    @FXML javafx.scene.control.ScrollPane graphScrollPane;
    
    // Load and display destination counts per user selected origin city
    public void displayInput(String cityName) {
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
                                new javafx.scene.control.Tooltip(d.getYValue() + " — " + d.getXValue() + " flights");
                        t.setShowDelay(javafx.util.Duration.millis(60));
                        javafx.scene.control.Tooltip.install(bar, t);
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

    // Scene switching
    public void switchToMain(ActionEvent event) throws IOException {
        Parent newRoot = FXMLLoader.load(getClass().getResource("/Main.fxml"));
        Main.swapCenter((Node) event.getSource(), newRoot);
    }
}
