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

                XYChart.Series<Number, String> series = new XYChart.Series<>();
                for (int i = 0; i < destinations.size(); i++) {
                    series.getData().add(new XYChart.Data<>(counts.get(i), destinations.get(i)));
                }

                barChart.getData().clear();
                barChart.getData().add(series);
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
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Main.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
}
