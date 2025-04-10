#!/bin/bash

java \
--module-path "./javafx-sdk-24/lib" \
--add-modules javafx.controls,javafx.fxml,javafx.web \
--enable-native-access=ALL-UNNAMED \
--add-exports javafx.base/com.sun.javafx.event=ALL-UNNAMED \
--add-exports javafx.base/com.sun.javafx.logging=ALL-UNNAMED \
--add-exports javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED \
--add-exports javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED \
--add-exports javafx.graphics/com.sun.javafx.util=ALL-UNNAMED \
--add-exports javafx.graphics/com.sun.prism=ALL-UNNAMED \
--add-exports javafx.graphics/com.sun.glass.ui=ALL-UNNAMED \
--add-exports javafx.graphics/com.sun.javafx.geom.transform=ALL-UNNAMED \
--add-exports javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED \
-jar SkyScraper.jar

