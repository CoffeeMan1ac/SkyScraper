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

                // Shrink y-axis tick-label font so JavaFX's collision check
                // doesn't drop any name. Per-category slot = plot height / N;
                // text fits when font ≲ 0.7 × slot. Floor at 3 px so even very
                // long destination lists stay visible (tooltip on bar hover
                // recovers readability for the tiny ones).
                int n = Math.max(1, destinations.size());
                double plotHeight = 480.0; // conservative estimate for this scene's chart area
                double fontSize = Math.max(3.0, Math.min(14.0, (plotHeight / n) * 0.7));
                barChart.getYAxis().setTickLabelFont(javafx.scene.text.Font.font(fontSize));

                XYChart.Series<Number, String> series = new XYChart.Series<>();
                for (int i = 0; i < destinations.size(); i++) {
                    XYChart.Data<Number, String> d = new XYChart.Data<>(counts.get(i), destinations.get(i));
                    series.getData().add(d);
                }

                barChart.getData().clear();
                barChart.getData().add(series);

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
        ((Node) event.getSource()).getScene().setRoot(newRoot);
    }
}
