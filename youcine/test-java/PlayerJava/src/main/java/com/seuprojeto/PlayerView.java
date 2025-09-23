// Salve como: PlayerView.java
package com.seuprojeto;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurface;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.base.TrackDescription;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

public class PlayerView {
    private BorderPane view;
    private EmbeddedMediaPlayer mediaPlayer;
    private Label channelTitleLabel;
    private final Runnable onBackToList;

    public PlayerView(Runnable onBackToList) {
        this.onBackToList = onBackToList;
        createView();
    }

    public Pane getView() {
        return view;
    }

    private void createView() {
        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(false);
        
        ImageViewVideoSurface videoSurface = new ImageViewVideoSurface(imageView);
        
        MediaPlayerFactory factory = new MediaPlayerFactory();
        mediaPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer();
        mediaPlayer.videoSurface().set(videoSurface);
        
        mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void error(MediaPlayer mediaPlayer) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Erro de Reprodução", "Este stream pode estar offline ou corrompido.");
                    onBackToList.run();
                });
            }
        });

        StackPane videoContainer = new StackPane(imageView);
        videoContainer.setStyle("-fx-background-color: black;");
        
        VBox controlsContainer = createControlsBox();
        videoContainer.getChildren().add(controlsContainer);
        StackPane.setAlignment(controlsContainer, Pos.BOTTOM_CENTER);

        controlsContainer.setVisible(false);
        Timeline hideTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> controlsContainer.setVisible(false)));
        videoContainer.setOnMouseMoved(e -> {
            controlsContainer.setVisible(true);
            hideTimeline.playFromStart();
        });
        
        view = new BorderPane();
        view.setCenter(videoContainer);
        
        imageView.fitWidthProperty().bind(view.widthProperty());
        imageView.fitHeightProperty().bind(view.heightProperty());
    }
    
    private VBox createControlsBox() {
        Button playPauseButton = new Button("Pause");
        playPauseButton.setOnAction(e -> {
            mediaPlayer.controls().pause();
            playPauseButton.setText(mediaPlayer.status().isPlaying() ? "Pause" : "Play");
        });
        
        Label volumeLabel = new Label("Volume:");
        volumeLabel.setStyle("-fx-text-fill: white;");
        Slider volumeSlider = new Slider(0, 100, 80);
        volumeSlider.valueProperty().addListener((obs, oldV, newV) -> mediaPlayer.audio().setVolume(newV.intValue()));
        
        ComboBox<String> audioTrackBox = createTrackComboBox("Áudio", 
            () -> mediaPlayer.audio().trackDescriptions().stream().map(TrackDescription::description).collect(Collectors.toList()), 
            mediaPlayer.audio()::setTrack);
            
        ComboBox<String> subtitleTrackBox = createTrackComboBox("Legenda", 
            () -> mediaPlayer.subpictures().trackDescriptions().stream().map(TrackDescription::description).collect(Collectors.toList()), 
            mediaPlayer.subpictures()::setTrack);
        
        Button backButton = new Button("◀ Voltar");
        backButton.setOnAction(e -> onBackToList.run());

        channelTitleLabel = new Label();
        channelTitleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        HBox topControls = new HBox(15, backButton, channelTitleLabel);
        topControls.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(channelTitleLabel, Priority.ALWAYS);

        HBox mainControls = new HBox(10, playPauseButton, volumeLabel, volumeSlider, audioTrackBox, subtitleTrackBox);
        mainControls.setAlignment(Pos.CENTER);
        
        Slider brightnessSlider = createAdjustSlider(1.0f, mediaPlayer.video()::setBrightness);
        Slider contrastSlider = createAdjustSlider(1.0f, mediaPlayer.video()::setContrast);
        Slider saturationSlider = createAdjustSlider(1.0f, mediaPlayer.video()::setSaturation);

        HBox imageControls = new HBox(10, new Label("Brilho:"){{setStyle("-fx-text-fill: white;");}}, brightnessSlider, new Label("Contraste:"){{setStyle("-fx-text-fill: white;");}}, contrastSlider, new Label("Saturação:"){{setStyle("-fx-text-fill: white;");}}, saturationSlider);
        imageControls.setAlignment(Pos.CENTER);
        
        VBox allControls = new VBox(10, topControls, mainControls, imageControls);
        allControls.setPadding(new Insets(10));
        allControls.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");
        return allControls;
    }
    
    private ComboBox<String> createTrackComboBox(String name, Supplier<List<String>> trackSupplier, Consumer<Integer> trackSetter) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setPromptText(name);
        comboBox.setOnShowing(e -> {
            List<String> tracks = trackSupplier.get();
            comboBox.getItems().setAll(tracks);
            if (tracks.isEmpty()) comboBox.getItems().add("N/A");
        });
        comboBox.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> {
            if (newV.intValue() >= 0) trackSetter.accept(comboBox.getSelectionModel().getSelectedIndex());
        });
        return comboBox;
    }
    
    private Slider createAdjustSlider(float initialValue, Consumer<Float> setter) {
        Slider slider = new Slider(0, 2, initialValue);
        slider.valueProperty().addListener((obs, oldV, newV) -> setter.accept(newV.floatValue()));
        return slider;
    }

    public void playMedia(String channelName, String url) {
        channelTitleLabel.setText(channelName);
        mediaPlayer.media().play(url);
    }
    
    public void stopMedia() {
        if (mediaPlayer != null) mediaPlayer.controls().stop();
    }
    
    public void releasePlayer() {
        if (mediaPlayer != null) mediaPlayer.release();
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}