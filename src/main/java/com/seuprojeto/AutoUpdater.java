package com.seuprojeto;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Optional;

public class AutoUpdater {

    // A VERSÃO ATUAL DO SEU APP COMPILADO (Mude isso a cada nova build)
    private static final String CURRENT_VERSION = "1.2";
    
    // O LINK RAW DO SEU JSON DE CONTROLE NO GITHUB
    private static final String UPDATE_JSON_URL = "https://raw.githubusercontent.com/jeffersonjaily/iptv_app/main/PlayerJava/src/main/update.json";
    
    private final OkHttpClient httpClient;
    private final Gson gson;

    public AutoUpdater() {
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
    }

    public void checkForUpdates() {
        Task<Void> checkTask = new Task<>() {
            @Override
            protected Void call() {
                try {
                    Request request = new Request.Builder().url(UPDATE_JSON_URL).build();
                    try (Response response = httpClient.newCall(request).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            String json = response.body().string();
                            JsonObject updateData = gson.fromJson(json, JsonObject.class);
                            
                            String latestVersion = updateData.get("version").getAsString();
                            String downloadUrl = updateData.get("url").getAsString();
                            String notes = updateData.get("notes").getAsString();

                            // Se a versão do GitHub for diferente da versão atual, engatilha o update
                            if (!CURRENT_VERSION.equals(latestVersion)) {
                                Platform.runLater(() -> promptUpdate(latestVersion, downloadUrl, notes));
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Falha ao checar atualizações: " + e.getMessage());
                }
                return null;
            }
        };
        
        Thread thread = new Thread(checkTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void promptUpdate(String latestVersion, String downloadUrl, String notes) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Atualização Disponível");
        alert.setHeaderText("Nova versão " + latestVersion + " está disponível!");
        alert.setContentText("Notas de atualização:\n" + notes + "\n\nDeseja baixar e instalar agora?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            startDownloadAndInstall(downloadUrl);
        }
    }

    private void startDownloadAndInstall(String downloadUrl) {
        Alert progressAlert = new Alert(Alert.AlertType.NONE);
        progressAlert.setTitle("Atualizando...");
        progressAlert.setHeaderText("Baixando atualização, por favor aguarde.");
        
        ProgressIndicator progressIndicator = new ProgressIndicator();
        VBox vbox = new VBox(10, new Label("Baixando nova versão..."), progressIndicator);
        progressAlert.getDialogPane().setContent(vbox);
        progressAlert.show();

        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Request request = new Request.Builder().url(downloadUrl).build();
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) throw new Exception("Falha no download");

                    // Baixa o EXE para a pasta temporária do Windows
                    File tempFile = new File(System.getProperty("java.io.tmpdir"), "PlayerJavaSetup_Update.exe");
                    try (InputStream is = response.body().byteStream();
                         FileOutputStream fos = new FileOutputStream(tempFile)) {
                        
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }

                    // Executa o instalador
                    Runtime.getRuntime().exec("cmd /c start \"\" \"" + tempFile.getAbsolutePath() + "\"");
                    
                    // Encerra a JVM para liberar os arquivos e permitir a instalação
                    Platform.runLater(() -> {
                        progressAlert.close();
                        System.exit(0);
                    });
                }
                return null;
            }
        };

        downloadTask.setOnFailed(e -> {
            progressAlert.close();
            Alert error = new Alert(Alert.AlertType.ERROR, "Falha ao baixar a atualização.");
            error.show();
        });

        Thread downloadThread = new Thread(downloadTask);
        downloadThread.setDaemon(true);
        downloadThread.start();
    }
}