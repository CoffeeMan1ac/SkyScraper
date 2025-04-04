package application;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import application.Flight;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;


public class FlightDetailsController {

	private static final String API_KEY = "VDjfGgv8mxiTvvLLwGicD6V2eq";
	@FXML
	private ImageView airlineLogo;

    @FXML
    private Label detailsLabel;

    private Stage stage;

    // Called from Controller.java to pass the Flight object
    public void setFlight(Flight flight) {
        String formattedDetails = String.format(
                "Flight Info:\n" +
                "Date: %s\n" +
                "Carrier: %s\n" +
                "Flight Number: %s\n" +
                "Origin: %s\n" +
                "Destination: %s\n" +
                "Departure Time: %s\n" +
                "Arrival Time: %s\n" +
                "Cancelled: %s\n" +
                "Diverted: %s\n" +
                "Distance: %s miles",
                flight.flDate,
                flight.mktCarrier,
                flight.flightNum,
                flight.originCity,
                flight.destCity,
                flight.depTime,
                flight.arrTime,
                flight.cancelled,
                flight.diverted,
                flight.distance
        );

        detailsLabel.setText(formattedDetails);
     
        loadAirlineLogo(flight.mktCarrier);
    }
    
    private void loadAirlineLogo(String airlineCode) {
        try {
            String width = "100";
            String height = "100";
            String type = "s"; // square
            String signatureBase = airlineCode + "_" + width + "_" + height + "_" + type + "_" + API_KEY;

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(signatureBase.getBytes(StandardCharsets.UTF_8));
            BigInteger number = new BigInteger(1, hashBytes);
            String md5Hash = String.format("%032x", number);

            String imageUrl = String.format(
                    "https://content.airhex.com/content/logos/airlines_%s_%s_%s_%s.png?md5apikey=%s",
                    airlineCode, width, height, type, md5Hash
            );
            
            System.out.println("Airline Logo URL: " + imageUrl);

            Image image = new Image(imageUrl, true);
            airlineLogo.setImage(image);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Optional, used to close the popup
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void closeWindow(ActionEvent event) {
        if (stage != null) {
            stage.close();
        } else {
            ((Stage) detailsLabel.getScene().getWindow()).close();
        }
    }
}
