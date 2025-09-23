package com.seuprojeto;

import com.gluonhq.charm.down.common.MobileApplication;
import com.gluonhq.charm.down.common.Platform;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends MobileApplication {

    public static final String HOME_VIEW = "homeView";
    public static final String PLAYER_VIEW = "playerView";
    
    private PlayerView playerView;

    @Override
    public void init() {
        this.playerView = new PlayerView(this::showChannelList);

        addViewFactory(HOME_VIEW, () -> new ChannelListView(this::showPlayer).getView());
        addViewFactory(PLAYER_VIEW, () -> playerView.getView());

        if (Platform.isAndroid()) {
            this.setBackHandler(this::onBackPressed);
        }
    }

    private void onBackPressed() {
        if (getNavigator().getView().getName().equals(PLAYER_VIEW)) {
            showChannelList();
        } else {
            Platform.exit();
        }
    }

    public void showPlayer(String channelName, String videoUrl) {
        playerView.playMedia(channelName, videoUrl);
        switchView(PLAYER_VIEW);
    }
    
    public void showChannelList() {
        playerView.stopMedia();
        switchView(HOME_VIEW);
    }
    
    public static void main(String[] args) {
        launch(App.class, args);
    }
}