// Save as: App.java
package com.seuprojeto;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class App extends Application {
    private Stage primaryStage;
    private BorderPane rootLayout;
    private ChannelListView channelListView;
    private PlayerView playerView;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("Player IPTV Profissional (JavaFX)");

        rootLayout = new BorderPane();
        MenuBar menuBar = createMenuBar();
        rootLayout.setTop(menuBar);

        playerView = new PlayerView(this::showChannelList); 
        channelListView = new ChannelListView(this::showPlayer); 
        
        rootLayout.setCenter(channelListView.getView());

        Scene scene = new Scene(rootLayout, 900, 700);
        // Load the CSS file
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> {
            if (playerView != null) {
                playerView.releasePlayer();
            }
            Platform.exit();
        });
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("Carregar Lista");
        MenuItem openItem = new MenuItem("Abrir Arquivo de Listas...");
        openItem.setOnAction(e -> channelListView.abrirEProcessarArquivo(primaryStage));
        MenuItem exitItem = new MenuItem("Sair");
        exitItem.setOnAction(e -> Platform.exit());
        fileMenu.getItems().addAll(openItem, new SeparatorMenuItem(), exitItem);

        Menu controlMenu = new Menu("Controles");
        MenuItem backItem = new MenuItem("Voltar para a Lista");
        backItem.setOnAction(e -> showChannelList());
        controlMenu.getItems().add(backItem);
        
        menuBar.getMenus().addAll(fileMenu, controlMenu);
        return menuBar;
    }

    public void showPlayer(String channelName, String videoUrl) {
        rootLayout.setCenter(playerView.getView());
        playerView.playMedia(channelName, videoUrl);
        primaryStage.setMaximized(true);
    }

    public void showChannelList() {
        playerView.stopMedia();
        rootLayout.setCenter(channelListView.getView());
        primaryStage.setMaximized(false);
    }
}