package com.seuprojeto;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import com.gluonhq.charm.down.common.Platform;
import com.gluonhq.charm.down.plugins.VideoService;

import java.util.function.Consumer;

public class PlayerView {

    private VBox view;
    private Label channelTitleLabel;
    private StackPane videoPane;
    private VideoService.VideoPlayer player;
    private Consumer<String> onPlayerStopped;

    public PlayerView(Consumer<String> onPlayerStopped) {
        this.onPlayerStopped = onPlayerStopped;
        
        channelTitleLabel = new Label();
        channelTitleLabel.getStyleClass().add("channel-title-label");

        videoPane = new StackPane();
        VBox.setVgrow(videoPane, Priority.ALWAYS);

        this.view = new VBox(10, channelTitleLabel, videoPane);
        this.view.setPadding(new Insets(20));
        this.view.getStyleClass().add("player-view");
    }

    public VBox getView() {
        return view;
    }

    public void playMedia(String channelName, String url) {
        channelTitleLabel.setText(channelName);
        if (player != null) {
            player.stop();
            player.release();
        }
        
        try {
            player = VideoService.get().getVideoPlayer();
            videoPane.getChildren().add(player.getSurface());
            player.play(url);
        } catch (Exception e) {
            e.printStackTrace();
            // Lidar com o erro
        }
    }

    public void stopMedia() {
        if (player != null) {
            player.stop();
        }
    }

    public void releasePlayer() {
        if (player != null) {
            player.release();
        }
    }
}