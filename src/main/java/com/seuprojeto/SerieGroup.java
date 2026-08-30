package com.seuprojeto;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SerieGroup {
    private String title;
    private String logo;
    private String type;
    
    // Comparador customizado para não colocar "Temporada 10" antes de "Temporada 2"
    private Map<String, List<MediaItem>> seasons = new TreeMap<>((s1, s2) -> {
        Integer n1 = extrairNumero(s1);
        Integer n2 = extrairNumero(s2);
        if (n1 != null && n2 != null) {
            return n1.compareTo(n2);
        }
        return s1.compareTo(s2);
    });

    public SerieGroup(String title, String logo, String type) {
        this.title = title;
        this.logo = logo;
        this.type = type;
    }

    public void addEpisode(String seasonName, MediaItem episode) {
        seasons.computeIfAbsent(seasonName, k -> new ArrayList<>()).add(episode);
    }

    private Integer extrairNumero(String texto) {
        Matcher matcher = Pattern.compile("\\d+").matcher(texto);
        return matcher.find() ? Integer.parseInt(matcher.group()) : null;
    }

    public String getTitle() { return title; }
    public String getLogo() { return logo; }
    public String getType() { return type; }
    public Map<String, List<MediaItem>> getSeasons() { return seasons; }
}