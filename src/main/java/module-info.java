module HelloSceneBuilder {
	requires javafx.controls;
	requires javafx.graphics;
	requires javafx.fxml;
	requires javafx.base;
	requires java.sql;
	requires com.opencsv;
	requires org.controlsfx.controls;
	requires javafx.web;
	requires org.jsoup;
	requires com.google.gson;
	
	opens application to javafx.graphics, javafx.fxml, javafx.base;

}
