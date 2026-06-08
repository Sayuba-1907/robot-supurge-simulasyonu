package com.example.robotvacuumsimulation;

import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import java.util.function.Consumer;

public class RoomSelectionScreen extends StackPane {

    public RoomSelectionScreen(Consumer<RoomType> onSelect) {
        setStyle("-fx-background-color: #0d1117;");

        VBox content = new VBox(40);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(50, 60, 50, 60));

        Label icon     = new Label("🤖");
        icon.setFont(Font.font("System", 42));
        Label title    = new Label("Robot Süpürge Simülasyonu");
        title.setFont(Font.font("System", FontWeight.BOLD, 28));
        title.setTextFill(Color.WHITE);
        Label subtitle = new Label("Temizlenecek odayı seçin");
        subtitle.setFont(Font.font("System", 16));
        subtitle.setTextFill(Color.web("#8b949e"));
        VBox titleBox = new VBox(8, icon, title, subtitle);
        titleBox.setAlignment(Pos.CENTER);

        HBox cardsRow = new HBox(20);
        cardsRow.setAlignment(Pos.CENTER);
        for (RoomType type : RoomType.values()) {
            cardsRow.getChildren().add(createCard(type, onSelect));
        }

        Label note = new Label(
            "🏠 Salon seçildiğinde robot doğrudan salonu temizler.\n"
          + "Diğer odalar seçildiğinde robot salondan kapıdan geçerek o odaya gider, temizler ve şarj için geri döner."
        );
        note.setTextFill(Color.web("#6e7681"));
        note.setFont(Font.font("System", 12));
        note.setWrapText(true);
        note.setTextAlignment(TextAlignment.CENTER);
        note.setMaxWidth(780);

        content.getChildren().addAll(titleBox, cardsRow, note);
        getChildren().add(content);
    }

    private VBox createCard(RoomType type, Consumer<RoomType> onSelect) {
        String accentColor = accentFor(type);

        VBox card = new VBox(12);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(20, 18, 20, 18));
        card.setMinWidth(200);
        card.setMaxWidth(200);
        card.setMinHeight(260);
        card.setStyle(cardStyle(accentColor, false));

        Canvas preview = new Canvas(164, 83);
        drawMiniPreview(preview.getGraphicsContext2D(), type);

        Label iconLabel = new Label(type.icon);
        iconLabel.setFont(Font.font("System", 30));

        Label name = new Label(type.displayName);
        name.setFont(Font.font("System", FontWeight.BOLD, 14));
        name.setTextFill(Color.WHITE);

        Label desc = new Label(type.description);
        desc.setFont(Font.font("System", 11));
        desc.setTextFill(Color.web("#8b949e"));
        desc.setWrapText(true);
        desc.setTextAlignment(TextAlignment.CENTER);
        desc.setMaxWidth(170);

        card.getChildren().addAll(preview, iconLabel, name, desc);

        card.setOnMouseEntered(e -> card.setStyle(cardStyle(accentColor, true)));
        card.setOnMouseExited(e  -> card.setStyle(cardStyle(accentColor, false)));

        card.setOnMouseClicked(e -> {
            ScaleTransition shrink = new ScaleTransition(Duration.millis(90), card);
            shrink.setToX(0.94); shrink.setToY(0.94);
            shrink.setOnFinished(ev -> {
                ScaleTransition grow = new ScaleTransition(Duration.millis(90), card);
                grow.setToX(1.0); grow.setToY(1.0);
                grow.setOnFinished(ev2 -> onSelect.accept(type));
                grow.play();
            });
            shrink.play();
        });

        return card;
    }

    private String cardStyle(String accent, boolean hovered) {
        String bg     = hovered ? "#21262d" : "#161b22";
        String border = hovered ? accent    : "#30363d";
        String bw     = hovered ? "2"       : "1";
        String shadow = hovered ? "; -fx-effect: dropshadow(gaussian, " + accent + ", 12, 0.35, 0, 0)" : "";
        return "-fx-background-color: " + bg + "; -fx-background-radius: 12; "
             + "-fx-border-color: "  + border + "; -fx-border-radius: 12; "
             + "-fx-border-width: "  + bw + "; -fx-cursor: hand" + shadow + ";";
    }

    private String accentFor(RoomType type) {
        return switch (type) {
            case SALON   -> "#3fb950";
            case BEDROOM -> "#a371f7";
            case OFFICE  -> "#d29922";
        };
    }

    private void drawMiniPreview(GraphicsContext gc, RoomType type) {
        double cw = 164.0 / 20.0;
        double ch = 83.0  / 14.0;

        gc.setFill(Color.web("#0d1117")); gc.fillRect(0, 0, 164, 83);
        gc.setFill(Color.web("#d4c4a8")); gc.fillRect(cw, ch, 18*cw, 12*ch);

        gc.setFill(Color.web("#374151"));
        gc.fillRect(0, 0, 164, ch);
        gc.fillRect(0, 13*ch, 164, ch);
        gc.fillRect(0, 0, cw, 83);
        gc.fillRect(19*cw, 0, cw, 83);

        gc.setFill(Color.web("#d4c4a8"));
        gc.fillRect(11*cw, 13*ch, 3*cw, ch);

        Color furniColor = Color.web("#8b6d3e");

        switch (type) {
            case SALON -> {
                gc.setFill(furniColor);
                gc.fillRect(7*cw,  ch,   6*cw, 2*ch);
                gc.fillRect(2*cw,  4*ch, 2*cw, 6*ch);
                gc.fillRect(8*cw,  6*ch, 3*cw, 3*ch);
                gc.fillRect(15*cw, 6*ch, 3*cw, 3*ch);
                gc.fillRect(15*cw, ch,   3*cw, ch);
                gc.setFill(Color.web("#3fb950", 0.85));
                gc.fillRoundRect(0.5*cw, 11.5*ch, 2*cw, 1.2*ch, 2, 2);
            }
            case BEDROOM -> {
                gc.setFill(furniColor);
                gc.fillRect(2*cw,  2*ch, 2*cw, 4*ch);
                gc.fillRect(16*cw, 2*ch, 2*cw, 4*ch);
                gc.fillRect(2*cw,  9*ch, 3*cw, 2*ch);
                gc.fillRect(15*cw, 9*ch, 3*cw, 2*ch);
                gc.setFill(Color.web("#6b8fa8"));
                gc.fillRect(7*cw, 2*ch, 4*cw, 3*ch);
                gc.setFill(Color.web("#4a2c1a"));
                gc.fillRect(7*cw, 2*ch, 4*cw, 0.5*ch);
                gc.setFill(Color.web("#f0e8e0"));
                gc.fillRect(7*cw+2, 2.6*ch, 1.7*cw, 0.9*ch);
                gc.fillRect(9*cw+1, 2.6*ch, 1.7*cw, 0.9*ch);
                gc.setFill(Color.web("#a371f7", 0.5));
                gc.fillRoundRect(0.5*cw, 11.5*ch, 2*cw, 1.2*ch, 2, 2);
            }
            case OFFICE -> {
                gc.setFill(Color.web("#4b5563"));
                for (int y = 1; y <= 3; y++) gc.fillRect(10*cw, y*ch, cw, ch);
                for (int y = 5; y <= 9; y++) gc.fillRect(10*cw, y*ch, cw, ch);
                gc.setFill(furniColor);
                gc.fillRect(2*cw,  2*ch, 3*cw, 2*ch);
                gc.fillRect(2*cw,  7*ch, 3*cw, 2*ch);
                gc.fillRect(6*cw,  2*ch, 3*cw, 2*ch);
                gc.fillRect(6*cw,  7*ch, 3*cw, 2*ch);
                gc.fillRect(12*cw, 2*ch, 3*cw, 2*ch);
                gc.fillRect(12*cw, 7*ch, 3*cw, 2*ch);
                gc.fillRect(16*cw, 2*ch, 2*cw, 2*ch);
                gc.fillRect(16*cw, 7*ch, 2*cw, 2*ch);
                gc.setFill(Color.web("#5c4a2e"));
                gc.fillRect(2*cw,  10*ch, 3*cw, 2*ch);
                gc.fillRect(14*cw, 10*ch, 3*cw, 2*ch);
                gc.setFill(Color.web("#d29922", 0.5));
                gc.fillRoundRect(0.5*cw, 11.5*ch, 2*cw, 1.2*ch, 2, 2);
            }
        }

        gc.setStroke(Color.web(accentFor(type), 0.7));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(1, 1, 162, 81, 4, 4);
    }
}
