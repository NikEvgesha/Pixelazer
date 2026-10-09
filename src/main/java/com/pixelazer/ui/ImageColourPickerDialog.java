package com.pixelazer.ui;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.image.BufferedImage;
import java.util.OptionalInt;

/** Pixel-exact colour picker for adding a source colour to the locked palette. */
final class ImageColourPickerDialog {
    private final BufferedImage source;
    private final Stage stage = new Stage();
    private final ImageView imageView;
    private final ComboBox<Integer> zoomBox = new ComboBox<>();
    private final Rectangle sample = new Rectangle(30, 30, Color.TRANSPARENT);
    private final Label valueLabel = new Label("Цвет не выбран");
    private final Label coordinates = new Label("Пиксель: —");
    private final Button accept = new Button("Добавить цвет");
    private int zoom;
    private Integer selectedArgb;

    ImageColourPickerDialog(Window owner, BufferedImage source) {
        this.source = source;
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Пипетка — выбор фиксированного цвета");
        stage.setMinWidth(640);
        stage.setMinHeight(480);

        imageView = new ImageView(SwingFXUtils.toFXImage(source, null));
        imageView.setPreserveRatio(false);
        imageView.setSmooth(false);

        int maximumZoom = Math.max(1, Math.min(32,
                8192 / Math.max(source.getWidth(), source.getHeight())));
        for (int value = 1; value <= maximumZoom; value++) zoomBox.getItems().add(value);
        zoom = Math.min(maximumZoom, Math.max(1,
                Math.min(760 / source.getWidth(), 520 / source.getHeight())));
        zoomBox.setValue(zoom);
        zoomBox.setPrefWidth(85);
        zoomBox.setCellFactory(list -> new ZoomCell());
        zoomBox.setButtonCell(new ZoomCell());
        zoomBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                zoom = newValue;
                updateImageSize();
            }
        });

        Button zoomOut = new Button("−");
        zoomOut.setOnAction(event -> changeZoom(-1));
        Button zoomIn = new Button("+");
        zoomIn.setOnAction(event -> changeZoom(1));
        HBox toolbar = new HBox(8, new Label("Масштаб:"), zoomOut, zoomBox, zoomIn,
                new Separator(), new Label("Нажмите на нужный пиксель"));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(10));

        ScrollPane scroll = new ScrollPane(imageView);
        scroll.setPannable(true);
        scroll.setStyle("-fx-background: #252525; -fx-background-color: #252525;");
        imageView.setOnMouseMoved(event -> showCoordinates(event.getX(), event.getY()));
        imageView.setOnMouseExited(event -> coordinates.setText("Пиксель: —"));
        imageView.setOnMouseClicked(event -> pick(event.getX(), event.getY()));

        sample.setStroke(Color.web("#555555"));
        accept.setDisable(true);
        accept.setDefaultButton(true);
        accept.setOnAction(event -> stage.close());
        Button cancel = new Button("Отмена");
        cancel.setOnAction(event -> {
            selectedArgb = null;
            stage.close();
        });
        HBox footer = new HBox(10, coordinates, sample, valueLabel, spacer(), cancel, accept);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10));

        BorderPane root = new BorderPane(scroll);
        root.setTop(toolbar);
        root.setBottom(footer);
        stage.setScene(new Scene(root, 960, 700));
        updateImageSize();
    }

    OptionalInt showAndWait() {
        stage.showAndWait();
        return selectedArgb == null ? OptionalInt.empty() : OptionalInt.of(selectedArgb);
    }

    private void pick(double viewX, double viewY) {
        int x = (int) (viewX / zoom);
        int y = (int) (viewY / zoom);
        if (x < 0 || x >= source.getWidth() || y < 0 || y >= source.getHeight()) return;
        selectedArgb = source.getRGB(x, y) | 0xff000000;
        sample.setFill(toFxColour(selectedArgb));
        valueLabel.setText(String.format("#%06X", selectedArgb & 0xffffff));
        coordinates.setText("Пиксель: " + x + ", " + y);
        accept.setDisable(false);
    }

    private void showCoordinates(double viewX, double viewY) {
        int x = (int) (viewX / zoom);
        int y = (int) (viewY / zoom);
        coordinates.setText(x >= 0 && x < source.getWidth() && y >= 0 && y < source.getHeight()
                ? "Пиксель: " + x + ", " + y : "Пиксель: —");
    }

    private void updateImageSize() {
        imageView.setFitWidth((double) source.getWidth() * zoom);
        imageView.setFitHeight((double) source.getHeight() * zoom);
    }

    private void changeZoom(int delta) {
        int index = zoomBox.getItems().indexOf(zoom);
        int next = Math.max(0, Math.min(zoomBox.getItems().size() - 1, index + delta));
        zoomBox.setValue(zoomBox.getItems().get(next));
    }

    private static Color toFxColour(int argb) {
        return Color.rgb((argb >>> 16) & 0xff, (argb >>> 8) & 0xff, argb & 0xff);
    }

    private static Region spacer() {
        Region region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        return region;
    }

    private static final class ZoomCell extends ListCell<Integer> {
        @Override protected void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item + "×");
        }
    }
}
