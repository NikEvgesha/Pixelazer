package com.pixelazer.ui;

import com.pixelazer.core.*;
import com.pixelazer.io.IndexedPngWriter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class PixelazerApp extends Application {
    private final PixelArtConverter converter = new PixelArtConverter();
    private final ImageView sourceView = sourceImageView();
    private final ImageView resultView = pixelImageView();
    private final Spinner<Integer> width = new Spinner<>(1, 1024, 64);
    private final Spinner<Integer> height = new Spinner<>(1, 1024, 64);
    private final Spinner<Integer> colours = new Spinner<>(2, 32, 16);
    private final ComboBox<ProcessingMode> mode = new ComboBox<>();
    private final ComboBox<DetailLevel> detailLevel = new ComboBox<>();
    private final ComboBox<OutlineSelection> outlineSelection = new ComboBox<>();
    private final ComboBox<OutlineThickness> outlineThickness = new ComboBox<>();
    private final ComboBox<DitheringMode> ditheringMode = new ComboBox<>();
    private final ComboBox<GridAlignmentMode> gridAlignmentMode = new ComboBox<>();
    private final Slider ditheringStrength = new Slider(0, 1, 0.65);
    private final Slider gridOffsetX = new Slider(-0.5, 0.5, 0);
    private final Slider gridOffsetY = new Slider(-0.5, 0.5, 0);
    private final ColorPicker manualOutlineColour = new ColorPicker(Color.BLACK);
    private final ColorPicker lockedColourPicker = new ColorPicker(Color.WHITE);
    private final FlowPane lockedPalettePane = new FlowPane(5, 5);
    private final List<Integer> lockedPaletteColours = new ArrayList<>();
    private final CheckBox closeOutlineGaps = new CheckBox("Замыкать небольшие разрывы");
    private final Slider edgeStrength = new Slider(0, 16, 8);
    private final CheckBox keepRatio = new CheckBox("Сохранять пропорции");
    private final CheckBox cleanup = new CheckBox("Убирать одиночные пиксели");
    private final FlowPane palettePane = new FlowPane(5, 5);
    private final Label status = new Label("Откройте PNG или JPEG");
    private final Button convertButton = new Button("Преобразовать");
    private final Button compareButton = new Button("Сравнить режимы");
    private final Button editButton = new Button("Редактировать результат");
    private final Button paletteEditButton = new Button("Редактировать палитру");
    private final Button saveButton = new Button("Сохранить PNG");
    private final Button lockedEyedropperButton = new Button("Пипетка с изображения");
    private BufferedImage source;
    private BufferedImage result;
    private List<Integer> currentPalette = List.of();
    private boolean changingRatio;

    @Override
    public void start(Stage stage) {
        keepRatio.setSelected(true);
        cleanup.setSelected(true);
        width.setEditable(true);
        height.setEditable(true);
        colours.setEditable(true);
        mode.getItems().setAll(ProcessingMode.values());
        mode.setValue(ProcessingMode.AUTO);
        mode.setMaxWidth(Double.MAX_VALUE);
        detailLevel.getItems().setAll(DetailLevel.values());
        detailLevel.setValue(DetailLevel.BALANCED);
        detailLevel.setMaxWidth(Double.MAX_VALUE);
        outlineSelection.getItems().setAll(OutlineSelection.values());
        outlineSelection.setValue(OutlineSelection.AUTO);
        outlineSelection.setMaxWidth(Double.MAX_VALUE);
        outlineThickness.getItems().setAll(OutlineThickness.values());
        outlineThickness.setValue(OutlineThickness.NORMAL);
        outlineThickness.setMaxWidth(Double.MAX_VALUE);
        ditheringMode.getItems().setAll(DitheringMode.values());
        ditheringMode.setValue(DitheringMode.OFF);
        ditheringMode.setMaxWidth(Double.MAX_VALUE);
        ditheringStrength.setShowTickLabels(true);
        ditheringStrength.setMajorTickUnit(0.25);
        ditheringStrength.setBlockIncrement(0.1);
        ditheringStrength.setDisable(true);
        ditheringMode.valueProperty().addListener((observable, oldValue, newValue) ->
                ditheringStrength.setDisable(newValue == DitheringMode.OFF));
        gridAlignmentMode.getItems().setAll(GridAlignmentMode.values());
        gridAlignmentMode.setValue(GridAlignmentMode.AUTO);
        gridAlignmentMode.setMaxWidth(Double.MAX_VALUE);
        configureOffsetSlider(gridOffsetX);
        configureOffsetSlider(gridOffsetY);
        manualOutlineColour.setDisable(true);
        outlineSelection.valueProperty().addListener((observable, oldValue, newValue) ->
                manualOutlineColour.setDisable(newValue != OutlineSelection.MANUAL));
        closeOutlineGaps.setSelected(true);
        edgeStrength.setShowTickLabels(true);
        edgeStrength.setShowTickMarks(true);
        edgeStrength.setMajorTickUnit(4);
        edgeStrength.setBlockIncrement(1);

        Button openButton = new Button("Открыть изображение");
        openButton.setOnAction(event -> open(stage));
        convertButton.setDisable(true);
        convertButton.setOnAction(event -> convert());
        editButton.setDisable(true);
        editButton.setOnAction(event -> edit(stage));
        paletteEditButton.setDisable(true);
        paletteEditButton.setOnAction(event -> editPalette(stage));
        saveButton.setDisable(true);
        saveButton.setOnAction(event -> save(stage));
        Button addLockedColour = new Button("Зафиксировать цвет");
        addLockedColour.setOnAction(event -> addLockedColour());
        lockedEyedropperButton.setDisable(true);
        lockedEyedropperButton.setOnAction(event -> pickLockedColour(stage));
        compareButton.setDisable(true);
        compareButton.setOnAction(event -> compareModes(stage, compareButton));
        width.valueProperty().addListener((obs, oldValue, newValue) -> syncHeight(newValue));
        height.valueProperty().addListener((obs, oldValue, newValue) -> syncWidth(newValue));
        colours.valueProperty().addListener((obs, oldValue, newValue) -> {
            while (lockedPaletteColours.size() > newValue) {
                lockedPaletteColours.remove(lockedPaletteColours.size() - 1);
            }
            renderLockedColours();
        });

        VBox manualOutlineSetting = labelled("Ручной цвет контура", manualOutlineColour);
        setVisibleManaged(manualOutlineSetting, false);
        outlineSelection.valueProperty().addListener((observable, oldValue, newValue) ->
                setVisibleManaged(manualOutlineSetting, newValue == OutlineSelection.MANUAL));
        VBox outlineSettings = new VBox(9,
                labelled("Уровень детализации", detailLevel),
                labelled("Определение контура", outlineSelection),
                manualOutlineSetting,
                labelled("Толщина контура", outlineThickness),
                closeOutlineGaps);
        outlineSettings.setPadding(new Insets(8, 4, 4, 4));
        TitledPane outlinePane = new TitledPane("Настройки контуров", outlineSettings);
        outlinePane.setExpanded(false);
        outlinePane.setAnimated(false);

        VBox ditheringStrengthSetting = labelled("Интенсивность дизеринга", ditheringStrength);
        setVisibleManaged(ditheringStrengthSetting, false);
        ditheringMode.valueProperty().addListener((observable, oldValue, newValue) ->
                setVisibleManaged(ditheringStrengthSetting, newValue != DitheringMode.OFF));
        VBox photoSettings = new VBox(9,
                labelled("Сохранение контуров", edgeStrength),
                labelled("Дизеринг", ditheringMode),
                ditheringStrengthSetting);
        photoSettings.setPadding(new Insets(8, 4, 4, 4));
        TitledPane photoPane = new TitledPane("Настройки фотографии", photoSettings);
        photoPane.setExpanded(false);
        photoPane.setAnimated(false);

        VBox paletteSettings = new VBox(8,
                new Label("Обязательные цвета палитры"),
                lockedColourPicker, addLockedColour, lockedEyedropperButton, lockedPalettePane);
        paletteSettings.setPadding(new Insets(8, 4, 4, 4));
        TitledPane lockedPaletteSettingsPane = new TitledPane("Фиксированные цвета", paletteSettings);
        lockedPaletteSettingsPane.setExpanded(false);
        lockedPaletteSettingsPane.setAnimated(false);

        VBox manualGridSettings = new VBox(8,
                labelled("Смещение X", gridOffsetX), labelled("Смещение Y", gridOffsetY));
        setVisibleManaged(manualGridSettings, false);
        gridAlignmentMode.valueProperty().addListener((observable, oldValue, newValue) ->
                setVisibleManaged(manualGridSettings, newValue == GridAlignmentMode.MANUAL));
        VBox gridSettings = new VBox(8,
                labelled("Выравнивание", gridAlignmentMode), manualGridSettings);
        gridSettings.setPadding(new Insets(8, 4, 4, 4));
        TitledPane gridPane = new TitledPane("Пиксельная сетка", gridSettings);
        gridPane.setExpanded(false);
        gridPane.setAnimated(false);

        mode.valueProperty().addListener((observable, oldValue, newValue) ->
                updateModeSettings(newValue, photoPane, outlinePane));
        updateModeSettings(mode.getValue(), photoPane, outlinePane);

        VBox controls = new VBox(10,
                openButton,
                labelled("Ширина", width),
                labelled("Высота", height),
                keepRatio,
                labelled("Цветов в палитре", colours),
                labelled("Тип изображения", mode),
                cleanup,
                photoPane,
                outlinePane,
                gridPane,
                lockedPaletteSettingsPane,
                convertButton,
                compareButton,
                editButton,
                paletteEditButton,
                saveButton,
                new Separator(),
                new Label("Палитра"),
                palettePane);
        controls.setPadding(new Insets(16));
        controls.setPrefWidth(260);
        ScrollPane controlsScroll = new ScrollPane(controls);
        controlsScroll.setFitToWidth(true);
        controlsScroll.setPrefWidth(280);
        controlsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        SplitPane previews = new SplitPane(preview("Оригинал", sourceView), preview("Результат", resultView));
        previews.setDividerPositions(0.5);
        BorderPane root = new BorderPane(previews);
        root.setLeft(controlsScroll);
        root.setBottom(status);
        BorderPane.setMargin(status, new Insets(8, 16, 12, 16));

        Scene scene = new Scene(root, 1100, 700);
        stage.setTitle("Pixelazer — edge-aware pixel art");
        stage.setMinWidth(800);
        stage.setMinHeight(520);
        stage.setScene(scene);
        stage.show();
    }

    private void open(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Открыть изображение");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Изображения PNG/JPEG", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;
        try {
            BufferedImage loaded = ImageIO.read(file);
            if (loaded == null) throw new IOException("Формат изображения не распознан");
            source = loaded;
            sourceView.setImage(SwingFXUtils.toFXImage(source, null));
            changingRatio = true;
            width.getValueFactory().setValue(Math.min(128, source.getWidth()));
            height.getValueFactory().setValue(Math.max(1, (int) Math.round(
                    width.getValue() * source.getHeight() / (double) source.getWidth())));
            changingRatio = false;
            result = null;
            currentPalette = List.of();
            resultView.setImage(null);
            palettePane.getChildren().clear();
            convertButton.setDisable(false);
            compareButton.setDisable(false);
            lockedEyedropperButton.setDisable(false);
            editButton.setDisable(true);
            paletteEditButton.setDisable(true);
            saveButton.setDisable(true);
            status.setText(file.getName() + " — " + source.getWidth() + "×" + source.getHeight());
        } catch (IOException exception) {
            showError("Не удалось открыть изображение", exception);
        }
    }

    private void convert() {
        if (source == null) return;
        ProcessingOptions options;
        try {
            options = createOptions(mode.getValue());
        } catch (IllegalArgumentException exception) {
            showError("Некорректные параметры", exception);
            return;
        }
        convertButton.setDisable(true);
        compareButton.setDisable(true);
        editButton.setDisable(true);
        paletteEditButton.setDisable(true);
        saveButton.setDisable(true);
        status.setText("Обработка…");
        BufferedImage input = source;
        CompletableFuture.supplyAsync(() -> converter.convert(input, options))
                .whenComplete((converted, error) -> Platform.runLater(() -> {
                    convertButton.setDisable(false);
                    compareButton.setDisable(false);
                    if (error != null) {
                        showError("Ошибка преобразования", error);
                        status.setText("Преобразование не выполнено");
                        return;
                    }
                    result = converted.image();
                    currentPalette = converted.palette();
                    updateResultPreview();
                    showPalette(currentPalette);
                    editButton.setDisable(false);
                    paletteEditButton.setDisable(false);
                    saveButton.setDisable(false);
                    status.setText("Готово: " + result.getWidth() + "×" + result.getHeight()
                            + ", цветов: " + converted.palette().size()
                            + ", режим: " + converted.resolvedMode().displayName()
                            + String.format(", сетка: %.1f / %.1f",
                            converted.gridOffsetX(), converted.gridOffsetY()));
                }));
    }

    private ProcessingOptions createOptions(ProcessingMode selectedMode) {
        Color outline = manualOutlineColour.getValue();
        int outlineArgb = toArgb(outline);
        return new ProcessingOptions(width.getValue(), height.getValue(), colours.getValue(),
                edgeStrength.getValue(), cleanup.isSelected(), selectedMode,
                detailLevel.getValue(), outlineSelection.getValue(), outlineArgb,
                outlineThickness.getValue(), closeOutlineGaps.isSelected(),
                ditheringMode.getValue(), ditheringStrength.getValue(), lockedPaletteColours,
                gridAlignmentMode.getValue(), gridOffsetX.getValue(), gridOffsetY.getValue());
    }

    private void compareModes(Stage owner, Button compareButton) {
        if (source == null) return;
        compareButton.setDisable(true);
        convertButton.setDisable(true);
        status.setText("Создание вариантов для сравнения…");
        BufferedImage input = source;
        List<ProcessingMode> modes = List.of(ProcessingMode.PHOTO,
                ProcessingMode.ILLUSTRATION, ProcessingMode.LINE_ART);
        List<ProcessingOptions> options;
        try {
            options = modes.stream().map(this::createOptions).toList();
        } catch (IllegalArgumentException exception) {
            compareButton.setDisable(false);
            convertButton.setDisable(false);
            showError("Некорректные параметры", exception);
            return;
        }
        CompletableFuture.supplyAsync(() -> options.stream()
                        .map(option -> converter.convert(input, option)).toList())
                .whenComplete((variants, error) -> Platform.runLater(() -> {
                    compareButton.setDisable(false);
                    convertButton.setDisable(false);
                    if (error != null) {
                        showError("Не удалось сравнить режимы", error);
                        status.setText("Сравнение не выполнено");
                        return;
                    }
                    new ModeComparisonDialog(owner, variants).showAndWait().ifPresent(chosen -> {
                        result = chosen.result().image();
                        currentPalette = chosen.result().palette();
                        mode.setValue(chosen.result().resolvedMode());
                        updateResultPreview();
                        showPalette(currentPalette);
                        editButton.setDisable(false);
                        paletteEditButton.setDisable(false);
                        saveButton.setDisable(false);
                        status.setText("Выбран режим: " + chosen.result().resolvedMode().displayName());
                    });
                }));
    }

    private void addLockedColour() {
        addLockedColour(toArgb(lockedColourPicker.getValue()));
    }

    private void pickLockedColour(Stage owner) {
        if (source == null) return;
        new ImageColourPickerDialog(owner, source).showAndWait().ifPresent(colour -> {
            lockedColourPicker.setValue(Color.rgb(
                    (colour >>> 16) & 0xff, (colour >>> 8) & 0xff, colour & 0xff));
            addLockedColour(colour);
        });
    }

    private void addLockedColour(int colour) {
        if (lockedPaletteColours.contains(colour)) {
            status.setText("Этот цвет уже зафиксирован");
            return;
        }
        if (lockedPaletteColours.size() >= colours.getValue()) {
            status.setText("Число фиксированных цветов не может превышать размер палитры");
            return;
        }
        lockedPaletteColours.add(colour);
        renderLockedColours();
    }

    private void renderLockedColours() {
        lockedPalettePane.getChildren().clear();
        for (int colour : List.copyOf(lockedPaletteColours)) {
            Region swatch = new Region();
            swatch.setPrefSize(22, 22);
            swatch.setStyle(String.format("-fx-background-color: #%06X; -fx-border-color: #555;",
                    colour & 0xffffff));
            Button remove = new Button("×", swatch);
            remove.setTooltip(new Tooltip(String.format("Удалить #%06X", colour & 0xffffff)));
            remove.setOnAction(event -> {
                lockedPaletteColours.remove(Integer.valueOf(colour));
                renderLockedColours();
            });
            lockedPalettePane.getChildren().add(remove);
        }
    }

    private static void updateModeSettings(ProcessingMode selectedMode,
                                           TitledPane photoPane, TitledPane outlinePane) {
        boolean photo = selectedMode == ProcessingMode.AUTO || selectedMode == ProcessingMode.PHOTO;
        boolean outlines = selectedMode == ProcessingMode.LINE_ART;
        setVisibleManaged(photoPane, photo);
        setVisibleManaged(outlinePane, outlines);
    }

    private static void setVisibleManaged(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private static int toArgb(Color colour) {
        return 0xff000000 | ((int) Math.round(colour.getRed() * 255) << 16)
                | ((int) Math.round(colour.getGreen() * 255) << 8)
                | (int) Math.round(colour.getBlue() * 255);
    }

    private void edit(Stage owner) {
        if (result == null || currentPalette.isEmpty()) return;
        new PixelEditorDialog(owner, result, currentPalette).showAndWait().ifPresent(edited -> {
            result = edited;
            updateResultPreview();
            saveButton.setDisable(false);
            status.setText("Изменения редактора применены");
        });
    }

    private void editPalette(Stage owner) {
        if (result == null || currentPalette.isEmpty()) return;
        new PaletteEditorDialog(owner, result, currentPalette).showAndWait().ifPresent(edited -> {
            result = edited.image();
            currentPalette = edited.palette();
            updateResultPreview();
            showPalette(currentPalette);
            status.setText("Изменения палитры применены");
        });
    }

    private void updateResultPreview() {
        BufferedImage preview = PixelPreviewScaler.scale(result, 480, 600);
        resultView.setImage(SwingFXUtils.toFXImage(preview, null));
    }

    private void save(Stage stage) {
        if (result == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить пиксель-арт");
        chooser.setInitialFileName("pixel-art.png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        if (!file.getName().toLowerCase().endsWith(".png")) file = new File(file + ".png");
        try {
            IndexedPngWriter.write(result, currentPalette, file);
            status.setText("Сохранено: " + file.getAbsolutePath());
        } catch (IOException exception) {
            showError("Не удалось сохранить PNG", exception);
        }
    }

    private void showPalette(java.util.List<Integer> palette) {
        palettePane.getChildren().clear();
        for (int argb : palette) {
            Region swatch = new Region();
            swatch.setPrefSize(28, 28);
            swatch.setStyle(String.format("-fx-background-color: #%06X; -fx-border-color: #555;",
                    argb & 0xffffff));
            Tooltip.install(swatch, new Tooltip(String.format("#%06X", argb & 0xffffff)));
            palettePane.getChildren().add(swatch);
        }
    }

    private void syncHeight(int newWidth) {
        if (!changingRatio && keepRatio.isSelected() && source != null) {
            changingRatio = true;
            height.getValueFactory().setValue(Math.max(1, Math.min(1024,
                    (int) Math.round(newWidth * source.getHeight() / (double) source.getWidth()))));
            changingRatio = false;
        }
    }

    private void syncWidth(int newHeight) {
        if (!changingRatio && keepRatio.isSelected() && source != null) {
            changingRatio = true;
            width.getValueFactory().setValue(Math.max(1, Math.min(1024,
                    (int) Math.round(newHeight * source.getWidth() / (double) source.getHeight()))));
            changingRatio = false;
        }
    }

    private static VBox labelled(String label, javafx.scene.Node node) {
        return new VBox(4, new Label(label), node);
    }

    private static void configureOffsetSlider(Slider slider) {
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(0.25);
        slider.setBlockIncrement(0.1);
        slider.setSnapToTicks(false);
    }

    private static StackPane preview(String title, ImageView view) {
        Label label = new Label(title);
        label.setStyle("-fx-background-color: rgba(20,20,20,.75); -fx-text-fill: white; -fx-padding: 5 10;");
        StackPane pane = new StackPane(view, label);
        pane.setStyle("-fx-background-color: #2b2b2b;");
        StackPane.setAlignment(label, Pos.TOP_LEFT);
        return pane;
    }

    private static ImageView sourceImageView() {
        ImageView view = new ImageView();
        view.setPreserveRatio(true);
        view.setSmooth(false);
        view.setFitWidth(400);
        view.setFitHeight(620);
        return view;
    }

    private static ImageView pixelImageView() {
        ImageView view = new ImageView();
        // No fitWidth/fitHeight: the supplied preview already has an exact integer
        // scale, so JavaFX never performs a second fractional interpolation pass.
        view.setPreserveRatio(false);
        view.setSmooth(false);
        return view;
    }

    private static void showError(String title, Throwable throwable) {
        Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
        Alert alert = new Alert(Alert.AlertType.ERROR, cause.getMessage(), ButtonType.OK);
        alert.setHeaderText(title);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
