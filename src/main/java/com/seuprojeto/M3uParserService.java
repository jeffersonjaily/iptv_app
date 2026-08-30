
package com.seuprojeto;

import java.io.BufferedReader;
import java.io.StringReader;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3uParserService {

    /*
     * ============================================================
     * REGEX / PADRÕES
     * ============================================================
     */

    private static final Pattern LOGO_PATTERN = Pattern.compile(
            "(?i)\\btvg-logo\\s*=\\s*\"([^\"]*)\""
    );

    private static final Pattern GROUP_PATTERN = Pattern.compile(
            "(?i)\\bgroup-title\\s*=\\s*\"([^\"]*)\""
    );

    private static final Pattern URL_RELATORIO_PATTERN = Pattern.compile(
            "(?i)\\bm3u[_-]?url\\s*[=:]?\\s*(https?://\\S+)"
    );

    /*
     * S01E01
     * S1E1
     * S01 E01
     * S01-E01
     * S01.E01
     * T01E01
     * T1 E1
     * Season 1 Episode 1
     * Temporada 1 Episódio 1
     */
    private static final Pattern TEMP_EP_PATTERN = Pattern.compile(
            "(?i)\\b(?:" +
                    "S|T" +
                    ")\\s*(\\d{1,2})" +
                    "\\s*[-_. ]*\\s*" +
                    "(?:E|EP|EPISODE|EPISODIO|EPISÓDIO)" +
                    "\\s*(\\d{1,3})\\b"
    );

    /*
     * Season 1 Episode 1
     * Temporada 1 Episódio 1
     */
    private static final Pattern SEASON_EP_PATTERN = Pattern.compile(
            "(?i)\\b(?:" +
                    "SEASON|TEMPORADA|TEMP" +
                    ")\\s*(\\d{1,2})" +
                    "\\s*(?:[-_. ]*\\s*)" +
                    "(?:E|EP|EPISODE|EPISODIO|EPISÓDIO)" +
                    "\\s*(\\d{1,3})\\b"
    );

    /*
     * 1x01
     * 01x01
     */
    private static final Pattern X_PATTERN = Pattern.compile(
            "(?i)\\b(\\d{1,2})\\s*[xX]\\s*(\\d{1,3})\\b"
    );

    /*
     * S01
     * T01
     * Season 1
     * Temporada 1
     * Temp 1
     */
    private static final Pattern TEMPORADA_PATTERN = Pattern.compile(
            "(?i)\\b(?:" +
                    "S|T|SEASON|TEMPORADA|TEMP" +
                    ")\\s*(\\d{1,2})\\b"
    );

    /*
     * EP01
     * EP 01
     * Episódio 01
     * Episode 01
     */
    private static final Pattern EPISODIO_PATTERN = Pattern.compile(
            "(?i)\\b(?:" +
                    "EP|EPS|EPISODE|EPISODIO|EPISÓDIO" +
                    ")\\s*[-_.:# ]*\\s*(\\d{1,3})\\b"
    );

    /*
     * Remove tags comuns:
     *
     * [1080p]
     * [FHD]
     * [PT-BR]
     * (WEB-DL)
     * (HD)
     */
    private static final Pattern TAG_PATTERN = Pattern.compile(
            "\\[[^\\]]*\\]|\\([^)]*\\)"
    );

    /*
     * ============================================================
     * URL DE RELATÓRIO
     * ============================================================
     */

    public static String extrairUrlDoRelatorio(String conteudo) {

        if (conteudo == null || conteudo.trim().isEmpty()) {
            return null;
        }

        Matcher matcher = URL_RELATORIO_PATTERN.matcher(conteudo);

        if (matcher.find()) {
            return limparUrl(matcher.group(1));
        }

        /*
         * Fallback para formatos antigos onde o texto pode estar
         * quebrado em várias linhas.
         */
        Pattern fallback = Pattern.compile(
                "(?i)\\bm3u[_-]?url\\b[\\s\\S]{0,300}?(https?://\\S+)"
        );

        matcher = fallback.matcher(conteudo);

        if (matcher.find()) {
            return limparUrl(matcher.group(1));
        }

        return null;
    }


    private static String limparUrl(String url) {

        if (url == null) {
            return null;
        }

        return url.trim()
                .replace("\"", "")
                .replace("'", "")
                .replace(",", "")
                .replace(";", "")
                .replace(")", "")
                .replace("]", "");
    }


    /*
     * ============================================================
     * NORMALIZAÇÃO
     * ============================================================
     */

    private static String normalizar(String texto) {

        if (texto == null) {
            return "";
        }

        String norm = Normalizer.normalize(
                texto,
                Normalizer.Form.NFD
        );

        return norm
                .replaceAll(
                        "\\p{M}",
                        ""
                )
                .toLowerCase(Locale.ROOT)
                .trim();
    }


    /*
     * ============================================================
     * PARSER M3U
     * ============================================================
     */

    public static List<MediaItem> parse(String m3uContent) {

        List<MediaItem> mediaItems = new ArrayList<>(20000);

        if (m3uContent == null || m3uContent.trim().isEmpty()) {
            return mediaItems;
        }

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new StringReader(m3uContent)
                        )
        ) {

            String line;

            MediaItem currentItem = null;

            while ((line = reader.readLine()) != null) {

                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                /*
                 * =================================================
                 * EXTINF
                 * =================================================
                 */

                if (line.regionMatches(
                        true,
                        0,
                        "#EXTINF:",
                        0,
                        8
                )) {

                    String logo = extrairAtributo(
                            LOGO_PATTERN,
                            line
                    );

                    String group = extrairAtributo(
                            GROUP_PATTERN,
                            line
                    );

                    /*
                     * Alguns provedores utilizam:
                     *
                     * #EXTGRP:
                     *
                     * em uma linha separada. Por isso o grupo
                     * inicial pode ficar vazio.
                     */
                    if (group == null || group.trim().isEmpty()) {
                        group = "Diversos";
                    }

                    String title = extrairTituloExtinf(
                            line
                    );

                    currentItem = new MediaItem(
                            title,
                            logo,
                            null,
                            group,
                            "CANAL"
                    );

                    continue;
                }


                /*
                 * =================================================
                 * EXTGRP
                 * =================================================
                 */

                if (line.regionMatches(
                        true,
                        0,
                        "#EXTGRP:",
                        0,
                        8
                )) {

                    if (currentItem != null) {

                        String group =
                                line.substring(8).trim();

                        if (!group.isEmpty()) {
                            currentItem.setGroup(group);
                        }
                    }

                    continue;
                }


                /*
                 * =================================================
                 * EXT-X-TVG / OUTRAS DIRETIVAS
                 * =================================================
                 */

                if (line.startsWith("#")) {
                    continue;
                }


                /*
                 * =================================================
                 * URL / STREAM
                 * =================================================
                 */

                if (currentItem != null) {

                    String url = line.trim();

                    if (!url.isEmpty()) {

                        /*
                         * Classificação ocorre antes de adicionar
                         * à lista.
                         */
                        String tipo = detectarTipo(
                                currentItem.getTitle(),
                                currentItem.getGroup(),
                                url
                        );

                        currentItem.setType(tipo);
                        currentItem.setUrl(url);

                        mediaItems.add(currentItem);
                    }

                    currentItem = null;
                }
            }

        } catch (Exception e) {

            /*
             * Não interrompe o programa por causa de uma entrada
             * malformada. O conteúdo válido que já foi processado
             * continua disponível.
             */

            System.err.println(
                    "Erro ao processar M3U: " +
                            e.getMessage()
            );
        }

        return mediaItems;
    }


    /*
     * ============================================================
     * EXTRAÇÃO DO TÍTULO
     * ============================================================
     */

    private static String extrairTituloExtinf(
            String linha
    ) {

        if (linha == null || linha.isEmpty()) {
            return "Desconhecido";
        }

        /*
         * O último vírgula normalmente separa metadados do título.
         *
         * Exemplo:
         *
         * #EXTINF:-1 tvg-name="Canal",Nome do Canal
         */
        int commaIndex = linha.lastIndexOf(',');

        if (commaIndex >= 0
                && commaIndex < linha.length() - 1) {

            String titulo =
                    linha.substring(
                            commaIndex + 1
                    ).trim();

            if (!titulo.isEmpty()) {
                return titulo;
            }
        }

        /*
         * Alguns arquivos usam apenas tvg-name.
         */
        Matcher tvgName = Pattern.compile(
                "(?i)\\btvg-name\\s*=\\s*\"([^\"]*)\""
        ).matcher(linha);

        if (tvgName.find()) {

            String titulo =
                    tvgName.group(1).trim();

            if (!titulo.isEmpty()) {
                return titulo;
            }
        }

        return "Desconhecido";
    }


    /*
     * ============================================================
     * ATRIBUTO
     * ============================================================
     */

    private static String extrairAtributo(
            Pattern pattern,
            String texto
    ) {

        if (pattern == null || texto == null) {
            return "";
        }

        Matcher matcher =
                pattern.matcher(texto);

        if (matcher.find()) {

            String valor =
                    matcher.group(1);

            return valor != null
                    ? valor.trim()
                    : "";
        }

        return "";
    }


    /*
     * ============================================================
     * DETECÇÃO DO TIPO
     * ============================================================
     */

    private static String detectarTipo(
            String titulo,
            String grupo,
            String url
    ) {

        String tituloNorm =
                normalizar(titulo);

        String grupoNorm =
                normalizar(grupo);

        String urlNorm =
                normalizar(url);


        /*
         * ========================================================
         * ANIME
         * ========================================================
         *
         * Deve ser testado antes de série quando o grupo indica
         * explicitamente anime.
         */

        if (ehAnime(grupoNorm, tituloNorm)) {
            return "ANIME";
        }


        /*
         * ========================================================
         * SÉRIE
         * ========================================================
         */

        if (ehSerie(grupoNorm, tituloNorm)) {
            return "SERIE";
        }


        /*
         * ========================================================
         * FILME
         * ========================================================
         */

        if (ehFilme(grupoNorm, tituloNorm)) {
            return "FILME";
        }


        /*
         * ========================================================
         * P2P
         * ========================================================
         *
         * O protocolo não determina sozinho se é filme/série/canal.
         * Portanto, somente usamos a URL como indicador auxiliar
         * quando a categoria não fornece informação suficiente.
         */

        if (urlNorm.startsWith("p2p://")
                || urlNorm.startsWith("acestream://")) {

            if (possuiIndicadorDeSerie(tituloNorm)) {
                return "SERIE";
            }
        }


        /*
         * ========================================================
         * FALLBACK
         * ========================================================
         */

        return "CANAL";
    }


    /*
     * ============================================================
     * ANIME
     * ============================================================
     */

    private static boolean ehAnime(
            String grupo,
            String titulo
    ) {

        return grupo.contains("anime")
                || grupo.contains("animes")
                || grupo.contains("animacao")
                || grupo.contains("desenho")
                || grupo.contains("cartoon")
                || titulo.contains("[anime]");
    }


    /*
     * ============================================================
     * SÉRIE
     * ============================================================
     */

    private static boolean ehSerie(
            String grupo,
            String titulo
    ) {

        if (grupo.contains("serie")
                || grupo.contains("series")
                || grupo.contains("seriados")
                || grupo.contains("temporada")
                || grupo.contains("tv show")
                || grupo.contains("tv shows")) {

            return true;
        }

        return possuiIndicadorDeSerie(titulo);
    }


    private static boolean possuiIndicadorDeSerie(
            String titulo
    ) {

        if (titulo == null || titulo.isEmpty()) {
            return false;
        }

        return TEMP_EP_PATTERN.matcher(titulo).find()
                || SEASON_EP_PATTERN.matcher(titulo).find()
                || X_PATTERN.matcher(titulo).find()
                || TEMPORADA_PATTERN.matcher(titulo).find()
                || EPISODIO_PATTERN.matcher(titulo).find();
    }


    /*
     * ============================================================
     * FILME
     * ============================================================
     */

    private static boolean ehFilme(
            String grupo,
            String titulo
    ) {

        if (grupo.contains("filme")
                || grupo.contains("filmes")
                || grupo.contains("movie")
                || grupo.contains("movies")
                || grupo.contains("cinema")
                || grupo.contains("vod")
                || grupo.contains("peliculas")
                || grupo.contains("longa metragem")) {

            return true;
        }

        /*
         * Não classificamos pelo título simplesmente porque
         * muitos provedores usam nomes como:
         *
         * "Movie Channel"
         *
         * e isso poderia gerar falsos positivos.
         */

        return false;
    }


    /*
     * ============================================================
     * AGRUPAMENTO DE SÉRIES
     * ============================================================
     *
     * Mantemos este método por compatibilidade com código existente.
     *
     * A lógica principal de agrupamento deve ficar no
     * OrganizadorDeSeries.
     */

    public static Map<String, SerieGroup> agruparSeries(
            List<MediaItem> todosOsItens,
            String aba
    ) {

        if (todosOsItens == null
                || todosOsItens.isEmpty()
                || aba == null) {

            return new LinkedHashMap<>();
        }

        /*
         * Delegamos para o organizador central.
         *
         * Isso evita manter duas implementações diferentes da
         * mesma regra.
         */
        return OrganizadorDeSeries.processar(
                todosOsItens,
                aba
        );
    }


    /*
     * ============================================================
     * MÉTODOS AUXILIARES PÚBLICOS
     * ============================================================
     */

    public static boolean ehSerieTitulo(
            String titulo
    ) {

        return possuiIndicadorDeSerie(
                normalizar(titulo)
        );
    }


    public static boolean ehUrlP2P(
            String url
    ) {

        if (url == null) {
            return false;
        }

        String valor =
                url.trim().toLowerCase(Locale.ROOT);

        return valor.startsWith("p2p://")
                || valor.startsWith("acestream://");
    }


    public static boolean ehUrlHTTP(
            String url
    ) {

        if (url == null) {
            return false;
        }

        String valor =
                url.trim().toLowerCase(Locale.ROOT);

        return valor.startsWith("http://")
                || valor.startsWith("https://");
    }
}

