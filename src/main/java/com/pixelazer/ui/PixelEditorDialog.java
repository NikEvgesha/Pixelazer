package com.pixelazer.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class PixelEditorDialog {
    private static final int MAX_CANVAS_SIDE = 8192;

    private final PixelEditorModel model;
    private final List<Integer> palette;
    private final Stage stage = new Stage();
    private final Canvas canvas = new Canvas();
    private final ComboBox<Integer> zoomBox = new ComboBox<>();
    private final Label coordinates = new Label("Пиксель: —");
    private final Map<Integer, ToggleButton> paletteButtons = new HashMap<>();
    private final ToggleButton paintTool = new ToggleButton("Кисть");
    private final ToggleButton eyedropperTool = new ToggleButton("Пипетка");
    private int selectedColour;
    private int zoom;
    private Tool tool = Tool.PAINT;
    private boolean accepted;
    private int lastPaintX = -1;
    private int lastPaintY = -1;

    PixelEditorDialog(Window owner, BufferedImage image, List<Integer> palette) {
        if (palette == null || palette.isEmpty()) {
            throw new IllegalArgumentException("Palette is required for editing");
        }
        this.model = new PixelEditorModel(image);
        this.palette = List.copyOf(palette);
        this.selectedColour = palette.get(0);

        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Редактор пикселей");
        stage.setMinWidth(720);
        stage.setMinHeight(520);

        int maximumZoom = Math.max(1, Math.min(32,
                MAX_CANVAS_SIDE / Math.max(model.width(), model.height())));
        for (int value = 1; value <= maximumZoom; value++) zoomBox.getItems().add(value);
        zoom = initialZoom(maximumZoom);
        zoomBox.setValue(zoom);
        zoomBox.setPrefWidth(90);
        zoomBox.setButtonCell(new ZoomListCell());
        zoomBox.setCellFactory(list -> new ZoomListCell());
        zoomBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) setZoom(newValue);
        });

        Button zoomOut = new Button("−");
        zoomOut.setOnAction(event -> changeZoom(-1));
        Button zoomIn = new Button("+");
        zoomIn.setOnAction(event -> changeZoom(1));

        ToggleGroup colourGroup = new ToggleGroup();
        FlowPane swatches = new FlowPane(6, 6);
        for (int i = 0; i < palette.size(); i++) {
            int colour = palette.get(i);
            ToggleButton button = colourButton(colour);
            button.setToggleGroup(colourGroup);
            if (i == 0) button.setSelected(true);
            button.setOnAction(event -> {
                selectedColour = colour;
                if (!button.isSelected()) button.setSelected(true);
            });
            paletteButtons.put(colour, button);
            swatches.getChildren().add(button);
        }

        ToggleGroup toolGroup = new ToggleGroup();
        paintTool.setToggleGroup(toolGroup);
        eyedropperTool.setToggleGroup(toolGroup);
        paintTool.setSelected(true);
        paintTool.setOnAction(event -> selectTool(Tool.PAINT));
        eyedropperTool.setOnAction(event -> selectTool(Tool.EYEDROPPER));

        HBox toolbar = new HBox(8, new Label("Инструмент:"), paintTool, eyedropperTool,
                new Separator(), new Label("Масштаб:"), zoomOut, zoomBox, zoomIn,
                new Separator(), new Label("Цвет:"), swatches);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(canvas);
        scrollPane.setPannable(true);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);
        scrollPane.setStyle("-fx-background: #252525; -fx-background-color: #252525;");

        Button cancel = new Button("Отмена");
        cancel.setOnAction(event -> stage.close());
        Button apply = new Button("Применить");
        apply.setDefaultButton(true);
        apply.setOnAction(event -> {
            accepted = true;
            stage.close();
        });
        HBox footer = new HBox(10, coordinates, spacer(), cancel, apply);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(10));

        BorderPane root = new BorderPane(scrollPane);
        root.setTop(toolbar);
        root.setBottom(footer);
        stage.setScene(new Scene(root, 1000, 720));

        canvas.setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.PRIMARY) return;
            if (tool == Tool.EYEDROPPER) pickColourAt(event.getX(), event.getY());
            else paintAt(event.getX(), event.getY());
        });
        canvas.setOnMouseDragged(event -> {
            if (event.isPrimaryButtonDown() && tool == Tool.PAINT) paintAt(event.getX(), event.getY());
        });
        canvas.setOnMouseReleased(event -> resetPaintPosition());
        canvas.setOnMouseMoved(event -> showCoordinates(event.getX(), event.getY()));
        canvas.setOnMouseExited(event -> coordinates.setText("Пиксель: —"));
        redrawAll();
    }

    Optional<BufferedImage> showAndWait() {
        stage.showAndWait();
        return accepted ? Optional.of(model.copyImage()) : Optional.empty();
    }

    private void paintAt(double canvasX, double canvasY) {
        int x = (int) (canvasX / zoom);
        int y = (int) (canvasY / zoom);
        if (x == lastPaintX && y == lastPaintY) return;
        lastPaintX = x;
        lastPaintY = y;
        if (model.paint(x, y, selectedColour)) drawPixel(x, y);
        showCoordinates(canvasX, canvasY);
    }

    private void pickColourAt(double canvasX, double canvasY) {
        int x = (int) (canvasX / zoom);
        int y = (int) (canvasY / zoom);
        if (x >= 0 && x < model.width() && y >= 0 && y < model.height()) {
            int picked = model.colourAt(x, y);
            ToggleButton matchingSwatch = paletteButtons.get(picked);
            if (matchingSwatch != null) {
                selectedColour = picked;
                matchingSwatch.setSelected(true);
            }
        }
        showCoordinates(canvasX, canvasY);
        selectTool(Tool.PAINT);
    }

    private void selectTool(Tool selected) {
        tool = selected;
        paintTool.setSelected(selected == Tool.PAINT);
        eyedropperTool.setSelected(selected == Tool.EYEDROPPER);
        resetPaintPosition();
    }

    private void resetPaintPosition() {
        lastPaintX = -1;
        lastPaintY = -1;
    }

    private void showCoordinates(double canvasX, double canvasY) {
        int x = (int) (canvasX / zoom);
        int y = (int) (canvasY / zoom);
        coordinates.setText(x >= 0 && x < model.width() && y >= 0 && y < model.height()
                ? "Пиксель: " + x + ", " + y : "Пиксель: —");
    }

    private void changeZoom(int delta) {
        int index = zoomBox.getItems().indexOf(zoom);
        int next = Math.max(0, Math.min(zoomBox.getItems().size() - 1, index + delta));
        zoomBox.setValue(zoomBox.getItems().get(next));
    }

    private void setZoom(int value) {
        zoom = value;
        resetPaintPosition();
        redrawAll();
    }

    private void redrawAll() {
        canvas.setWidth((double) model.width() * zoom);
        canvas.setHeight((double) model.height() * zoom);
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.setImageSmoothing(false);
        graphics.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        for (int y = 0; y < model.height(); y++) {
            for (int x = 0; x < model.width(); x++) drawPixel(x, y);
        }
    }

    private void drawPixel(int x, int y) {
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.setFill(toFxColour(model.colourAt(x, y)));
        graphics.fillRect((double) x * zoom, (double) y * zoom, zoom, zoom);
        if (zoom >= 8) {
            graphics.setStroke(Color.rgb(0, 0, 0, 0.16));
            graphics.setLineWidth(1);
            graphics.strokeRect((double) x * zoom + 0.5, (double) y * zoom + 0.5,
                    zoom - 1, zoom - 1);
        }
    }

    private int initialZoom(int maximumZoom) {
        int horizontal = Math.max(1, 780 / model.width());
        int vertical = Math.max(1, 560 / model.height());
        return Math.min(maximumZoom, Math.min(horizontal, vertical));
    }

    private static ToggleButton colourButton(int argb) {
        Rectangle sample = new Rectangle(26, 26, toFxColour(argb));
        sample.setStroke(Color.rgb(70, 70, 70));
        ToggleButton button = new ToggleButton("", sample);
        button.setPadding(new Insets(3));
        button.setTooltip(new Tooltip(String.format("#%06X", argb & 0xffffff)));
        return button;
    }

    private static Color toFxColour(int argb) {
        return Color.rgb((argb >>> 16) & 0xff, (argb >>> 8) & 0xff, argb & 0xff,
                ((argb >>> 24) & 0xff) / 255.0);
    }

    private static Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private static final class ZoomListCell extends ListCell<Integer> {
        @Override
        protected void updateItem(Integer value, boolean empty) {
            super.updateItem(value, empty);
            setText(empty || value == null ? null : value + "×");
        }
    }

    private enum Tool {
        PAINT,
        EYEDROPPER
    }
}
