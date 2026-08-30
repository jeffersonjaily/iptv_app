package com.seuprojeto;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Agora chamamos o nosso Shell (Roteador de Telas)
        MainShell shellPrincipal = new MainShell();

        // Passa o layout principal para a Janela
        Scene scene = new Scene(shellPrincipal.getView(), 1280, 720);
        
        primaryStage.setTitle("Player IPTV Profissional");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args); 
    }
}