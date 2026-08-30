package com.seuprojeto;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class MediaCard extends StackPane {

    public MediaCard(String title, String imageUrl, String mediaUrl, Runnable onSelect) {
        this.setPrefSize(160, 240);
        this.setStyle("-fx-cursor: hand;");

        // Container principal do Card
        VBox cardContent = new VBox(10);
        cardContent.setAlignment(Pos.TOP_CENTER);

        // Container da Imagem com Clip e Sombra
        StackPane imageWrapper = new StackPane();
        imageWrapper.setPrefSize(160, 220);
        
        Rectangle placeholder = new Rectangle(160, 220);
        placeholder.setArcWidth(20);
        placeholder.setArcHeight(20);
        placeholder.setFill(Color.web("#1A1A1A"));
        
        // Sombra suave
        DropShadow shadow = new DropShadow();
        shadow.setRadius(10);
        shadow.setOffsetY(5);
        shadow.setColor(Color.rgb(0, 0, 0, 0.5));
        imageWrapper.setEffect(shadow);

        ImageView poster = new ImageView();
        poster.setFitWidth(160);
        poster.setFitHeight(220);
        
        Rectangle clip = new Rectangle(160, 220);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        poster.setClip(clip);

        if (imageUrl != null && imageUrl.startsWith("http")) {
            Image img = new Image(imageUrl, 160, 220, true, true, true);
            poster.setImage(img);
            poster.setOpacity(0);
            img.progressProperty().addListener((obs, old, val) -> {
                if (val.doubleValue() == 1.0) {
                    FadeTransition ft = new FadeTransition(Duration.millis(400), poster);
                    ft.setToValue(1.0);
                    ft.play();
                }
            });
        }

        // Overlay de Play ao passar o mouse
        StackPane playOverlay = new StackPane();
        playOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.4); -fx-background-radius: 10;");
        playOverlay.setOpacity(0);
        Label playIcon = new Label("▶");
        playIcon.setStyle("-fx-text-fill: white; -fx-font-size: 40px;");
        playOverlay.getChildren().add(playIcon);

        imageWrapper.getChildren().addAll(placeholder, poster, playOverlay);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 0 5 0 5;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(150);
        titleLabel.setAlignment(Pos.CENTER);

        cardContent.getChildren().addAll(imageWrapper, titleLabel);
        this.getChildren().add(cardContent);

        // Efeitos de Interação
        this.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), this);
            st.setToX(1.1);
            st.setToY(1.1);
            st.play();
            playOverlay.setOpacity(1);
            shadow.setRadius(20);
        });

        this.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), this);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
            playOverlay.setOpacity(0);
            shadow.setRadius(10);
        });

        this.setOnMouseClicked(e -> onSelect.run());
    }
}
