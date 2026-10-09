package com.pixelazer.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Replaces generated palette entries and remaps the current result losslessly. */
final class PaletteEditorDialog {
    private final Stage stage = new Stage();
    private final BufferedImage source;
    private final List<Integer> originalPalette;
    private final List<ColorPicker> editors = new ArrayList<>();
    private boolean accepted;

    PaletteEditorDialog(Window owner, BufferedImage source, List<Integer> palette) {
        this.source = source;
        this.originalPalette = List.copyOf(palette);
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Управление палитрой");

        VBox rows = new VBox(8);
        rows.setPadding(new Insets(12));
        for (int i = 0; i < palette.size(); i++) {
            int colour = palette.get(i);
            Rectangle oldSample = new Rectangle(34, 34, toFxColour(colour));
            oldSample.setStroke(Color.web("#555555"));
            ColorPicker editor = new ColorPicker(toFxColour(colour));
            editor.setPrefWidth(150);
            editors.add(editor);
            Label number = new Label((i + 1) + ".");
            HBox row = new HBox(10, number, oldSample,
                    new Label(String.format("#%06X  →", colour & 0xffffff)), editor);
            row.setAlignment(Pos.CENTER_LEFT);
            rows.getChildren().add(row);
        }

        ScrollPane scroll = new ScrollPane(rows);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(Math.min(520, 58 * palette.size()));
        Button cancel = new Button("Отмена");
        cancel.setOnAction(event -> stage.close());
        Button apply = new Button("Применить палитру");
        apply.setDefaultButton(true);
        apply.setOnAction(event -> {
            accepted = true;
            stage.close();
        });
        HBox buttons = new HBox(10, cancel, apply);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(10));
        VBox root = new VBox(scroll, buttons);
        stage.setScene(new Scene(root, 430, Math.min(680, 130 + 58 * palette.size())));
    }

    Optional<Result> showAndWait() {
        stage.showAndWait();
        if (!accepted) return Optional.empty();
        List<Integer> replacements = editors.stream().map(editor -> toArgb(editor.getValue())).toList();
        BufferedImage remapped = PaletteRemapper.remap(source, originalPalette, replacements);
        return Optional.of(new Result(remapped, replacements.stream().distinct().toList()));
    }

    private static Color toFxColour(int argb) {
        return Color.rgb((argb >>> 16) & 0xff, (argb >>> 8) & 0xff, argb & 0xff,
                ((argb >>> 24) & 0xff) / 255.0);
    }

    private static int toArgb(Color colour) {
        return ((int) Math.round(colour.getOpacity() * 255) << 24)
                | ((int) Math.round(colour.getRed() * 255) << 16)
                | ((int) Math.round(colour.getGreen() * 255) << 8)
                | (int) Math.round(colour.getBlue() * 255);
    }

    record Result(BufferedImage image, List<Integer> palette) {}
}
