package com.seuprojeto;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import java.util.Set;
import java.util.function.Consumer;

public class ProviderBar {
    private ScrollPane scrollPane;
    private HBox container;
    private String provedorAtivo = "Todos";
    private Consumer<String> onProviderSelected;
    
    private String corDestaque = "#E50914";

    public ProviderBar(Consumer<String> onProviderSelected) {
        this.onProviderSelected = onProviderSelected;
        
        container = new HBox(10);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(5, 0, 15, 0)); // Espaço extra para a barra não engolir o botão

        scrollPane = new ScrollPane(container);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED); // Mostra a barra se precisar
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setFitToHeight(true);
        scrollPane.setPrefViewportHeight(55);
        scrollPane.setPannable(true); // Permite clicar e arrastar a barra de provedores!
    }

    public ScrollPane getView() { return scrollPane; }
    
    public void setCorDestaque(String cor) {
        this.corDestaque = cor;
    }

    public void atualizarProvedores(Set<String> provedores) {
        container.getChildren().clear();
        
        Button btnTodos = criarBotaoPill("Todos");
        container.getChildren().add(btnTodos);

        for (String prov : provedores) {
            container.getChildren().add(criarBotaoPill(prov));
        }
    }

    private Button criarBotaoPill(String nome) {
        Button btn = new Button(nome);
        
        String baseStyle = "-fx-font-weight: bold; -fx-padding: 8px 16px; -fx-background-radius: 20px; -fx-cursor: hand; ";
        String inactiveStyle = baseStyle + "-fx-background-color: #333333; -fx-text-fill: #CCCCCC;";
        String activeStyle = baseStyle + "-fx-background-color: " + corDestaque + "; -fx-text-fill: white;";
        
        btn.setStyle(nome.equals(provedorAtivo) ? activeStyle : inactiveStyle);

        btn.setOnAction(e -> {
            provedorAtivo = nome;
            for (javafx.scene.Node node : container.getChildren()) {
                if (node instanceof Button b) {
                    b.setStyle(b.getText().equals(provedorAtivo) ? activeStyle : inactiveStyle);
                }
            }
            onProviderSelected.accept(nome);
        });
        
        return btn;
    }
}