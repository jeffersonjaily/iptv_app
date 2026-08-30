package com.seuprojeto;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiConsumer;

public class ChannelListView {

    // =========================================================
    // LAYOUT PRINCIPAL
    // =========================================================

    private VBox rootLayout;
    private VBox contentArea;
    private VBox carrosselContainer;

    private ScrollPane mainScroll;

    // =========================================================
    // TOPO
    // =========================================================

    private HBox topNavbar;
    private HBox navigationBox;
    private HBox actionBox;

    private HBox urlContainer;
    private VBox filterContainer;

    private TextField urlField;
    private Button loadUrlButton;
    private Button changeListButton;
    private TextField searchField;

    private Label statusLabel;
    private Label logoLabel;

    // =========================================================
    // PROVEDORES
    // =========================================================

    private ProviderBar providerBar;

    private String provedorSelecionado = "Todos";

    // =========================================================
    // TEMA
    // =========================================================

    private String corFundo = "#0F0F0F";
    private String corMenu = "#000000";
    private String corDestaque = "#E50914";

    private String abaAtiva = "TUDO";

    // =========================================================
    // DADOS
    // =========================================================

    private volatile List<MediaItem> todosOsCanais =
            new ArrayList<>();

    private final BiConsumer<String, String> onPlayChannel;

    private final OkHttpClient httpClient =
            new OkHttpClient.Builder()
                    .build();

    private final Gson gson =
            new Gson();

    private final File CACHE_FILE =
            new File("playlist_cache.json");

    // =========================================================
    // CONTROLE DE FILTRO / RENDERIZAÇÃO
    // =========================================================

    private final PauseTransition debounceTimer =
            new PauseTransition(
                    Duration.millis(350)
            );

    private Task<Void> renderTask;

    // =========================================================
    // CONSTRUTOR
    // =========================================================

    public ChannelListView(
            BiConsumer<String, String> onPlayChannel
    ) {

        this.onPlayChannel = onPlayChannel;

        createView();

        carregarDoCacheOuArquivoLocal();
    }

    public VBox getView() {
        return rootLayout;
    }

    // =========================================================
    // CRIAÇÃO DA INTERFACE
    // =========================================================

    private void createView() {

        rootLayout = new VBox();

        rootLayout.setStyle(
                "-fx-background-color: " + corFundo + ";"
        );

        // =====================================================
        // TOP NAVBAR
        // =====================================================

        topNavbar = new HBox(20);

        topNavbar.setAlignment(
                Pos.CENTER_LEFT
        );

        topNavbar.setPadding(
                new Insets(
                        15,
                        25,
                        15,
                        25
                )
        );

        aplicarEstiloNavbar();

        // =====================================================
        // LOGO
        // =====================================================

        logoLabel =
                new Label("PLAYER PRO");

        aplicarEstiloLogo();

        // =====================================================
        // MENU
        // =====================================================

        navigationBox =
                new HBox(10);

        navigationBox.setAlignment(
                Pos.CENTER
        );

        navigationBox.getChildren().addAll(
                criarBotaoMenu(
                        "Início",
                        "TUDO"
                ),

                criarBotaoMenu(
                        "TV Ao Vivo",
                        "CANAL"
                ),

                criarBotaoMenu(
                        "Filmes",
                        "FILME"
                ),

                criarBotaoMenu(
                        "Séries",
                        "SERIE"
                )
        );

        HBox.setHgrow(
                navigationBox,
                Priority.ALWAYS
        );

        // =====================================================
        // URL
        // =====================================================

        urlField =
                new TextField();

        urlField.setPromptText(
                "URL da lista M3U..."
        );

        urlField.setPrefWidth(300);

        urlField.setStyle(
                "-fx-background-color: #222222;" +
                "-fx-text-fill: white;" +
                "-fx-prompt-text-fill: #777777;" +
                "-fx-padding: 8px;" +
                "-fx-background-radius: 5;"
        );

        loadUrlButton =
                new Button("Baixar");

        aplicarEstiloBotaoBaixar();

        loadUrlButton.setOnAction(event -> {

            String url =
                    urlField.getText();

            if (url == null) {
                return;
            }

            final String urlFinal =
                    url.trim();

            if (urlFinal.isEmpty()) {

                setStatus(
                        "Informe uma URL válida."
                );

                return;
            }

            baixarListaDaRede(
                    urlFinal
            );
        });

        urlContainer =
                new HBox(
                        10,
                        urlField,
                        loadUrlButton
                );

        urlContainer.setAlignment(
                Pos.CENTER_LEFT
        );

        // =====================================================
        // STATUS
        // =====================================================

        statusLabel =
                new Label("Pronto.");

        statusLabel.setStyle(
                "-fx-text-fill: #777777;" +
                "-fx-font-weight: bold;"
        );

        // =====================================================
        // BUSCA
        // =====================================================

        searchField =
                new TextField();

        searchField.setPromptText(
                "🔍 Buscar no catálogo..."
        );

        searchField.setPrefWidth(250);

        searchField.setStyle(
                "-fx-background-color: #222222;" +
                "-fx-text-fill: white;" +
                "-fx-prompt-text-fill: #777777;" +
                "-fx-padding: 8px;" +
                "-fx-background-radius: 5;"
        );

        searchField.textProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                dispararFiltro()
                );

        // =====================================================
        // ATUALIZAR LISTA
        // =====================================================

        changeListButton =
                new Button("🔄 Atualizar Lista");

        changeListButton.setStyle(
                "-fx-background-color: #333333;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8px 12px;" +
                "-fx-cursor: hand;" +
                "-fx-background-radius: 5;"
        );

        changeListButton.setOnAction(
                event ->
                        alternarModoHeader(false)
        );

        // =====================================================
        // ACTION BOX
        // =====================================================

        actionBox =
                new HBox(
                        15,
                        changeListButton,
                        searchField,
                        statusLabel
                );

        actionBox.setAlignment(
                Pos.CENTER_RIGHT
        );

        actionBox.setVisible(false);
        actionBox.setManaged(false);

        // =====================================================
        // MONTA NAVBAR
        // =====================================================

        topNavbar.getChildren().addAll(
                logoLabel,
                navigationBox,
                urlContainer,
                actionBox
        );

        // =====================================================
        // PROVIDER BAR
        // =====================================================

        VBox subHeaderBox =
                new VBox(10);

        subHeaderBox.setPadding(
                new Insets(
                        15,
                        25,
                        0,
                        25
                )
        );

        subHeaderBox.setAlignment(
                Pos.CENTER_LEFT
        );

        providerBar =
                new ProviderBar(
                        provedor -> {

                            if (provedor == null) {
                                provedor = "Todos";
                            }

                            provedorSelecionado =
                                    provedor;

                            aplicarTemaEstrategico(
                                    provedor
                            );

                            dispararFiltro();
                        }
                );

        filterContainer =
                new VBox(
                        providerBar.getView()
                );

        filterContainer.setVisible(false);
        filterContainer.setManaged(false);

        subHeaderBox.getChildren().add(
                filterContainer
        );

        // =====================================================
        // CONTEÚDO
        // =====================================================

        contentArea =
                new VBox(20);

        contentArea.setPadding(
                new Insets(
                        10,
                        25,
                        25,
                        25
                )
        );

        contentArea.setStyle(
                "-fx-background-color: " +
                corFundo +
                ";"
        );

        VBox.setVgrow(
                contentArea,
                Priority.ALWAYS
        );

        // =====================================================
        // CARROSSÉIS
        // =====================================================

        carrosselContainer =
                new VBox(30);

        carrosselContainer.setPadding(
                new Insets(
                        10,
                        0,
                        20,
                        0
                )
        );

        carrosselContainer.setStyle(
                "-fx-background-color: " +
                corFundo +
                ";"
        );

        // =====================================================
        // SCROLL PRINCIPAL
        // =====================================================

        mainScroll =
                new ScrollPane(
                        carrosselContainer
                );

        mainScroll.setFitToWidth(true);

        mainScroll.setVbarPolicy(
                ScrollPane.ScrollBarPolicy.AS_NEEDED
        );

        mainScroll.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        mainScroll.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-background: " + corFundo + ";" +
                "-fx-control-inner-background: " +
                corFundo + ";"
        );

        VBox.setVgrow(
                mainScroll,
                Priority.ALWAYS
        );

        contentArea.getChildren().addAll(
                subHeaderBox,
                mainScroll
        );

        rootLayout.getChildren().addAll(
                topNavbar,
                contentArea
        );

        resetarEstiloBotoesMenu();
        atualizarBotaoAtivo();
    }

    // =========================================================
    // ESTILOS
    // =========================================================

    private void aplicarEstiloNavbar() {

        topNavbar.setStyle(
                "-fx-background-color: " +
                corMenu +
                ";" +
                "-fx-border-color: #1A1A1A;" +
                "-fx-border-width: 0 0 1 0;"
        );
    }

    private void aplicarEstiloLogo() {

        logoLabel.setStyle(
                "-fx-text-fill: " +
                corDestaque +
                ";" +
                "-fx-font-size: 22px;" +
                "-fx-font-weight: 900;"
        );
    }

    private void aplicarEstiloBotaoBaixar() {

        loadUrlButton.setStyle(
                "-fx-background-color: " +
                corDestaque +
                ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8px 15px;" +
                "-fx-cursor: hand;" +
                "-fx-background-radius: 5;"
        );
    }

    // =========================================================
    // TEMA
    // =========================================================

    private void aplicarTemaEstrategico(
            String provedor
    ) {

        if (provedor == null ||
                provedor.equalsIgnoreCase("Todos")) {

            corFundo = "#0F0F0F";
            corMenu = "#000000";
            corDestaque = "#E50914";

            logoLabel.setText(
                    "PLAYER PRO"
            );

        } else {

            String prov =
                    provedor.toLowerCase(
                            Locale.ROOT
                    );

            if (prov.contains("netflix")) {

                corFundo = "#141414";
                corMenu = "#000000";
                corDestaque = "#E50914";

                logoLabel.setText(
                        "NETFLIX"
                );

            } else if (
                    prov.contains("hbo") ||
                    prov.contains("max")
            ) {

                corFundo = "#0A051A";
                corMenu = "#05020D";
                corDestaque = "#7C3AED";

                logoLabel.setText(
                        "HBO MAX"
                );

            } else if (
                    prov.contains("disney")
            ) {

                corFundo = "#040714";
                corMenu = "#02040A";
                corDestaque = "#0063E5";

                logoLabel.setText(
                        "DISNEY+"
                );

            } else if (
                    prov.contains("prime") ||
                    prov.contains("amazon")
            ) {

                corFundo = "#0F171E";
                corMenu = "#000000";
                corDestaque = "#00A8E1";

                logoLabel.setText(
                        "PRIME VIDEO"
                );

            } else if (
                    prov.contains("apple")
            ) {

                corFundo = "#000000";
                corMenu = "#111111";
                corDestaque = "#FFFFFF";

                logoLabel.setText(
                        "APPLE TV+"
                );

            } else {

                corFundo = "#0F0F0F";
                corMenu = "#000000";
                corDestaque = "#E50914";

                logoLabel.setText(
                        "PLAYER PRO"
                );
            }
        }

        rootLayout.setStyle(
                "-fx-background-color: " +
                corFundo +
                ";"
        );

        contentArea.setStyle(
                "-fx-background-color: " +
                corFundo +
                ";"
        );

        carrosselContainer.setStyle(
                "-fx-background-color: " +
                corFundo +
                ";"
        );

        mainScroll.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-background: " +
                corFundo +
                ";" +
                "-fx-control-inner-background: " +
                corFundo +
                ";"
        );

        aplicarEstiloNavbar();
        aplicarEstiloLogo();
        aplicarEstiloBotaoBaixar();

        providerBar.setCorDestaque(
                corDestaque
        );

        resetarEstiloBotoesMenu();
        atualizarBotaoAtivo();
    }

    // =========================================================
    // HEADER
    // =========================================================

    private void alternarModoHeader(
            boolean listaCarregada
    ) {

        urlContainer.setVisible(
                !listaCarregada
        );

        urlContainer.setManaged(
                !listaCarregada
        );

        actionBox.setVisible(
                listaCarregada
        );

        actionBox.setManaged(
                listaCarregada
        );

        filterContainer.setVisible(
                listaCarregada
        );

        filterContainer.setManaged(
                listaCarregada
        );
    }

    // =========================================================
    // MENU
    // =========================================================

    private Button criarBotaoMenu(
            String texto,
            String tipoAba
    ) {

        Button btn =
                new Button(texto);

        btn.setWrapText(false);

        btn.setAlignment(
                Pos.CENTER
        );

        btn.setUserData(
                tipoAba
        );

        btn.setOnAction(
                event -> {

                    abaAtiva =
                            tipoAba;

                    resetarEstiloBotoesMenu();

                    atualizarBotaoAtivo();

                    dispararFiltro();
                }
        );

        return btn;
    }

    private void resetarEstiloBotoesMenu() {

        String normalStyle =
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #BBBBBB;" +
                "-fx-font-size: 15px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 8 15;";

        for (javafx.scene.Node node :
                navigationBox.getChildren()) {

            if (node instanceof Button) {

                Button btn =
                        (Button) node;

                btn.setStyle(
                        normalStyle
                );

                btn.setOnMouseEntered(
                        event -> {

                            if (!abaAtiva.equals(
                                    btn.getUserData()
                            )) {

                                btn.setStyle(
                                        "-fx-background-color: transparent;" +
                                        "-fx-text-fill: white;" +
                                        "-fx-font-size: 15px;" +
                                        "-fx-font-weight: bold;" +
                                        "-fx-cursor: hand;" +
                                        "-fx-padding: 8 15;"
                                );
                            }
                        }
                );

                btn.setOnMouseExited(
                        event -> {

                            if (!abaAtiva.equals(
                                    btn.getUserData()
                            )) {

                                btn.setStyle(
                                        normalStyle
                                );
                            }
                        }
                );
            }
        }
    }

    private void atualizarBotaoAtivo() {

        String activeStyle =
                "-fx-background-color: transparent;" +
                "-fx-text-fill: white;" +
                "-fx-border-color: " +
                corDestaque +
                ";" +
                "-fx-border-width: 0 0 3 0;" +
                "-fx-font-size: 15px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 8 15;";

        for (javafx.scene.Node node :
                navigationBox.getChildren()) {

            if (node instanceof Button) {

                Button btn =
                        (Button) node;

                if (abaAtiva.equals(
                        btn.getUserData()
                )) {

                    btn.setStyle(
                            activeStyle
                    );
                }
            }
        }
    }

    // =========================================================
    // FILTRO
    // =========================================================

    private void dispararFiltro() {

        debounceTimer.setOnFinished(
                event ->
                        renderizarTelaAsync()
        );

        debounceTimer.playFromStart();
    }

    // =========================================================
    // DOWNLOAD DA LISTA
    // =========================================================

    private void baixarListaDaRede(
            String url
    ) {

        if (url == null) {
            return;
        }

        final String urlFinal =
                url.trim();

        if (urlFinal.isEmpty()) {
            return;
        }

        setStatus(
                "Baixando lista..."
        );

        urlField.setDisable(true);
        loadUrlButton.setDisable(true);

        Task<Void> task =
                new Task<Void>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        Request request =
                                new Request.Builder()
                                        .url(urlFinal)
                                        .header(
                                                "User-Agent",
                                                "Mozilla/5.0"
                                        )
                                        .build();

                        try (
                                Response response =
                                        httpClient
                                                .newCall(request)
                                                .execute()
                        ) {

                            if (!response.isSuccessful()) {

                                throw new IOException(
                                        "HTTP " +
                                        response.code()
                                );
                            }

                            if (response.body() == null) {

                                throw new IOException(
                                        "Resposta vazia."
                                );
                            }

                            String content =
                                    response.body()
                                            .string();

                            if (content.trim()
                                    .isEmpty()) {

                                throw new IOException(
                                        "Lista vazia."
                                );
                            }

                            final String conteudoFinal =
                                    content;

                            List<MediaItem> lista =
                                    M3uParserService
                                            .parse(
                                                    conteudoFinal
                                            );

                            if (lista == null ||
                                    lista.isEmpty()) {

                                throw new IOException(
                                        "Nenhuma mídia encontrada."
                                );
                            }

                            todosOsCanais =
                                    new ArrayList<>(
                                            lista
                                    );

                            salvarEmCache();

                            Platform.runLater(
                                    () -> {

                                        atualizarListaDeProvedores();

                                        alternarModoHeader(
                                                true
                                        );

                                        resetarEstiloBotoesMenu();

                                        atualizarBotaoAtivo();

                                        renderizarTelaAsync();

                                        setStatus(
                                                lista.size() +
                                                " mídias carregadas."
                                        );
                                    }
                            );
                        }

                        return null;
                    }
                };

        task.setOnSucceeded(
                event -> {

                    urlField.setDisable(false);
                    loadUrlButton.setDisable(false);
                }
        );

        task.setOnFailed(
                event -> {

                    urlField.setDisable(false);
                    loadUrlButton.setDisable(false);

                    Throwable erro =
                            task.getException();

                    String mensagem =
                            erro != null &&
                            erro.getMessage() != null
                                    ? erro.getMessage()
                                    : "Erro desconhecido.";

                    setStatus(
                            "Erro: " +
                            mensagem
                    );
                }
        );

        Thread thread =
                new Thread(
                        task,
                        "Download-M3U"
                );

        thread.setDaemon(true);

        thread.start();
    }

    // =========================================================
    // CACHE
    // =========================================================

    private void salvarEmCache() {

        try (
                FileWriter writer =
                        new FileWriter(
                                CACHE_FILE
                        )
        ) {

            gson.toJson(
                    todosOsCanais,
                    writer
            );

        } catch (IOException e) {

            System.out.println(
                    "Erro ao salvar cache: " +
                    e.getMessage()
            );
        }
    }

    // =========================================================
    // CARREGAMENTO LOCAL
    // =========================================================

    private void carregarDoCacheOuArquivoLocal() {

        // =====================================================
        // CACHE
        // =====================================================

        if (CACHE_FILE.exists()) {

            try (
                    FileReader reader =
                            new FileReader(
                                    CACHE_FILE
                            )
            ) {

                Type listType =
                        new TypeToken<
                                ArrayList<MediaItem>
                                >() {
                        }.getType();

                List<MediaItem> cache =
                        gson.fromJson(
                                reader,
                                listType
                        );

                if (cache != null &&
                        !cache.isEmpty()) {

                    todosOsCanais =
                            new ArrayList<>(
                                    cache
                            );

                    finalizarCarregamentoLocal();

                    return;
                }

            } catch (Exception e) {

                System.out.println(
                        "Cache inválido: " +
                        e.getMessage()
                );
            }
        }

        // =====================================================
        // ARQUIVO LOCAL
        // =====================================================

        File file =
                new File(
                        "LISTA_IPTV.TXT"
                );

        if (!file.exists()) {

            setStatus(
                    "Nenhuma lista encontrada."
            );

            return;
        }

        try {

            String content =
                    new String(
                            Files.readAllBytes(
                                    Paths.get(
                                            file.toURI()
                                    )
                            ),
                            StandardCharsets.UTF_8
                    );

            if (content.trim().isEmpty()) {

                setStatus(
                        "LISTA_IPTV.TXT está vazia."
                );

                return;
            }

            List<MediaItem> lista =
                    M3uParserService.parse(
                            content
                    );

            if (lista != null &&
                    !lista.isEmpty()) {

                todosOsCanais =
                        new ArrayList<>(
                                lista
                        );

                salvarEmCache();

                finalizarCarregamentoLocal();

            } else {

                setStatus(
                        "Nenhuma mídia encontrada."
                );
            }

        } catch (Exception e) {

            setStatus(
                    "Erro ao carregar lista local."
            );

            System.out.println(
                    "Erro lista local: " +
                    e.getMessage()
            );
        }
    }

    private void finalizarCarregamentoLocal() {

        Platform.runLater(
                () -> {

                    atualizarListaDeProvedores();

                    alternarModoHeader(
                            true
                    );

                    resetarEstiloBotoesMenu();

                    atualizarBotaoAtivo();

                    renderizarTelaAsync();

                    setStatus(
                            todosOsCanais.size() +
                            " mídias carregadas."
                    );
                }
        );
    }

    // =========================================================
    // PROVEDORES
    // =========================================================

    private void atualizarListaDeProvedores() {

        Set<String> provedoresUnicos =
                new TreeSet<>(
                        String.CASE_INSENSITIVE_ORDER
                );

        for (MediaItem item :
                todosOsCanais) {

            if (item == null) {
                continue;
            }

            String grupo =
                    item.getGroup();

            if (grupo != null &&
                    !grupo.trim().isEmpty()) {

                provedoresUnicos.add(
                        grupo.trim()
                );
            }
        }

        Platform.runLater(
                () ->
                        providerBar
                                .atualizarProvedores(
                                        provedoresUnicos
                                )
        );
    }

    // =========================================================
    // RENDERIZAÇÃO
    // =========================================================

    private synchronized void renderizarTelaAsync() {

        final String textoBusca =
                searchField.getText() == null
                        ? ""
                        : searchField
                                .getText()
                                .trim()
                                .toLowerCase(
                                        Locale.ROOT
                                );

        final String aba =
                abaAtiva;

        final String provedor =
                provedorSelecionado;

        final List<MediaItem> snapshot =
                new ArrayList<>(
                        todosOsCanais
                );

        if (renderTask != null &&
                renderTask.isRunning()) {

            renderTask.cancel();
        }

        setStatus(
                "Montando interface..."
        );

        renderTask =
                new Task<Void>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        Map<String,
                                List<MediaItem>>
                                grupos =
                                new TreeMap<>(
                                        String.CASE_INSENSITIVE_ORDER
                                );

                        for (
                                MediaItem item :
                                snapshot
                        ) {

                            if (isCancelled()) {
                                return null;
                            }

                            if (item == null) {
                                continue;
                            }

                            String tipo =
                                    item.getType();

                            String grupo =
                                    item.getGroup();

                            String titulo =
                                    item.getTitle();

                            if (tipo == null) {
                                tipo = "";
                            }

                            if (grupo == null ||
                                    grupo.trim()
                                            .isEmpty()) {

                                grupo =
                                        "Outros";
                            }

                            if (titulo == null) {
                                titulo = "";
                            }

                            if (!"TUDO".equalsIgnoreCase(
                                    aba
                            ) &&
                                    !tipo.equalsIgnoreCase(
                                            aba
                                    )) {

                                continue;
                            }

                            if (!"Todos".equalsIgnoreCase(
                                    provedor
                            ) &&
                                    !grupo.equalsIgnoreCase(
                                            provedor
                                    )) {

                                continue;
                            }

                            if (!textoBusca.isEmpty() &&
                                    !titulo
                                            .toLowerCase(
                                                    Locale.ROOT
                                            )
                                            .contains(
                                                    textoBusca
                                            )) {

                                continue;
                            }

                            grupos
                                    .computeIfAbsent(
                                            grupo,
                                            key ->
                                                    new ArrayList<>()
                                    )
                                    .add(item);
                        }

                        Platform.runLater(
                                () ->
                                        carrosselContainer
                                                .getChildren()
                                                .clear()
                        );

                        int count = 0;

                        for (
                                Map.Entry<
                                        String,
                                        List<MediaItem>
                                        > entrada :
                                grupos.entrySet()
                        ) {

                            if (isCancelled()) {
                                break;
                            }

                            String titulo =
                                    entrada.getKey();

                            List<MediaItem> itens =
                                    entrada.getValue();

                            VBox trilho =
                                    criarTrilho(
                                            titulo,
                                            itens
                                    );

                            Platform.runLater(
                                    () -> {

                                        if (!isCancelled()) {

                                            carrosselContainer
                                                    .getChildren()
                                                    .add(
                                                            trilho
                                                    );
                                        }
                                    }
                            );

                            count +=
                                    itens.size();

                            Thread.sleep(10);
                        }

                        final int total =
                                count;

                        Platform.runLater(
                                () ->
                                        setStatus(
                                                total +
                                                " mídias organizadas."
                                        )
                        );

                        return null;
                    }
                };

        Thread thread =
                new Thread(
                        renderTask,
                        "Renderizador-Catalogo"
                );

        thread.setDaemon(true);

        thread.start();
    }

    // =========================================================
    // TRILHO
    // =========================================================

    private VBox criarTrilho(
            String tituloSetor,
            List<MediaItem> itens
    ) {

        VBox trilhoBox =
                new VBox(15);

        Label titulo =
                new Label(
                        tituloSetor == null ||
                        tituloSetor.trim().isEmpty()
                                ? "Outros"
                                : tituloSetor
                );

        titulo.setStyle(
                "-fx-text-fill: #FFFFFF;" +
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 0 0 0 5px;"
        );

        HBox cardContainer =
                new HBox(15);

        cardContainer.setAlignment(
                Pos.CENTER_LEFT
        );

        ScrollPane scrollHorizontal =
                new ScrollPane(
                        cardContainer
                );

        scrollHorizontal.setVbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        scrollHorizontal.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        scrollHorizontal.setFitToHeight(true);

        scrollHorizontal.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-background: transparent;" +
                "-fx-control-inner-background: transparent;"
        );

        scrollHorizontal.setPannable(true);

        // =====================================================
        // ROLAGEM VERTICAL → HORIZONTAL
        // =====================================================

        scrollHorizontal.setOnScroll(
                event -> {

                    if (event.getDeltaY() != 0) {

                        double velocidade =
                                event.getDeltaY() /
                                Math.max(
                                        1,
                                        scrollHorizontal
                                                .getWidth()
                                ) *
                                2.5;

                        scrollHorizontal.setHvalue(
                                Math.max(
                                        0,
                                        Math.min(
                                                1,
                                                scrollHorizontal
                                                        .getHvalue()
                                                        - velocidade
                                        )
                                )
                        );

                        event.consume();
                    }
                }
        );

        // =====================================================
        // CARREGAMENTO EM LOTES
        // =====================================================

        final int BATCH =
                30;

        final int[] index =
                {0};

        Runnable carregarMais =
                () -> {

                    if (index[0] >= itens.size()) {
                        return;
                    }

                    int inicio =
                            index[0];

                    int fim =
                            Math.min(
                                    inicio + BATCH,
                                    itens.size()
                            );

                    List<MediaCard> novos =
                            new ArrayList<>();

                    for (
                            int i = inicio;
                            i < fim;
                            i++
                    ) {

                        MediaItem item =
                                itens.get(i);

                        if (item == null) {
                            continue;
                        }

                        final MediaItem itemFinal =
                                item;

                        MediaCard card =
                                new MediaCard(
                                        itemFinal.getTitle(),
                                        itemFinal.getLogo(),
                                        itemFinal.getUrl(),
                                        () -> {

                                            if (onPlayChannel == null) {
                                                return;
                                            }

                                            Platform.runLater(
                                                    () ->
                                                            onPlayChannel.accept(
                                                                    itemFinal.getTitle(),
                                                                    itemFinal.getUrl()
                                                            )
                                            );
                                        }
                                );

                        novos.add(card);
                    }

                    index[0] =
                            fim;

                    if (!novos.isEmpty()) {

                        Platform.runLater(
                                () ->
                                        cardContainer
                                                .getChildren()
                                                .addAll(
                                                        novos
                                                )
                        );
                    }
                };

        carregarMais.run();

        // =====================================================
        // LAZY LOADING
        // =====================================================

        scrollHorizontal
                .hvalueProperty()
                .addListener(
                        (observable,
                         oldValue,
                         newValue) -> {

                            if (newValue.doubleValue()
                                    >= 0.70) {

                                if (index[0] <
                                        itens.size()) {

                                    carregarMais.run();
                                }
                            }
                        }
                );

        trilhoBox.getChildren().addAll(
                titulo,
                scrollHorizontal
        );

        return trilhoBox;
    }

    // =========================================================
    // STATUS
    // =========================================================

    private void setStatus(
            String message
    ) {

        if (message == null) {
            message = "";
        }

        final String mensagemFinal =
                message;

        Platform.runLater(
                () ->
                        statusLabel.setText(
                                mensagemFinal
                        )
        );
    }
}