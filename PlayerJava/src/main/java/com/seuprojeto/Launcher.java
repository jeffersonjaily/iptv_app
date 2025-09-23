// Salve como: Launcher.java
package com.seuprojeto;

import javafx.application.Application;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;

class Launcher {
    public static void main(String[] args) {
        // Altere o caminho para o seu diretório de instalação do VLC
        System.setProperty("jna.library.path", "C:\\Program Files\\VideoLAN\\VLC");

        new NativeDiscovery().discover();
        Application.launch(App.class, args);
    }
}