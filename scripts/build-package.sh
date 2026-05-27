#!/usr/bin/env bash
# Builds a portable app-image at target/dist/SkyScraper/ with a bundled JRE.
# Run: ./scripts/build-package.sh
# Output: target/dist/SkyScraper/bin/SkyScraper (launcher)
set -euo pipefail

cd "$(dirname "$0")/.."

JAR_NAME="skyscraper-1.0.0-SNAPSHOT.jar"
INPUT_DIR="target/jpackage-input"

./mvnw -q clean package -DskipTests
./mvnw -q dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory="$INPUT_DIR"
cp "target/$JAR_NAME" "$INPUT_DIR/"

rm -rf target/dist
jpackage \
    --type app-image \
    --name SkyScraper \
    --app-version 1.0.0 \
    --dest target/dist \
    --input "$INPUT_DIR" \
    --main-jar "$JAR_NAME" \
    --main-class application.Main \
    --module-path "$INPUT_DIR" \
    --add-modules javafx.controls,javafx.fxml,javafx.graphics \
    --java-options "--enable-native-access=javafx.graphics" \
    --java-options "--add-exports=javafx.base/com.sun.javafx.event=ALL-UNNAMED" \
    --java-options "--add-exports=javafx.base/com.sun.javafx.logging=ALL-UNNAMED" \
    --java-options "--add-exports=javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED" \
    --java-options "--add-exports=javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED" \
    --java-options "--add-exports=javafx.graphics/com.sun.javafx.util=ALL-UNNAMED" \
    --java-options "--add-exports=javafx.graphics/com.sun.prism=ALL-UNNAMED" \
    --java-options "--add-exports=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED" \
    --java-options "--add-exports=javafx.graphics/com.sun.javafx.geom.transform=ALL-UNNAMED" \
    --java-options "--add-exports=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED"

echo "Built: target/dist/SkyScraper/bin/SkyScraper"
