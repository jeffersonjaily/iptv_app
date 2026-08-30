package com.seuprojeto;

import javafx.application.Application;
// import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery; // Comentado temporariamente para testar a UI primeiro

public class Launcher {
    public static void main(String[] args) {
        // Por enquanto, vamos apenas iniciar a interface gráfica.
        // O VLCJ será reconfigurado quando formos arrumar o PlayerView.
        
        System.out.println("Iniciando a Aplicação Desktop...");
        Application.launch(App.class, args);
    }
}