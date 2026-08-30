package com.seuprojeto;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurface;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlayerView {

    private VBox view;

    private Label channelTitleLabel;
    private Label timeLabel;
    private Label statusLabel;

    private StackPane videoPane;
    private ImageView videoImageView;

    private Slider timeSlider;

    private Button playBtn;
    private Button pauseBtn;
    private Button stopBtn;

    private boolean isUserDraggingSlider = false;
    private boolean isClosing = false;
    private boolean isLiveStream = false;
    private boolean mediaReady = false;

    private String currentUrl;
    private String currentChannelName;

    private long currentLength = 0;

    private final MediaPlayerFactory mediaPlayerFactory;
    private final EmbeddedMediaPlayer mediaPlayer;

    // AVISO: Removido o 'final' para permitir a recriação do pool de threads
    private ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    private final Runnable onBackAction;

    public PlayerView(Runnable onBackAction) {

        this.onBackAction = onBackAction;

        /*
         * Configuração do VLC.
         *
         * Valores mais conservadores para IPTV:
         * - network-caching: buffer de rede
         * - clock-jitter: reduz problemas de sincronização
         * - no-video-title-show: remove título do VLC
         * - avcodec-hw: aceleração de hardware quando disponível
         */
        String[] vlcArgs = {
                "--network-caching=3000",
                "--live-caching=3000",
                "--file-caching=3000",
                "--clock-jitter=500",
                "--clock-synchro=0",
                "--no-video-title-show",
                "--avcodec-hw=any",
                "--no-stats"
        };

        mediaPlayerFactory = new MediaPlayerFactory(vlcArgs);

        mediaPlayer =
                mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();

        videoImageView = new ImageView();
        videoImageView.setPreserveRatio(true);

        mediaPlayer.videoSurface().set(
                new ImageViewVideoSurface(videoImageView)
        );

        configurarEventosVLC();
        createView();
    }

    public VBox getView() {
        return view;
    }

    private void createView() {

        view = new VBox(0);
        view.setStyle("-fx-background-color: black;");

        // =========================================================
        // BARRA SUPERIOR
        // =========================================================

        HBox topBar = new HBox(15);

        topBar.setPadding(new Insets(12));
        topBar.setAlignment(Pos.CENTER_LEFT);

        topBar.setStyle(
                "-fx-background-color: rgba(10,10,10,0.90);"
        );

        Button backButton = new Button("⬅ Voltar");

        backButton.setStyle(
                "-fx-background-color: #E50914;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 14px;" +
                "-fx-padding: 8 16 8 16;" +
                "-fx-cursor: hand;"
        );

        backButton.setOnAction(event -> voltar());

        channelTitleLabel = new Label("Carregando...");

        channelTitleLabel.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;"
        );

        statusLabel = new Label("");

        statusLabel.setStyle(
                "-fx-text-fill: #cccccc;" +
                "-fx-font-size: 12px;"
        );

        topBar.getChildren().addAll(
                backButton,
                channelTitleLabel,
                statusLabel
        );

        // =========================================================
        // ÁREA DO VÍDEO
        // =========================================================

        videoPane = new StackPane();

        videoPane.setStyle(
                "-fx-background-color: black;"
        );

        videoPane.getChildren().add(videoImageView);

        VBox.setVgrow(videoPane, Priority.ALWAYS);

        videoImageView.fitWidthProperty()
                .bind(videoPane.widthProperty());

        videoImageView.fitHeightProperty()
                .bind(videoPane.heightProperty());

        // =========================================================
        // PAINEL INFERIOR
        // =========================================================

        VBox bottomPane = new VBox(10);

        bottomPane.setPadding(new Insets(12));

        bottomPane.setStyle(
                "-fx-background-color: rgba(10,10,10,0.95);"
        );

        // =========================================================
        // LINHA DO TEMPO
        // =========================================================

        HBox timelineBox = new HBox(12);

        timelineBox.setAlignment(Pos.CENTER);

        timeSlider = new Slider(0, 100, 0);

        timeSlider.setDisable(true);

        HBox.setHgrow(
                timeSlider,
                Priority.ALWAYS
        );

        timeLabel = new Label("00:00:00 / 00:00:00");

        timeLabel.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-family: monospace;" +
                "-fx-font-size: 13px;"
        );

        // Usuário começou a arrastar
        timeSlider.setOnMousePressed(event -> {

            if (isLiveStream || !mediaReady) {
                return;
            }

            isUserDraggingSlider = true;
        });

        // Usuário soltou
        timeSlider.setOnMouseReleased(event -> {

            if (isLiveStream || !mediaReady) {
                isUserDraggingSlider = false;
                return;
            }

            executarSeek();

            isUserDraggingSlider = false;
        });

        timelineBox.getChildren().addAll(
                timeSlider,
                timeLabel
        );

        // =========================================================
        // CONTROLES
        // =========================================================

        HBox controlBar = new HBox(12);

        controlBar.setAlignment(Pos.CENTER);

        playBtn = criarBotao(
                "▶",
                "#E50914"
        );

        pauseBtn = criarBotao(
                "⏸",
                "#E50914"
        );

        stopBtn = criarBotao(
                "⏹",
                "#555555"
        );

        playBtn.setOnAction(event -> {

            if (!isClosing) {
                mediaPlayer.controls().play();
            }
        });

        pauseBtn.setOnAction(event -> {

            if (!isClosing) {
                mediaPlayer.controls().pause();
            }
        });

        stopBtn.setOnAction(event -> {

            if (!isClosing) {
                salvarProgresso();
                mediaPlayer.controls().stop();
            }
        });

        controlBar.getChildren().addAll(
                playBtn,
                pauseBtn,
                stopBtn
        );

        bottomPane.getChildren().addAll(
                timelineBox,
                controlBar
        );

        view.getChildren().addAll(
                topBar,
                videoPane,
                bottomPane
        );
    }

    private Button criarBotao(
            String texto,
            String cor
    ) {

        Button button = new Button(texto);

        button.setStyle(
                "-fx-background-color: " + cor + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-min-width: 55px;" +
                "-fx-min-height: 38px;" +
                "-fx-cursor: hand;"
        );

        return button;
    }

    // =============================================================
    // EVENTOS DO VLC
    // =============================================================

    private void configurarEventosVLC() {

        mediaPlayer.events().addMediaPlayerEventListener(
                new MediaPlayerEventAdapter() {

                    @Override
                    public void playing(MediaPlayer player) {

                        Platform.runLater(() -> {

                            mediaReady = true;

                            statusLabel.setText(
                                    isLiveStream
                                            ? "AO VIVO"
                                            : "Reproduzindo"
                            );

                            if (!isLiveStream) {
                                timeSlider.setDisable(false);
                            }
                        });
                    }

                    @Override
                    public void paused(MediaPlayer player) {

                        Platform.runLater(() ->
                                statusLabel.setText("Pausado")
                        );
                    }

                    @Override
                    public void stopped(MediaPlayer player) {

                        Platform.runLater(() -> {

                            mediaReady = false;

                            statusLabel.setText("Parado");

                            if (isLiveStream) {
                                timeSlider.setValue(0);
                            }
                        });
                    }

                    @Override
                    public void finished(MediaPlayer player) {

                        Platform.runLater(() -> {

                            statusLabel.setText("Finalizado");

                            if (!isLiveStream) {
                                timeSlider.setValue(100);
                            }
                        });
                    }

                    @Override
                    public void error(MediaPlayer player) {

                        Platform.runLater(() -> {

                            mediaReady = false;

                            statusLabel.setText(
                                    "Erro ao reproduzir"
                            );

                            mostrarErro(
                                    "O VLC não conseguiu reproduzir este conteúdo."
                            );
                        });
                    }

                    @Override
                    public void timeChanged(
                            MediaPlayer player,
                            long newTime
                    ) {

                        if (isClosing) {
                            return;
                        }

                        Platform.runLater(() -> {

                            if (isUserDraggingSlider) {
                                return;
                            }

                            long length =
                                    player.status().length();

                            currentLength = length;

                            if (length <= 0) {

                                // ==============================
                                // STREAM AO VIVO
                                // ==============================

                                isLiveStream = true;

                                timeSlider.setDisable(true);

                                timeSlider.setValue(0);

                                timeLabel.setText(
                                        "AO VIVO"
                                );

                            } else {

                                // ==============================
                                // VOD / ARQUIVO
                                // ==============================

                                isLiveStream = false;

                                timeSlider.setDisable(false);

                                float position =
                                        player.status().position();

                                if (position >= 0 &&
                                        position <= 1) {

                                    timeSlider.setValue(
                                            position * 100
                                    );
                                }

                                timeLabel.setText(
                                        formatarTempo(newTime)
                                                + " / " +
                                        formatarTempo(length)
                                );
                            }
                        });
                    }
                }
        );
    }

    // =============================================================
    // REPRODUZIR MÍDIA
    // =============================================================

    public synchronized void playMedia(
            String channelName,
            String url
    ) {

        if (isClosing) {
            return;
        }

        if (url == null ||
                url.trim().isEmpty()) {

            mostrarErro(
                    "URL do canal inválida."
            );

            return;
        }

        String novaUrl = url.trim();

        if (currentUrl != null &&
                currentUrl.equals(novaUrl) &&
                mediaReady) {

            return;
        }

        salvarProgresso();

        // Chama o método seguro que limpa e recria as threads
        criarNovoScheduler();

        try {
            mediaPlayer.controls().stop();
        } catch (Exception ignored) {
        }

        currentUrl = novaUrl;

        currentChannelName =
                channelName != null &&
                !channelName.trim().isEmpty()
                        ? channelName.trim()
                        : "Sem nome";

        channelTitleLabel.setText(
                currentChannelName
        );

        statusLabel.setText(
                "Conectando..."
        );

        mediaReady = false;

        currentLength = 0;

        isLiveStream = false;

        timeSlider.setValue(0);

        timeSlider.setDisable(true);

        timeLabel.setText(
                "00:00:00 / 00:00:00"
        );

        try {

            boolean iniciou =
                    mediaPlayer.media().play(
                            currentUrl
                    );

            if (!iniciou) {

                statusLabel.setText(
                        "Falha ao iniciar"
                );

                mostrarErro(
                        "Não foi possível iniciar o stream."
                );

                return;
            }

        } catch (Exception e) {

            statusLabel.setText(
                    "Erro"
            );

            mostrarErro(
                    "Erro ao abrir o stream:\n"
                            + e.getMessage()
            );

            return;
        }

        // =========================================================
        // TENTAR RETOMAR PROGRESSO
        // =========================================================

        final String urlParaRetomar =
                currentUrl;

        long progresso = 0;

        try {

            progresso =
                    HistoricoManager.carregarProgresso(
                            urlParaRetomar
                    );

        } catch (Exception ignored) {
        }

        final long progressoFinal =
                progresso;

        if (progressoFinal > 0) {

            scheduler.schedule(() -> {

                if (isClosing) {
                    return;
                }

                if (!urlParaRetomar.equals(currentUrl)) {
                    return;
                }

                Platform.runLater(() -> {

                    if (isClosing) {
                        return;
                    }

                    if (!urlParaRetomar.equals(currentUrl)) {
                        return;
                    }

                    try {

                        long length =
                                mediaPlayer.status().length();

                        if (length > 0) {

                            long destino =
                                    progressoFinal * 1000L;

                            if (destino >= length) {

                                destino =
                                        Math.max(
                                                0,
                                                length - 5000
                                        );
                            }

                            if (destino > 0) {

                                mediaPlayer.controls()
                                        .setTime(destino);
                            }
                        }

                    } catch (Exception ignored) {
                    }
                });

            }, 3, TimeUnit.SECONDS);
        }
    }

    // =============================================================
    // CRIA NOVO SCHEDULER
    // =============================================================

    private void criarNovoScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    // =============================================================
    // SEEK
    // =============================================================

    private void executarSeek() {

        if (isLiveStream ||
                !mediaReady ||
                currentLength <= 0) {

            return;
        }

        try {

            double valor =
                    timeSlider.getValue() / 100.0;

            long destino =
                    (long) (
                            currentLength * valor
                    );

            if (destino < 0) {
                destino = 0;
            }

            if (destino > currentLength) {
                destino = currentLength;
            }

            mediaPlayer.controls()
                    .setTime(destino);

        } catch (Exception ignored) {
        }
    }

    // =============================================================
    // SALVAR PROGRESSO
    // =============================================================

    private void salvarProgresso() {

        if (currentUrl == null ||
                currentUrl.trim().isEmpty()) {

            return;
        }

        if (isLiveStream) {
            return;
        }

        if (!mediaReady) {
            return;
        }

        try {

            long tempo =
                    mediaPlayer.status().time();

            long duracao =
                    mediaPlayer.status().length();

            if (tempo <= 0) {
                return;
            }

            if (duracao > 0 &&
                    tempo >= duracao - 3000) {

                HistoricoManager.salvarProgresso(
                        currentUrl,
                        0
                );

                return;
            }

            HistoricoManager.salvarProgresso(
                    currentUrl,
                    tempo / 1000
            );

        } catch (Exception ignored) {
        }
    }

    // =============================================================
    // VOLTAR
    // =============================================================

    private void voltar() {

        if (isClosing) {
            return;
        }

        salvarProgresso();

        stopMedia();

        if (onBackAction != null) {

            Platform.runLater(
                    onBackAction
            );
        }
    }

    // =============================================================
    // STOP
    // =============================================================

    public synchronized void stopMedia() {

        try {

            salvarProgresso();

        } catch (Exception ignored) {
        }

        try {

            mediaPlayer.controls()
                    .stop();

        } catch (Exception ignored) {
        }

        mediaReady = false;
    }

    // =============================================================
    // FECHAR PLAYER
    // =============================================================

    public synchronized void dispose() {

        if (isClosing) {
            return;
        }

        isClosing = true;

        try {
            salvarProgresso();
        } catch (Exception ignored) {
        }

        try {
            mediaPlayer.controls().stop();
        } catch (Exception ignored) {
        }

        try {
            mediaPlayer.release();
        } catch (Exception ignored) {
        }

        try {
            mediaPlayerFactory.release();
        } catch (Exception ignored) {
        }

        try {
            if (scheduler != null) {
                scheduler.shutdownNow();
            }
        } catch (Exception ignored) {
        }
    }

    // =============================================================
    // FORMATA TEMPO
    // =============================================================

    private String formatarTempo(long millis) {

        if (millis < 0) {
            millis = 0;
        }

        long segundos =
                millis / 1000;

        long horas =
                segundos / 3600;

        long minutos =
                (segundos % 3600) / 60;

        long segundosRestantes =
                segundos % 60;

        return String.format(
                "%02d:%02d:%02d",
                horas,
                minutos,
                segundosRestantes
        );
    }

    // =============================================================
    // MENSAGEM DE ERRO
    // =============================================================

    private void mostrarErro(String mensagem) {

        Platform.runLater(() -> {

            Alert alert =
                    new Alert(
                            Alert.AlertType.ERROR
                    );

            alert.setTitle(
                    "Erro no Player"
            );

            alert.setHeaderText(
                    "Não foi possível reproduzir"
            );

            alert.setContentText(
                    mensagem
            );

            alert.show();
        });
    }
}