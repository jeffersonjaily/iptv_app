// Salve como: ChannelListView.java
package com.seuprojeto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ChannelListView {
    private VBox view;
    private TreeView<String> treeView;
    private ComboBox<String> listaCombobox;
    private Label statusLabel;
    private Button loadUrlButton;
    private TextField urlField;
    private TextField ipField;
    private TextField portField;
    private Button bluetoothButton; // Adicionado: Botão para Bluetooth

    private final Map<String, String> listasEncontradas = new HashMap<>();
    private final List<Map<String, String>> todosOsCanais = new ArrayList<>();
    private final Map<TreeItem<String>, String> channelLinks = new HashMap<>();

    private final BiConsumer<String, String> onPlayChannel;

    public ChannelListView(BiConsumer<String, String> onPlayChannel) {
        this.onPlayChannel = onPlayChannel;
        createView();
        checkForLocalListFile();
    }

    public VBox getView() {
        return view;
    }

    private void createView() {
        // Painel para importação de URL
        Label urlLabel = new Label("Cole uma URL aqui:");
        urlLabel.setStyle("-fx-text-fill: white;");

        urlField = new TextField();
        urlField.setPromptText("Ex: http://servidor.com/lista.m3u");

        loadUrlButton = new Button("Carregar da URL");
        loadUrlButton.setOnAction(e -> carregarUrl());
        loadUrlButton.setStyle("-fx-font-weight: bold; -fx-background-color: #4CAF50; -fx-text-fill: white;");

        HBox urlPanel = new HBox(10, urlField, loadUrlButton);
        HBox.setHgrow(urlField, Priority.ALWAYS);
        
        // Painel para importação de Rede Local
        Label networkLabel = new Label("Conectar na Rede Local:");
        networkLabel.setStyle("-fx-text-fill: white;");
        
        ipField = new TextField();
        ipField.setPromptText("IP (Ex: 192.168.1.10)");
        ipField.setPrefWidth(150);

        portField = new TextField();
        portField.setPromptText("Porta (Ex: 8080)");
        portField.setPrefWidth(100);
        
        Button connectButton = new Button("Conectar");
        connectButton.setOnAction(e -> connectToLocalNetwork());
        connectButton.setStyle("-fx-font-weight: bold; -fx-background-color: #0077CC; -fx-text-fill: white;");

        HBox networkPanel = new HBox(10, ipField, portField, connectButton);
        networkPanel.setPadding(new Insets(10, 0, 0, 0));
        
        // Painel para importação de Bluetooth
        bluetoothButton = new Button("Importar via Bluetooth");
        bluetoothButton.setOnAction(e -> importFromBluetooth());
        bluetoothButton.setStyle("-fx-font-weight: bold; -fx-background-color: #3B5998; -fx-text-fill: white;");
        
        // Painel para seleção de arquivo local ou URL
        VBox topPanel = new VBox(10);
        
        Label selectionLabel = new Label("Selecione uma lista encontrada no arquivo:");
        selectionLabel.setStyle("-fx-text-fill: white;");
        
        listaCombobox = new ComboBox<>();
        listaCombobox.setPromptText("Listas encontradas no arquivo");
        listaCombobox.setMaxWidth(Double.MAX_VALUE);
        
        Button loadFileButton = new Button("Carregar Canais da Lista");
        loadFileButton.setOnAction(e -> carregarCanaisDaLista());
        loadFileButton.setStyle("-fx-font-weight: bold; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        
        topPanel.getChildren().addAll(selectionLabel, listaCombobox, loadFileButton, urlLabel, urlPanel, networkLabel, networkPanel, bluetoothButton);

        TextField filterEntry = new TextField();
        filterEntry.setPromptText("Digite para filtrar...");
        filterEntry.setOnKeyReleased(e -> filtrarLista(filterEntry.getText()));

        statusLabel = new Label("Pronto.");
        statusLabel.setStyle("-fx-text-fill: lightgray; -fx-padding: 5px;");
        
        treeView = new TreeView<>(new TreeItem<>("Canais"));
        treeView.setShowRoot(false);
        treeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<String> selectedItem = treeView.getSelectionModel().getSelectedItem();
                if (selectedItem != null && channelLinks.containsKey(selectedItem)) {
                    String url = channelLinks.get(selectedItem);
                    if (url != null) {
                        onPlayChannel.accept(selectedItem.getValue(), url);
                    }
                }
            }
        });

        view = new VBox(10, topPanel, filterEntry, statusLabel, treeView);
        view.setPadding(new Insets(10));
        view.setStyle("-fx-background-color: #2B2B2B;");
        VBox.setVgrow(treeView, Priority.ALWAYS);
    }

    public void abrirEProcessarArquivo(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Abrir arquivo de lista IPTV");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Arquivos de Lista", "*.txt", "*.m3u", "*.m3u8"),
            new FileChooser.ExtensionFilter("Todos os arquivos", "*.*")
        );
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            processarArquivo(file);
        }
    }

    private void checkForLocalListFile() {
        File localFile = new File("LISTA_IPTV.TXT");
        if (localFile.exists()) {
            setStatus("Arquivo LISTA_IPTV.TXT encontrado! Processando...");
            processarArquivo(localFile);
        } else {
            setStatus("Pronto. Use 'Arquivo > Abrir...' ou cole uma URL.");
        }
    }

    private void processarArquivo(File file) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(file.toURI())));
            listasEncontradas.clear();
            
            Pattern linkPattern = Pattern.compile("http[s]?://[^\\s]+(?:m3u_plus|m3u|m3u8)");
            Matcher linkMatcher = linkPattern.matcher(content);
            int linkCount = 0;
            while (linkMatcher.find()) {
                String link = linkMatcher.group();
                String nome = "Link M3U #" + (++linkCount) + " (" + link.split("/")[2] + ")";
                listasEncontradas.put(nome, link);
            }
            
            Pattern blockPattern = Pattern.compile("(?:Servidor|\\p{So}\\p{L}\\p{M}*|\\p{So})[\\s➤:]+(?<host>[\\w.:-]+)[\\s\\S]*?(?:Usuário|\\p{So}\\p{L}\\p{M}*|\\p{So})[\\s➤:]+(?<user>[\\w-]+)[\\s\\S]*?(?:Senha|\\p{So}\\p{L}\\p{M}*|\\p{So})[\\s➤:]+(?<pass>[\\w-]+)");
            Matcher blockMatcher = blockPattern.matcher(content);
            while (blockMatcher.find()) {
                String host = blockMatcher.group("host");
                String user = blockMatcher.group("user");
                String password = blockMatcher.group("pass");
                String link = String.format("http://%s/get.php?username=%s&password=%s&type=m3u_plus", host, user, password);
                String nome = "Servidor: " + host + " | Usuário: " + user;
                listasEncontradas.put(nome, link);
            }

            if (listasEncontradas.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Nenhuma Lista", "Nenhuma lista de IPTV válida foi encontrada no arquivo.");
            } else {
                listaCombobox.getItems().setAll(listasEncontradas.keySet());
                listaCombobox.getSelectionModel().selectFirst();
                setStatus(listasEncontradas.size() + " listas encontradas!");
            }

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erro de Arquivo", "Não foi possível ler o arquivo: " + e.getMessage());
        }
    }

    private void carregarCanaisDaLista() {
        String nomeSelecionado = listaCombobox.getSelectionModel().getSelectedItem();
        if (nomeSelecionado == null) return;
        String m3uUrl = listasEncontradas.get(nomeSelecionado);
        executarDownload(m3uUrl);
    }

    private void carregarUrl() {
        String url = urlField.getText().trim();
        if (url.isEmpty() || !url.startsWith("http")) {
            showAlert(Alert.AlertType.ERROR, "URL Inválida", "Por favor, insira um URL válido.");
            return;
        }
        executarDownload(url);
    }

    private void connectToLocalNetwork() {
        String ip = ipField.getText().trim();
        String port = portField.getText().trim();
        
        if (ip.isEmpty() || port.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Conexão Inválida", "Por favor, preencha o IP e a Porta.");
            return;
        }
        
        String url = String.format("http://%s:%s/lista.m3u", ip, port);
        executarDownload(url);
    }
    
    private void importFromBluetooth() {
        showAlert(Alert.AlertType.INFORMATION, "Importar via Bluetooth", "Funcionalidade em desenvolvimento. Por favor, use outros métodos de importação.");
    }

    private void executarDownload(String url) {
        setStatus("Baixando lista... Aguarde...");
        urlField.setDisable(true);
        loadUrlButton.setDisable(true);
        Task<List<Map<String, String>>> task = new Task<>() {
            @Override
            protected List<Map<String, String>> call() throws Exception {
                Request request = new Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build();
                try (Response response = new OkHttpClient().newCall(request).execute()) {
                    if (!response.isSuccessful()) throw new IOException("Falha: " + response);
                    ResponseBody body = response.body();
                    return body != null ? parseM3uContent(body.string()) : new ArrayList<>();
                }
            }
        };
        task.setOnSucceeded(e -> {
            todosOsCanais.clear();
            todosOsCanais.addAll(task.getValue());
            popularTreeView(todosOsCanais);
            setStatus(todosOsCanais.size() + " canais carregados!");
            urlField.setDisable(false);
            loadUrlButton.setDisable(false);
        });
        task.setOnFailed(e -> {
            showAlert(Alert.AlertType.ERROR, "Erro de Rede", "Falha ao baixar lista: " + task.getException().getMessage());
            setStatus("Falha ao carregar a lista.");
            urlField.setDisable(false);
            loadUrlButton.setDisable(false);
        });
        new Thread(task).start();
    }
    
    private List<Map<String, String>> parseM3uContent(String content) {
        List<Map<String, String>> canais = new ArrayList<>();
        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().startsWith("#EXTINF:")) {
                try {
                    String infoLine = lines[i];
                    String urlLine = lines[i + 1].trim();
                    if (!urlLine.isEmpty() && !urlLine.startsWith("#")) {
                        String name = infoLine.substring(infoLine.lastIndexOf(",") + 1).trim();
                        Matcher groupMatcher = Pattern.compile("group-title=\"(.*?)\"").matcher(infoLine);
                        String group = groupMatcher.find() ? groupMatcher.group(1) : "Geral";
                        Map<String, String> canal = new HashMap<>();
                        canal.put("title", name); canal.put("group", group); canal.put("url", urlLine);
                        canais.add(canal);
                    }
                } catch (Exception ignored) {}
            }
        }
        return canais;
    }

    private void popularTreeView(List<Map<String, String>> canais) {
        TreeItem<String> rootItem = new TreeItem<>("Canais");
        treeView.setRoot(rootItem);
        channelLinks.clear();
        Map<String, TreeItem<String>> categoryNodes = new HashMap<>();
        for (Map<String, String> canal : canais) {
            String group = canal.get("group");
            String name = canal.get("title");
            TreeItem<String> parentNode = categoryNodes.computeIfAbsent(group, g -> {
                TreeItem<String> newNode = new TreeItem<>(g);
                rootItem.getChildren().add(newNode);
                return newNode;
            });
            TreeItem<String> childNode = new TreeItem<>(name);
            parentNode.getChildren().add(childNode);
            channelLinks.put(childNode, canal.get("url"));
        }
    }

    private void filtrarLista(String filtro) {
        String textoFiltro = filtro.toLowerCase().trim();
        List<Map<String, String>> canaisFiltrados;
        if (textoFiltro.isEmpty()) {
            canaisFiltrados = todosOsCanais;
        } else {
            canaisFiltrados = todosOsCanais.stream()
                .filter(c -> c.get("title").toLowerCase().contains(textoFiltro) || c.get("group").toLowerCase().contains(textoFiltro))
                .collect(Collectors.toList());
        }
        popularTreeView(canaisFiltrados);
        if (!textoFiltro.isEmpty()) {
            treeView.getRoot().getChildren().forEach(item -> item.setExpanded(true));
        }
    }

    private void setStatus(String message) { Platform.runLater(() -> statusLabel.setText(message)); }
    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}