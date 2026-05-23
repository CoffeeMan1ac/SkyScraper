package application;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HeatmapViewer {

    private static final String INPUT_SVG = "us.svg";
    private static final String OUTPUT_SVG = "updated_map.svg";
    private static final String CSV_FILE = "flights_sample.csv";

   
    private static Map<String, Integer> stateCounts = new HashMap<>();

   
    @FXML
    private WebView webView;

    @FXML
    public void showHeatmap() {
        try {
            readCsvAndCountStates();
            int minCount = stateCounts.values().stream().min(Integer::compareTo).orElse(0);
            int maxCount = stateCounts.values().stream().max(Integer::compareTo).orElse(0);
            File inputFile = new File(INPUT_SVG);
            Document svg = Jsoup.parse(inputFile, "UTF-8");
            updateSvgColors(svg, minCount, maxCount);
            writeUpdatedSvgToFile(svg);
            displaySvgInWebView();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readCsvAndCountStates() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(CSV_FILE));
        String line;
        reader.readLine();
        while ((line = reader.readLine()) != null) {
            String[] values = line.split(",");
            
            // State Abbreviation column 
            String stateAbbr = values[10].trim();
            if (stateAbbr.endsWith("\"")) {
                stateAbbr = stateAbbr.substring(0, stateAbbr.length() - 1);
            }

          
            stateCounts.put(stateAbbr, stateCounts.getOrDefault(stateAbbr, 0) + 1);
        }

        reader.close();
    }


    private void updateSvgColors(Document svg, int minCount, int maxCount) {
        for (Map.Entry<String, Integer> entry : stateCounts.entrySet()) {
            String stateAbbr = entry.getKey().trim().toUpperCase(); 
            int stateCount = entry.getValue();
            String color = calculateColor(stateCount, minCount, maxCount);

            // Find the state element by ID and update its fill color
            Element stateElement = svg.getElementById(stateAbbr);
           
            if (stateElement != null) {
                String style = stateElement.attr("style");
                String updatedStyle = style.replaceAll("fill:\\s*#[A-Fa-f0-9]{6}", "fill:" + color);
                stateElement.attr("style", updatedStyle);
            } 
        }
    }

    private void writeUpdatedSvgToFile(Document svg) throws IOException {
        FileWriter writer = new FileWriter(OUTPUT_SVG);
        writer.write(svg.outerHtml());
        writer.close();
    }

    // Embeds the updated SVG into the WebView
    private void displaySvgInWebView() {
        File updatedFile = new File(OUTPUT_SVG);
        String svgContent = readSvgContent(updatedFile);

        int minCount = stateCounts.values().stream().min(Integer::compareTo).orElse(0);
        int maxCount = stateCounts.values().stream().max(Integer::compareTo).orElse(0);

        String htmlContent = """
            <html>
                <head>
                    <style>
                        body {
                            margin: 0;
                            padding: 0;
                            background-color: white;
                            font-family: Arial, sans-serif;
                            overflow: hidden;
                        }
                        .container {
                            width: 100%%;
                            height: 100%%;
                            display: flex;
                            flex-direction: column;
                            align-items: center;
                            justify-content: center;
                        }
                        .svg-wrapper {
                            width: 100%%;
                            height: 90%%;
                            overflow: hidden;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                        }
                        svg {
                            width: 100%%;
                            height: auto;
                        }
                        .legend-bar {
                            width: 60%%;
                            height: 10px;
                            background: linear-gradient(to right, #0000FF, #FF0000);
                            margin-top: 8px;
                            border-radius: 5px;
                        }
                        .legend-labels {
                            width: 60%%;
                            display: flex;
                            justify-content: space-between;
                            font-size: 10px;
                            margin-top: 2px;
                            color: #333;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="svg-wrapper">
                            %s
                        </div>
                        <div class="legend-bar"></div>
                        <div class="legend-labels">
                            <span>Low (%d)</span>
                            <span>High (%d)</span>
                        </div>
                    </div>
                </body>
            </html>
            """.formatted(svgContent, minCount, maxCount);

        webView.getEngine().loadContent(htmlContent);
    }

    // Reads SVG file contents into a string
    private String readSvgContent(File svgFile) {
        try {
            return new String(java.nio.file.Files.readAllBytes(svgFile.toPath()));
        } catch (IOException e) {
            e.printStackTrace();
            return "<svg></svg>"; 
        }
    }

    // Maps a value to the gradient
    private String calculateColor(int stateCount, int minCount, int maxCount) {
        if (maxCount == minCount) {
            return "#808080"; //Default Grey
        }

        // Normalize state count
        double ratio = (double) (stateCount - minCount) / (maxCount - minCount);
        int red = (int) (ratio * 255);
        int blue = (int) ((1 - ratio) * 255);
        int green = 0; 

        return String.format("#%02X%02X%02X", red, green, blue); 
    }
    
    // Scene switching
    @FXML
    public void switchToMainFromHeatmap(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Main.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
}
