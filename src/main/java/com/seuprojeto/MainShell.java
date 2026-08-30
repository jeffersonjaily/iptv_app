package com.seuprojeto;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainShell {

    private BorderPane rootLayout;

    private VBox menuLateral;
    private StackPane areaDeConteudo;
    private VBox containerConteudo;
    private ScrollPane scrollConteudoGenerico;

    private PlayerView playerView;

    private Label statusLabel;
    private TextField barraBusca;

    // =========================
    // TOP BAR
    // =========================

    private HBox topBar;

    private ComboBox<String> modoFonteBox;
    private ComboBox<String> seletorListasRepositorio;

    private TextField urlManualField;

    private Button btnEscolherArquivo;
    private Button btnCarregar;

    private Label nomeArquivoLabel;

    private File arquivoSelecionado;

    // =========================
    // REPOSITÓRIOS
    // =========================

    private final Map<String, String> repositorios =
            new LinkedHashMap<>();

    // =========================
    // ESTADO
    // =========================

    private String abaAtiva = "INICIO";

    private List<MediaItem> todosOsCanais =
            new ArrayList<>();

    // =========================
    // THREADS
    // =========================

    private final ExecutorService executor =
            Executors.newFixedThreadPool(3);

    // =========================
    // JSON / CACHE
    // =========================

    private final Gson gson = new Gson();

    private final File CACHE_FILE =
            new File("playlist_cache.json");

    // =========================
    // HTTP
    // =========================

    private final OkHttpClient httpClient =
            new OkHttpClient.Builder()
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build();

    // =========================
    // CONSTRUTOR
    // =========================

    public MainShell() {

        playerView =
                new PlayerView(this::fecharModoCinema);

        configurarRepositorios();

        createView();

        carregarDoCacheOuArquivoLocal();
    }

    // =========================
    // VIEW PRINCIPAL
    // =========================

    public BorderPane getView() {
        return rootLayout;
    }

    // =========================
    // REPOSITÓRIOS
    // =========================

    private void configurarRepositorios() {

        repositorios.clear();

        repositorios.put(
                "Lista GitHub Principal",
                "https://raw.githubusercontent.com/"
                        + "jeffersonjaily/iptv_app/main/"
                        + "PlayerAndroid2/app/src/main/assets/"
                        + "LISTA_IPTV.TXT"
        );

        repositorios.put(
                "Servidor fixo",
                "http://jimbim.top/get.php"
                        + "?username=y8FjVm"
                        + "&password=xHMSSX"
                        + "&type=m3u_plus"
        );
    }

    // =========================
    // CRIAÇÃO DA INTERFACE
    // =========================

    private void createView() {

        rootLayout = new BorderPane();

        rootLayout.setStyle(
                "-fx-background-color: #0F0F0F;"
        );

        // =====================================================
        // MENU LATERAL
        // =====================================================

        menuLateral = new VBox(10);

        menuLateral.setPrefWidth(250);

        menuLateral.setPadding(
                new Insets(30, 15, 30, 15)
        );

        menuLateral.setStyle(
                "-fx-background-color: #000000;"
        );

        Label logo = new Label("PLAYER PRO");

        logo.setStyle(
                "-fx-text-fill: #E50914;" +
                "-fx-font-size: 28px;" +
                "-fx-font-weight: 900;" +
                "-fx-padding: 0 0 30 10;"
        );

        menuLateral.getChildren().addAll(

                logo,

                criarLabelSessao("EXPLORAR"),

                criarBotaoMenu(
                        "🏠 Início",
                        "INICIO"
                ),

                criarBotaoMenu(
                        "🎬 Filmes",
                        "FILME"
                ),

                criarBotaoMenu(
                        "🍿 Séries",
                        "SERIE"
                ),

                criarBotaoMenu(
                        "⛩️ Animes",
                        "ANIME"
                ),

                criarBotaoMenu(
                        "📺 TV Ao Vivo",
                        "CANAL"
                )
        );

        rootLayout.setLeft(menuLateral);

        // =====================================================
        // TOP BAR
        // =====================================================

        topBar = new HBox(15);

        topBar.setAlignment(
                Pos.CENTER_LEFT
        );

        topBar.setPadding(
                new Insets(20, 30, 20, 30)
        );

        // =====================================================
        // MODO DA FONTE
        // =====================================================

        modoFonteBox = new ComboBox<>();

        modoFonteBox.getItems().addAll(
                "Repositório",
                "URL Manual",
                "Arquivo Local"
        );

        modoFonteBox
                .getSelectionModel()
                .selectFirst();

        modoFonteBox.setStyle(
                "-fx-background-color: #333333;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;"
        );

        // =====================================================
        // ÁREA DE INPUT
        // =====================================================

        StackPane inputArea = new StackPane();

        inputArea.setAlignment(
                Pos.CENTER_LEFT
        );

        // =====================================================
        // REPOSITÓRIO
        // =====================================================

        seletorListasRepositorio =
                new ComboBox<>();

        seletorListasRepositorio
                .getItems()
                .addAll(
                        repositorios.keySet()
                );

        seletorListasRepositorio
                .getSelectionModel()
                .selectFirst();

        seletorListasRepositorio.setStyle(
                "-fx-background-color: #222222;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;"
        );

        // =====================================================
        // URL MANUAL
        // =====================================================

        urlManualField = new TextField();

        urlManualField.setPromptText(
                "Cole o link M3U aqui..."
        );

        urlManualField.setPrefWidth(350);

        urlManualField.setStyle(
                "-fx-background-color: #222222;" +
                "-fx-text-fill: white;" +
                "-fx-border-color: #444;" +
                "-fx-border-radius: 3;"
        );

        urlManualField.setVisible(false);

        // =====================================================
        // ARQUIVO LOCAL
        // =====================================================

        HBox boxArquivo =
                new HBox(10);

        boxArquivo.setAlignment(
                Pos.CENTER_LEFT
        );

        btnEscolherArquivo =
                new Button("📂 Procurar");

        btnEscolherArquivo.setStyle(
                "-fx-background-color: #444444;" +
                "-fx-text-fill: white;" +
                "-fx-cursor: hand;"
        );

        nomeArquivoLabel =
                new Label("Nenhum arquivo");

        nomeArquivoLabel.setStyle(
                "-fx-text-fill: #AAAAAA;"
        );

        boxArquivo.getChildren().addAll(
                btnEscolherArquivo,
                nomeArquivoLabel
        );

        boxArquivo.setVisible(false);

        // =====================================================
        // ESCOLHER ARQUIVO
        // =====================================================

        btnEscolherArquivo.setOnAction(event -> {

            FileChooser fc =
                    new FileChooser();

            fc.setTitle(
                    "Selecionar lista IPTV"
            );

            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            "Listas IPTV",
                            "*.m3u",
                            "*.m3u8",
                            "*.txt"
                    )
            );

            arquivoSelecionado =
                    fc.showOpenDialog(
                            rootLayout
                                    .getScene()
                                    .getWindow()
                    );

            if (arquivoSelecionado != null) {

                nomeArquivoLabel.setText(
                        arquivoSelecionado.getName()
                );

                setStatus(
                        "Arquivo selecionado: "
                                + arquivoSelecionado.getName()
                );
            }
        });

        // =====================================================
        // INPUTS
        // =====================================================

        inputArea.getChildren().addAll(
                seletorListasRepositorio,
                urlManualField,
                boxArquivo
        );

        // =====================================================
        // ALTERAÇÃO DO MODO
        // =====================================================

        modoFonteBox
                .valueProperty()
                .addListener(
                        (obs, oldValue, newValue) -> {

                            if (newValue == null) {
                                return;
                            }

                            seletorListasRepositorio
                                    .setVisible(
                                            newValue.equals(
                                                    "Repositório"
                                            )
                                    );

                            urlManualField
                                    .setVisible(
                                            newValue.equals(
                                                    "URL Manual"
                                            )
                                    );

                            boxArquivo
                                    .setVisible(
                                            newValue.equals(
                                                    "Arquivo Local"
                                            )
                                    );
                        }
                );

        // =====================================================
        // BOTÃO CARREGAR
        // =====================================================

        btnCarregar =
                new Button("Carregar");

        btnCarregar.setStyle(
                "-fx-background-color: #E50914;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;"
        );

        btnCarregar.setOnAction(
                event -> carregarFonteSelecionada()
        );

        // =====================================================
        // BUSCA
        // =====================================================

        barraBusca =
                new TextField();

        barraBusca.setPromptText(
                "Buscar..."
        );

        barraBusca.setPrefWidth(220);

        barraBusca.setStyle(
                "-fx-background-color: #1A1A1A;" +
                "-fx-text-fill: white;" +
                "-fx-border-color: #333;" +
                "-fx-border-radius: 5;"
        );

        barraBusca.textProperty()
                .addListener(
                        (obs, oldValue, newValue) ->
                                renderizarFiltrosDeTela()
                );

        // =====================================================
        // STATUS
        // =====================================================

        statusLabel =
                new Label("Pronto");

        statusLabel.setStyle(
                "-fx-text-fill: #888888;" +
                "-fx-font-weight: bold;"
        );

        Region spacer =
                new Region();

        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );

        topBar.getChildren().addAll(
                modoFonteBox,
                inputArea,
                btnCarregar,
                barraBusca,
                spacer,
                statusLabel
        );

        rootLayout.setTop(topBar);

        // =====================================================
        // CONTEÚDO
        // =====================================================

        containerConteudo =
                new VBox(30);

        containerConteudo.setPadding(
                new Insets(
                        10,
                        30,
                        30,
                        30
                )
        );

        scrollConteudoGenerico =
                new ScrollPane(
                        containerConteudo
                );

        scrollConteudoGenerico.setFitToWidth(
                true
        );

        scrollConteudoGenerico.setStyle(
                "-fx-background: transparent;" +
                "-fx-background-color: transparent;" +
                "-fx-border-color: transparent;"
        );

        areaDeConteudo =
                new StackPane(
                        scrollConteudoGenerico
                );

        rootLayout.setCenter(
                areaDeConteudo
        );
    }

    // =====================================================
    // CARREGAR FONTE SELECIONADA
    // =====================================================

    private void carregarFonteSelecionada() {

        String modo =
                modoFonteBox.getValue();

        if (modo == null) {
            setStatus(
                    "Selecione uma fonte."
            );
            return;
        }

        switch (modo) {

            case "Repositório":

                String repositorio =
                        repositorios.get(
                                seletorListasRepositorio
                                        .getValue()
                        );

                baixarListaDaRede(
                        repositorio
                );

                break;

            case "URL Manual":

                String url =
                        urlManualField
                                .getText()
                                .trim();

                if (url.isEmpty()) {

                    setStatus(
                            "Digite uma URL."
                    );

                    return;
                }

                baixarListaDaRede(url);

                break;

            case "Arquivo Local":

                carregarListaDeArquivoLocal();

                break;

            default:

                setStatus(
                        "Modo desconhecido."
                );
        }
    }

    // =====================================================
    // CARREGAR ARQUIVO LOCAL
    // =====================================================

    private void carregarListaDeArquivoLocal() {

        if (arquivoSelecionado == null) {

            setStatus(
                    "Nenhum arquivo selecionado."
            );

            return;
        }

        final File arquivoFinal =
                arquivoSelecionado;

        setStatus(
                "Lendo arquivo..."
        );

        btnCarregar.setDisable(true);

        Task<String> task =
                new Task<String>() {

                    @Override
                    protected String call()
                            throws Exception {

                        byte[] bytes = Files.readAllBytes(arquivoFinal.toPath());
                        return new String(bytes, StandardCharsets.UTF_8);
                    }
                };

        task.setOnSucceeded(event -> {

            btnCarregar.setDisable(false);

            String conteudo =
                    task.getValue();

            processarPayloadLista(
                    conteudo
            );
        });

        task.setOnFailed(event -> {

            btnCarregar.setDisable(false);

            Throwable erro =
                    task.getException();

            String mensagem =
                    erro != null
                            ? erro.getMessage()
                            : "Erro desconhecido.";

            setStatus(
                    "Erro ao ler arquivo: "
                            + mensagem
            );

            if (erro != null) {
                erro.printStackTrace();
            }
        });

        executor.submit(task);
    }

    // =====================================================
    // PROCESSAR PAYLOAD
    // =====================================================

    private void processarPayloadLista(
            String payload) {

        if (payload == null ||
                payload.trim().isEmpty()) {

            setStatus(
                    "Erro: Arquivo/URL vazio."
            );

            return;
        }

        // =================================================
        // RELATÓRIO
        // =================================================

        if (payload.contains(
                "RELATÓRIO PREMIUM")
                ||
                payload.contains(
                        "m3u_Url")) {

            setStatus(
                    "Relatório detectado. "
                            + "Extraindo link real..."
            );

            String urlReal =
                    M3uParserService
                            .extrairUrlDoRelatorio(
                                    payload
                            );

            if (urlReal != null &&
                    !urlReal.trim().isEmpty()) {

                baixarListaDaRede(
                        urlReal
                );

                return;
            }
        }

        // =================================================
        // PARSER
        // =================================================

        try {

            List<MediaItem> lista =
                    M3uParserService.parse(
                            payload
                    );

            if (lista == null) {
                lista =
                        new ArrayList<>();
            }

            final List<MediaItem> listaFinal =
                    new ArrayList<>(lista);

            Platform.runLater(() -> {

                todosOsCanais.clear();

                todosOsCanais.addAll(
                        listaFinal
                );

                if (todosOsCanais.isEmpty()) {

                    statusLabel.setText(
                            "Nenhum item encontrado."
                    );

                    return;
                }

                salvarEmCache();

                atualizarInterfaceDepoisDoCarregamento();

                statusLabel.setText(
                        "Sincronizado ("
                                + todosOsCanais.size()
                                + " itens)."
                );
            });

        } catch (Exception erro) {

            erro.printStackTrace();

            setStatus(
                    "Erro ao processar M3U: "
                            + erro.getMessage()
            );
        }
    }

    // =====================================================
    // BAIXAR LISTA DA INTERNET
    // =====================================================

    private void baixarListaDaRede(
            String url) {

        if (url == null ||
                url.trim().isEmpty()) {

            setStatus(
                    "Erro: URL vazia."
            );

            return;
        }

        final String urlFinal =
                url.trim();

        setStatus(
                "Baixando lista..."
        );

        btnCarregar.setDisable(true);

        Task<String> task =
                new Task<String>() {

                    @Override
                    protected String call()
                            throws Exception {

                        Request request =
                                new Request.Builder()
                                        .url(urlFinal)
                                        .header(
                                                "User-Agent",
                                                "Mozilla/5.0"
                                        )
                                        .header(
                                                "Accept",
                                                "*/*"
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
                                        "HTTP "
                                                + response.code()
                                                + " - "
                                                + response.message()
                                );
                            }

                            if (response.body()
                                    == null) {

                                throw new IOException(
                                        "Servidor retornou "
                                                + "conteúdo vazio."
                                );
                            }

                            String content =
                                    response.body()
                                            .string();

                            if (content == null ||
                                    content.trim()
                                            .isEmpty()) {

                                throw new IOException(
                                        "Lista recebida "
                                                + "está vazia."
                                );
                            }

                            return content;
                        }
                    }
                };

        task.setOnSucceeded(event -> {

            btnCarregar.setDisable(false);

            processarPayloadLista(
                    task.getValue()
            );
        });

        task.setOnFailed(event -> {

            btnCarregar.setDisable(false);

            Throwable erro =
                    task.getException();

            String mensagem =
                    erro != null
                            ? erro.getMessage()
                            : "Erro desconhecido.";

            System.err.println(
                    "======================================"
            );

            System.err.println(
                    "ERRO AO BAIXAR LISTA"
            );

            System.err.println(
                    "URL: " + urlFinal
            );

            System.err.println(
                    "MENSAGEM: " + mensagem
            );

            if (erro != null) {
                erro.printStackTrace();
            }

            System.err.println(
                    "======================================"
            );

            setStatus(
                    "Erro: " + mensagem
            );
        });

        executor.submit(task);
    }

    // =====================================================
    // ATUALIZAR INTERFACE
    // =====================================================

    private void atualizarInterfaceDepoisDoCarregamento() {

        resetarEstiloBotoesMenu();

        atualizarBotaoAtivo();

        renderizarFiltrosDeTela();
    }

    // =====================================================
    // RENDERIZAÇÃO
    // =====================================================

    private void renderizarFiltrosDeTela() {

        if (!Platform.isFxApplicationThread()) {

            Platform.runLater(
                    this::renderizarFiltrosDeTela
            );

            return;
        }

        containerConteudo
                .getChildren()
                .clear();

        String busca =
                barraBusca.getText();

        if (busca == null) {
            busca = "";
        }

        busca =
                busca.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (todosOsCanais.isEmpty()) {
            return;
        }

        final String buscaFinal =
                busca;

        Task<Runnable> task =
                new Task<Runnable>() {

                    @Override
                    protected Runnable call() {

                        Map<String,
                                List<MediaItem>>
                                categorias =
                                new TreeMap<>(
                                        String.CASE_INSENSITIVE_ORDER
                                );

                        if (abaAtiva.equalsIgnoreCase(
                                "SERIE")
                                ||
                                abaAtiva.equalsIgnoreCase(
                                        "ANIME")) {

                            Map<String,
                                    SerieGroup>
                                    agrupados =
                                    OrganizadorDeSeries
                                            .processar(
                                                    todosOsCanais,
                                                    abaAtiva
                                            );

                            Map<String,
                                    List<SerieGroup>>
                                    categorizados =
                                    new TreeMap<>(
                                            String.CASE_INSENSITIVE_ORDER
                                    );

                            for (
                                    SerieGroup group :
                                    agrupados.values()
                            ) {

                                if (
                                        !buscaFinal.isEmpty()
                                        &&
                                        (
                                                group.getTitle()
                                                        == null
                                                ||
                                                !group.getTitle()
                                                        .toLowerCase(
                                                                Locale.ROOT
                                                        )
                                                        .contains(
                                                                buscaFinal
                                                        )
                                        )
                                ) {
                                    continue;
                                }

                                String categoria =
                                        "Diversos";

                                for (
                                        List<MediaItem> eps :
                                        group.getSeasons()
                                                .values()
                                ) {

                                    if (!eps.isEmpty()) {

                                        categoria =
                                                eps.get(0)
                                                        .getGroup();

                                        if (
                                                categoria
                                                        == null
                                                ||
                                                categoria
                                                        .trim()
                                                        .isEmpty()
                                        ) {
                                            categoria =
                                                    "Diversos";
                                        }

                                        break;
                                    }
                                }

                                categorizados
                                        .computeIfAbsent(
                                                categoria,
                                                k ->
                                                        new ArrayList<>()
                                        )
                                        .add(group);
                            }

                            final Map<String,
                                    List<SerieGroup>>
                                    resultado =
                                    categorizados;

                            return () -> {

                                for (
                                        Map.Entry<
                                                String,
                                                List<SerieGroup>>
                                                entry :
                                                resultado.entrySet()
                                ) {

                                    VBox trilho =
                                            criarTrilhoSeries(
                                                    entry.getKey(),
                                                    entry.getValue()
                                            );

                                    containerConteudo
                                            .getChildren()
                                            .add(trilho);
                                }
                            };
                        }

                        // =================================
                        // CANAIS / FILMES / INÍCIO
                        // =================================

                        for (
                                MediaItem item :
                                todosOsCanais
                        ) {

                            boolean pertenceAba;

                            if (
                                    abaAtiva.equalsIgnoreCase(
                                            "INICIO"
                                    )
                            ) {

                                pertenceAba =
                                        true;

                            } else {

                                pertenceAba =
                                        item.getType() != null
                                        &&
                                        item.getType()
                                                .equalsIgnoreCase(
                                                        abaAtiva
                                                );
                            }

                            if (!pertenceAba) {
                                continue;
                            }

                            if (
                                    !buscaFinal.isEmpty()
                                    &&
                                    (
                                            item.getTitle()
                                                    == null
                                            ||
                                            !item.getTitle()
                                                    .toLowerCase(
                                                            Locale.ROOT
                                                    )
                                                    .contains(
                                                            buscaFinal
                                                    )
                                    )
                            ) {
                                continue;
                            }

                            String categoria =
                                    item.getGroup();

                            if (
                                    categoria == null
                                    ||
                                    categoria.trim()
                                            .isEmpty()
                            ) {

                                categoria =
                                        "Diversos";
                            }

                            categorias
                                    .computeIfAbsent(
                                            categoria,
                                            k ->
                                                    new ArrayList<>()
                                    )
                                    .add(item);
                        }

                        final Map<String,
                                List<MediaItem>>
                                resultado =
                                categorias;

                        return () -> {

                            scrollConteudoGenerico
                                    .setVvalue(0);

                            if (
                                    abaAtiva.equalsIgnoreCase(
                                            "INICIO"
                                    )
                            ) {

                                desenharContinuarAssistindo();
                            }

                            for (
                                    Map.Entry<
                                            String,
                                            List<MediaItem>>
                                            entry :
                                            resultado.entrySet()
                            ) {

                                VBox trilho =
                                        criarTrilho(
                                                entry.getKey(),
                                                entry.getValue()
                                        );

                                containerConteudo
                                        .getChildren()
                                        .add(trilho);
                            }
                        };
                    }
                };

        task.setOnSucceeded(event -> {

            Runnable ui =
                    task.getValue();

            if (ui != null) {
                ui.run();
            }
        });

        task.setOnFailed(event -> {

            Throwable erro =
                    task.getException();

            statusLabel.setText(
                    "Erro ao renderizar: "
                            +
                            (
                                    erro != null
                                            &&
                                            erro.getMessage()
                                                    != null
                                            ?
                                            erro.getMessage()
                                            :
                                            "erro desconhecido"
                            )
            );
        });

        executor.submit(task);
    }

    // =====================================================
    // TRILHO NORMAL
    // =====================================================

    private VBox criarTrilho(
            String tituloCategoria,
            List<MediaItem> itens) {

        VBox trilhoBox =
                new VBox(10);

        trilhoBox.setPadding(
                new Insets(
                        0,
                        0,
                        20,
                        0
                )
        );

        Label titulo =
                new Label(
                        tituloCategoria.toUpperCase()
                );

        titulo.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 18px;" +
                "-fx-font-weight: 900;" +
                "-fx-padding: 0 0 5 0;"
        );

        HBox cardContainer =
                new HBox(15);

        cardContainer.setAlignment(
                Pos.CENTER_LEFT
        );

        ScrollPane scrollHorizontal =
                criarScrollHorizontal(
                        cardContainer
                );

        final int[] index =
                {0};

        final int BATCH = 25;

        Runnable carregarMais = () -> {

            int limit =
                    Math.min(
                            index[0] + BATCH,
                            itens.size()
                    );

            if (index[0] >= limit) {
                return;
            }

            List<MediaCard> novosCards =
                    new ArrayList<>();

            for (
                    int i = index[0];
                    i < limit;
                    i++
            ) {

                MediaItem item =
                        itens.get(i);

                MediaCard card =
                        new MediaCard(
                                item.getTitle(),
                                item.getLogo(),
                                item.getUrl(),
                                () ->
                                        processarCliqueCanal(
                                                item.getTitle(),
                                                item.getUrl()
                                        )
                        );

                novosCards.add(card);
            }

            cardContainer
                    .getChildren()
                    .addAll(novosCards);

            index[0] = limit;
        };

        carregarMais.run();

        scrollHorizontal
                .hvalueProperty()
                .addListener(
                        (obs, oldValue, newValue) -> {

                            if (
                                    newValue.doubleValue()
                                            >= 0.70
                                    &&
                                    index[0] < itens.size()
                            ) {

                                carregarMais.run();
                            }
                        }
                );

        trilhoBox
                .getChildren()
                .addAll(
                        titulo,
                        scrollHorizontal
                );

        return trilhoBox;
    }

    // =====================================================
    // TRILHO DE SÉRIES
    // =====================================================

    private VBox criarTrilhoSeries(
            String tituloCategoria,
            List<SerieGroup> series) {

        VBox trilhoBox =
                new VBox(10);

        trilhoBox.setPadding(
                new Insets(
                        0,
                        0,
                        20,
                        0
                )
        );

        Label titulo =
                new Label(
                        tituloCategoria.toUpperCase()
                );

        titulo.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 18px;" +
                "-fx-font-weight: 900;" +
                "-fx-padding: 0 0 5 0;"
        );

        HBox cardContainer =
                new HBox(15);

        cardContainer.setAlignment(
                Pos.CENTER_LEFT
        );

        ScrollPane scrollHorizontal =
                criarScrollHorizontal(
                        cardContainer
                );

        final int[] index =
                {0};

        final int BATCH = 25;

        Runnable carregarMais = () -> {

            int limit =
                    Math.min(
                            index[0] + BATCH,
                            series.size()
                    );

            if (index[0] >= limit) {
                return;
            }

            List<MediaCard> novosCards =
                    new ArrayList<>();

            for (
                    int i = index[0];
                    i < limit;
                    i++
            ) {

                SerieGroup group =
                        series.get(i);

                novosCards.add(
                        new MediaCard(
                                group.getTitle(),
                                group.getLogo(),
                                "",
                                () ->
                                        mostrarDetalhesSerie(
                                                group
                                        )
                        )
                );
            }

            cardContainer
                    .getChildren()
                    .addAll(novosCards);

            index[0] = limit;
        };

        carregarMais.run();

        scrollHorizontal
                .hvalueProperty()
                .addListener(
                        (obs, oldValue, newValue) -> {

                            if (
                                    newValue.doubleValue()
                                            >= 0.70
                                    &&
                                    index[0] < series.size()
                            ) {

                                carregarMais.run();
                            }
                        }
                );

        trilhoBox
                .getChildren()
                .addAll(
                        titulo,
                        scrollHorizontal
                );

        return trilhoBox;
    }

    // =====================================================
    // SCROLL HORIZONTAL
    // =====================================================

    private ScrollPane criarScrollHorizontal(
            HBox container) {

        ScrollPane scroll =
                new ScrollPane(container);

        scroll.setVbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        scroll.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        scroll.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-background: transparent;" +
                "-fx-border-color: transparent;"
        );

        scroll.setPannable(true);

        scroll.setOnScroll(event -> {

            if (event.getDeltaY() != 0) {

                double largura =
                        Math.max(
                                scroll.getWidth(),
                                1
                        );

                double speed =
                        (event.getDeltaY()
                                / largura)
                                * 2.5;

                scroll.setHvalue(
                        scroll.getHvalue()
                                - speed
                );

                event.consume();
            }
        });

        return scroll;
    }

    // =====================================================
    // CONTINUAR ASSISTINDO
    // =====================================================

    private void desenharContinuarAssistindo() {

        Map<String, Long>
                historicoSalvo =
                HistoricoManager
                        .getTodosOsProgressos();

        if (
                historicoSalvo == null
                ||
                historicoSalvo.isEmpty()
        ) {
            return;
        }

        VBox trilho =
                new VBox(10);

        Label lbl =
                new Label(
                        "Continuar Assistindo"
                );

        lbl.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 22px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 0 0 10 0;"
        );

        HBox cardContainer =
                new HBox(20);

        ScrollPane scrollHorizontal =
                criarScrollHorizontal(
                        cardContainer
                );

        int count = 0;

        for (
                MediaItem item :
                todosOsCanais
        ) {

            if (
                    item.getUrl() == null
                    ||
                    !historicoSalvo
                            .containsKey(
                                    item.getUrl()
                            )
            ) {
                continue;
            }

            Long tempoSalvo =
                    historicoSalvo.get(
                            item.getUrl()
                    );

            if (
                    tempoSalvo == null
                    ||
                    tempoSalvo <= 10
            ) {
                continue;
            }

            MediaCard card =
                    new MediaCard(
                            item.getTitle(),
                            item.getLogo(),
                            item.getUrl(),
                            () ->
                                    processarCliqueCanal(
                                            item.getTitle(),
                                            item.getUrl()
                                    )
                    );

            cardContainer
                    .getChildren()
                    .add(card);

            count++;
        }

        if (count > 0) {

            trilho.getChildren().addAll(
                    lbl,
                    scrollHorizontal
            );

            containerConteudo
                    .getChildren()
                    .add(
                            0,
                            trilho
                    );
        }
    }

    // =====================================================
    // DETALHES DA SÉRIE
    // =====================================================

    private void mostrarDetalhesSerie(
            SerieGroup group) {

        containerConteudo
                .getChildren()
                .clear();

        scrollConteudoGenerico
                .setVvalue(0);

        VBox header =
                new VBox(20);

        header.setPadding(
                new Insets(
                        0,
                        0,
                        30,
                        0
                )
        );

        Button btnVoltar =
                new Button("← VOLTAR");

        btnVoltar.setStyle(
                "-fx-background-color: #E50914;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8 20;" +
                "-fx-background-radius: 5;" +
                "-fx-cursor: hand;"
        );

        btnVoltar.setOnAction(
                event ->
                        renderizarFiltrosDeTela()
        );

        Label titulo =
                new Label(
                        group.getTitle()
                );

        titulo.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 42px;" +
                "-fx-font-weight: 900;"
        );

        header.getChildren().addAll(
                btnVoltar,
                titulo
        );

        containerConteudo
                .getChildren()
                .add(header);

        for (
                Map.Entry<
                        String,
                        List<MediaItem>>
                        season :
                        group.getSeasons()
                                .entrySet()
        ) {

            TitledPane tp =
                    new TitledPane();

            tp.setText(
                    season.getKey()
                            .toUpperCase()
            );

            tp.setExpanded(
                    season.getKey()
                            .equalsIgnoreCase(
                                    "TEMPORADA 1"
                            )
            );

            tp.setStyle(
                    "-fx-base: #111;" +
                    "-fx-text-fill: #E50914;" +
                    "-fx-font-weight: bold;"
            );

            VBox eps =
                    new VBox(5);

            eps.setStyle(
                    "-fx-background-color: #0F0F0F;" +
                    "-fx-padding: 10;"
            );

            for (
                    MediaItem ep :
                    season.getValue()
            ) {

                Button b =
                        new Button(
                                "▶  "
                                        + ep.getTitle()
                        );

                b.setMaxWidth(
                        Double.MAX_VALUE
                );

                b.setAlignment(
                        Pos.CENTER_LEFT
                );

                b.setStyle(
                        "-fx-background-color: transparent;" +
                        "-fx-text-fill: #EEE;" +
                        "-fx-padding: 12;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: #222;" +
                        "-fx-border-width: 0 0 1 0;"
                );

                b.setOnMouseEntered(
                        event ->
                                b.setStyle(
                                        "-fx-background-color: #1A1A1A;" +
                                        "-fx-text-fill: white;" +
                                        "-fx-padding: 12;" +
                                        "-fx-cursor: hand;" +
                                        "-fx-border-color: #E50914;" +
                                        "-fx-border-width: 0 0 1 0;"
                                )
                );

                b.setOnMouseExited(
                        event ->
                                b.setStyle(
                                        "-fx-background-color: transparent;" +
                                        "-fx-text-fill: #EEE;" +
                                        "-fx-padding: 12;" +
                                        "-fx-cursor: hand;" +
                                        "-fx-border-color: #222;" +
                                        "-fx-border-width: 0 0 1 0;"
                                )
                );

                b.setOnAction(
                        event ->
                                processarCliqueCanal(
                                        ep.getTitle(),
                                        ep.getUrl()
                                )
                );

                eps.getChildren()
                        .add(b);
            }

            tp.setContent(eps);

            containerConteudo
                    .getChildren()
                    .add(tp);
        }
    }

    // =====================================================
    // CLIQUE EM MÍDIA
    // =====================================================

    private void processarCliqueCanal(
        String titulo,
        String urlBruta) {

    if (urlBruta == null ||
            urlBruta.trim().isEmpty()) {

        setStatus("URL da mídia inválida.");
        return;
    }

    final String url =
            urlBruta.trim();

    final String urlLower =
            url.toLowerCase(Locale.ROOT);

    // =====================================================
    // P2P / ACESTREAM
    // =====================================================

    if (urlLower.startsWith("p2p://") ||
            urlLower.startsWith("acestream://")) {

        setStatus(
                "Sinal P2P detectado. Negociando nós..."
        );

        try {

            P2PStreamManager p2pManager =
                    new P2PStreamManager();

            Task<String> task =
                    new Task<String>() {

                        @Override
                        protected String call()
                                throws Exception {

                            try {

                                return p2pManager
                                        .resolveP2PToLocalHttp(
                                                url
                                        )
                                        .get();

                            } catch (Exception e) {

                                throw new Exception(
                                        "Falha ao resolver fluxo P2P.",
                                        e
                                );
                            }
                        }
                    };

            task.setOnSucceeded(event -> {

                String localHttpUrl =
                        task.getValue();

                if (localHttpUrl == null ||
                        localHttpUrl.trim().isEmpty()) {

                    setStatus(
                            "Falha ao abrir fluxo P2P. "
                                    + "A Engine está rodando?"
                    );

                    return;
                }

                System.out.println(
                        "P2P resolvido: "
                                + localHttpUrl
                );

                setStatus(
                        "Fluxo P2P conectado."
                );

                abrirModoCinema(
                        titulo,
                        localHttpUrl
                );
            });

            task.setOnFailed(event -> {

                Throwable erro =
                        task.getException();

                System.err.println(
                        "======================================"
                );

                System.err.println(
                        "ERRO NO FLUXO P2P"
                );

                if (erro != null) {
                    erro.printStackTrace();
                }

                System.err.println(
                        "======================================"
                );

                setStatus(
                        "Falha ao abrir fluxo P2P. "
                                + "Verifique se a Engine está rodando."
                );
            });

            executor.submit(task);

        } catch (Exception e) {

            e.printStackTrace();

            setStatus(
                    "Erro ao iniciar Engine P2P."
            );
        }

        return;
    }

    // =====================================================
    // STREAM NORMAL
    // =====================================================

    setStatus(
            "Abrindo: " + titulo
    );

    abrirModoCinema(
            titulo,
            url
    );
}

    // =====================================================
    // ABRIR PLAYER
    // =====================================================

    private void abrirModoCinema(
            String titulo,
            String url) {

        Platform.runLater(() -> {

            rootLayout.setLeft(null);

            rootLayout.setTop(null);

            rootLayout.setCenter(
                    playerView.getView()
            );

            playerView.playMedia(
                    titulo,
                    url
            );
        });
    }

    // =====================================================
    // FECHAR PLAYER
    // =====================================================

    private void fecharModoCinema() {

        Platform.runLater(() -> {

            try {
                playerView.stopMedia();
            } catch (Exception ignored) {
            }

            rootLayout.setLeft(
                    menuLateral
            );

            rootLayout.setTop(
                    topBar
            );

            rootLayout.setCenter(
                    areaDeConteudo
            );

            renderizarFiltrosDeTela();
        });
    }

    // =====================================================
    // CACHE
    // =====================================================

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

        } catch (IOException erro) {

            System.err.println(
                    "Erro ao salvar cache: "
                            + erro.getMessage()
            );
        }
    }

    // =====================================================
    // CARREGAR CACHE
    // =====================================================

    private void carregarDoCacheOuArquivoLocal() {

        if (!CACHE_FILE.exists()) {
            return;
        }

        Task<List<MediaItem>> task =
                new Task<List<MediaItem>>() {

                    @Override
                    protected List<MediaItem> call()
                            throws Exception {

                        try (
                                FileReader reader =
                                        new FileReader(
                                                CACHE_FILE
                                        )
                        ) {

                            List<MediaItem> cache =
                                    gson.fromJson(
                                            reader,
                                            new TypeToken<
                                                    ArrayList<MediaItem>
                                                    >() {
                                            }.getType()
                                    );

                            if (cache == null) {
                                return new ArrayList<>();
                            }

                            return cache;
                        }
                    }
                };

        task.setOnSucceeded(
                event -> {

                    List<MediaItem> cache =
                            task.getValue();

                    todosOsCanais =
                            cache != null
                                    ?
                                    cache
                                    :
                                    new ArrayList<>();

                    if (
                            !todosOsCanais.isEmpty()
                    ) {

                        setStatus(
                                "Carregado do cache ("
                                        + todosOsCanais.size()
                                        + " itens)."
                        );

                        renderizarFiltrosDeTela();
                    }
                }
        );

        task.setOnFailed(
                event -> {

                    Throwable erro =
                            task.getException();

                    System.err.println(
                            "Erro ao carregar cache."
                    );

                    if (erro != null) {
                        erro.printStackTrace();
                    }
                }
        );

        executor.submit(task);
    }

    // =====================================================
    // BOTÕES DO MENU
    // =====================================================

    private Button criarBotaoMenu(
            String texto,
            String idTela) {

        Button btn =
                new Button(texto);

        btn.setMaxWidth(
                Double.MAX_VALUE
        );

        btn.setAlignment(
                Pos.CENTER_LEFT
        );

        aplicarEstiloBotaoMenu(
                btn,
                false
        );

        btn.setOnMouseEntered(
                event -> {

                    if (!abaAtiva.equals(idTela)) {

                        btn.setStyle(
                                "-fx-background-color: #111;" +
                                "-fx-text-fill: #EEE;" +
                                "-fx-font-size: 16px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 12 20;" +
                                "-fx-background-radius: 10;"
                        );
                    }
                }
        );

        btn.setOnMouseExited(
                event -> {

                    if (!abaAtiva.equals(idTela)) {

                        aplicarEstiloBotaoMenu(
                                btn,
                                false
                        );
                    }
                }
        );

        btn.setOnAction(
                event -> {

                    abaAtiva =
                            idTela;

                    atualizarBotaoAtivo();

                    renderizarFiltrosDeTela();
                }
        );

        return btn;
    }

    // =====================================================
    // ESTILO DOS BOTÕES
    // =====================================================

    private void aplicarEstiloBotaoMenu(
            Button btn,
            boolean ativo) {

        if (ativo) {

            btn.setStyle(
                    "-fx-background-color: #E50914;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 16px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 12 20;" +
                    "-fx-background-radius: 10;"
            );

        } else {

            btn.setStyle(
                    "-fx-background-color: transparent;" +
                    "-fx-text-fill: #888;" +
                    "-fx-font-size: 16px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 12 20;"
            );
        }
    }

    // =====================================================
    // RESET MENU
    // =====================================================

    private void resetarEstiloBotoesMenu() {

        for (
                Node node :
                menuLateral.getChildren()
        ) {

            if (node instanceof Button) {

                aplicarEstiloBotaoMenu(
                        (Button) node,
                        false
                );
            }
        }
    }

    // =====================================================
    // BOTÃO ATIVO
    // =====================================================

    private void atualizarBotaoAtivo() {

        for (
                Node node :
                menuLateral.getChildren()
        ) {

            if (!(node instanceof Button)) {
                continue;
            }

            Button btn =
                    (Button) node;

            String texto =
                    btn.getText();

            boolean ativo = false;

            if (
                    abaAtiva.equals("INICIO")
                    &&
                    texto.contains("Início")
            ) {
                ativo = true;
            }

            else if (
                    abaAtiva.equals("FILME")
                    &&
                    texto.contains("Filmes")
            ) {
                ativo = true;
            }

            else if (
                    abaAtiva.equals("SERIE")
                    &&
                    texto.contains("Séries")
            ) {
                ativo = true;
            }

            else if (
                    abaAtiva.equals("ANIME")
                    &&
                    texto.contains("Animes")
            ) {
                ativo = true;
            }

            else if (
                    abaAtiva.equals("CANAL")
                    &&
                    texto.contains("TV Ao Vivo")
            ) {
                ativo = true;
            }

            aplicarEstiloBotaoMenu(
                    btn,
                    ativo
            );
        }
    }

    // =====================================================
    // STATUS
    // =====================================================

    private void setStatus(
            String mensagem) {

        Platform.runLater(() -> {

            if (statusLabel != null) {

                statusLabel.setText(
                        mensagem != null
                                ?
                                mensagem
                                :
                                ""
                );
            }
        });
    }

    // =====================================================
    // LABEL DE SEÇÃO
    // =====================================================

    private Label criarLabelSessao(
            String texto) {

        Label lbl =
                new Label(texto);

        lbl.setStyle(
                "-fx-text-fill: #444;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 20 0 10 10;"
        );

        return lbl;
    }

    // =====================================================
    // ENCERRAMENTO
    // =====================================================

    public void shutdown() {

        try {

            executor.shutdownNow();

        } catch (Exception ignored) {
        }

        try {

            playerView.stopMedia();

        } catch (Exception ignored) {
        }
    }
}