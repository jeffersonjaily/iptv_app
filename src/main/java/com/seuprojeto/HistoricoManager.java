package com.seuprojeto;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class HistoricoManager {
    private static final String FILE_PATH = "historico_progresso.json";
    private static Map<String, Long> progressoMidia = new HashMap<>();
    private static final Gson gson = new Gson();

    static {
        carregarDoDisco();
    }

    public static void salvarProgresso(String url, long tempoSegundos) {
        if (url != null && !url.isEmpty()) {
            progressoMidia.put(url, tempoSegundos);
            salvarNoDisco();
        }
    }

    public static long carregarProgresso(String url) {
        return progressoMidia.getOrDefault(url, 0L);
    }

    private static void salvarNoDisco() {
        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            gson.toJson(progressoMidia, writer);
        } catch (Exception e) {
            System.err.println("Erro ao salvar histórico: " + e.getMessage());
        }
    }

    private static void carregarDoDisco() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<HashMap<String, Long>>(){}.getType();
                Map<String, Long> carregado = gson.fromJson(reader, type);
                if (carregado != null) {
                    progressoMidia = carregado;
                }
            } catch (Exception e) {
                System.err.println("Erro ao carregar histórico: " + e.getMessage());
            }
        }
    }
    
    public static Map<String, Long> getTodosOsProgressos() {
        return new HashMap<>(progressoMidia);
    }
}
