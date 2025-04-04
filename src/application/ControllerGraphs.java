// Updated ControllerGraphs.java
package application;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
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

    @FXML
    Label sqlLabel;

    @FXML
    BarChart<Number, String> barChart;
    
    @FXML Label debugLabel;
    
    public void displayInput(String cityName) {
        sqlLabel.setText("Origin city: " + cityName);

        Task<Void> dbTask = new Task<>() {
        	@Override
        	protected Void call() {
                try {
                	System.out.println("Started preloading...");
                	MemoryLoader.importCSVToMemory();
                	System.out.println("Preloading complete.");


                    //System.out.println("Querying for city: " + cityName);
                    //SQLPart.debugListAllOrigins();
                	MemoryLoader.queryCityDestCounts(cityName);
                    System.out.println("Query finished.");

                } catch (Exception e) {
                    System.out.println("Exception caught in call():");
                    e.printStackTrace(); // Make sure we see any errors
                }

                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();

                ArrayList<String> destinations = MemoryLoader.getDestList();
                ArrayList<Integer> counts = MemoryLoader.getDestCountList();

                // 🟡 Show debug info in the UI
                debugLabel.setText("Found " + destinations.size() + " destinations.");

                XYChart.Series<Number, String> series = new XYChart.Series<>();
                for (int i = 0; i < destinations.size(); i++) {
                    series.getData().add(new XYChart.Data<>(counts.get(i), destinations.get(i)));
                }

                barChart.getData().clear();
                barChart.getData().add(series);
            }

        	
        	// THIS BELOW INSTEAD LOADS DYNAMICALLY
//            @Override
//            protected void succeeded() {
//                super.succeeded();
//
//                ArrayList<String> destinations = SQLPart.getDestList();
//                ArrayList<Integer> counts = SQLPart.getDestCountList();
//
//                debugLabel.setText("Found " + destinations.size() + " destinations.");
//
//                XYChart.Series<Number, String> series = new XYChart.Series<>();
//                barChart.getData().clear();
//                barChart.getData().add(series);
//
//                // 🟢 Add data dynamically
//                new Thread(() -> {
//                    for (int i = 0; i < destinations.size(); i++) {
//                        int index = i;
//                        try {
//                            Thread.sleep(50); // Adjust speed here (smaller = faster)
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        javafx.application.Platform.runLater(() -> {
//                            series.getData().add(new XYChart.Data<>(counts.get(index), destinations.get(index)));
//                        });
//                    }
//                }).start();
//            }


            @Override
            protected void failed() {
                getException().printStackTrace(); // Show any errors
            }
        };

        new Thread(dbTask).start();
    }


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
