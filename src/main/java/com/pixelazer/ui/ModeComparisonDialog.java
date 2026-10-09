package com.pixelazer.ui;

import com.pixelazer.core.PixelArtConverter;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

final class ModeComparisonDialog {
    private final Stage stage = new Stage();
    private Choice choice;

    ModeComparisonDialog(Window owner, List<PixelArtConverter.ConversionResult> variants) {
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Сравнение режимов");
        stage.setMinWidth(800);
        stage.setMinHeight(520);

        HBox cards = new HBox(14);
        cards.setPadding(new Insets(16));
        cards.setAlignment(Pos.CENTER);
        for (var result : variants) {
            BufferedImage preview = PixelPreviewScaler.scale(result.image(), 300, 440);
            ImageView view = new ImageView(SwingFXUtils.toFXImage(preview, null));
            view.setSmooth(false);
            StackPane imageHolder = new StackPane(view);
            imageHolder.setMinSize(300, 440);
            imageHolder.setStyle("-fx-background-color: #252525;");

            Button select = new Button("Выбрать этот вариант");
            select.setMaxWidth(Double.MAX_VALUE);
            select.setOnAction(event -> {
                choice = new Choice(result);
                stage.close();
            });
            VBox card = new VBox(9,
                    new Label(result.resolvedMode().displayName()), imageHolder, select);
            card.setAlignment(Pos.TOP_CENTER);
            card.setPadding(new Insets(10));
            card.setStyle("-fx-border-color: #777; -fx-border-radius: 4;");
            HBox.setHgrow(card, Priority.ALWAYS);
            cards.getChildren().add(card);
        }

        ScrollPane scroll = new ScrollPane(cards);
        scroll.setFitToHeight(true);
        stage.setScene(new Scene(scroll, 1080, 620));
    }

    Optional<Choice> showAndWait() {
        stage.showAndWait();
        return Optional.ofNullable(choice);
    }

    record Choice(PixelArtConverter.ConversionResult result) {}
}
