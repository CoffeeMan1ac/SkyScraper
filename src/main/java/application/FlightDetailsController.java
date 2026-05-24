package application;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;


public class FlightDetailsController {

    @FXML private ImageView airlineLogo;
    @FXML private Label airlineFallback;
    @FXML private Label detailsLabel;

    private Runnable onClose;

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

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
        String imageUrl = String.format("https://images.kiwi.com/airlines/64/%s.png", airlineCode);

        airlineFallback.setText(airlineCode);
        airlineLogo.setVisible(true);
        airlineFallback.setVisible(false);

        Image image = new Image(imageUrl, true);
        image.errorProperty().addListener((obs, wasError, isError) -> {
            if (Boolean.TRUE.equals(isError)) {
                airlineLogo.setVisible(false);
                airlineFallback.setVisible(true);
            }
        });
        airlineLogo.setImage(image);
    }

    @FXML
    private void closeFlightDetails() {
        if (onClose != null) onClose.run();
    }
}
