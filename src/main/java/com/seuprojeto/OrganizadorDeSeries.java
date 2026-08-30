package com.seuprojeto;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrganizadorDeSeries {

    /*
     * ============================================================
     * PADRÕES
     * ============================================================
     */

    private static final Pattern PADRAO_TEMP_EP = Pattern.compile(
            "(?i)" +
            "(?:\\bS|\\bT|\\bSEASON\\s*|\\bTEMPORADA\\s*|\\bTEMP\\s*)" +
            "(\\d{1,2})" +
            "\\s*" +
            "(?:[-_.: ]*\\bE(?:P(?:ISODE|ISÓDIO)?)?\\s*|[-_.: ]*EP\\s*|\\s*)" +
            "(\\d{1,3})"
    );

    private static final Pattern PADRAO_1X01 = Pattern.compile(
            "(?i)\\b(\\d{1,2})\\s*[xX]\\s*(\\d{1,3})\\b"
    );

    private static final Pattern PADRAO_TEMPORADA = Pattern.compile(
            "(?i)\\b(?:S|T|SEASON|TEMPORADA|TEMP)\\s*(\\d{1,2})\\b"
    );

    private static final Pattern PADRAO_EPISODIO = Pattern.compile(
            "(?i)\\b(?:EP|EPS|EPISODIO|EPISÓDIO|EPISODE)\\s*[-_.:# ]*\\s*(\\d{1,3})\\b"
    );

    private static final Pattern PADRAO_TAGS = Pattern.compile(
            "\\[[^\\]]*\\]|\\([^)]*\\)"
    );

    /*
     * ============================================================
     * PROCESSAMENTO
     * ============================================================
     */

    public static Map<String, SerieGroup> processar(
            List<MediaItem> todosOsItens,
            String tipoAlvo
    ) {

        Map<String, SerieGroup> mapaAgrupado =
                new LinkedHashMap<>();

        if (todosOsItens == null || todosOsItens.isEmpty()) {
            return mapaAgrupado;
        }

        if (tipoAlvo == null || tipoAlvo.trim().isEmpty()) {
            return mapaAgrupado;
        }

        String serieAtivaNaMemoria = null;
        String categoriaAtivaNaMemoria = null;

        for (MediaItem item : todosOsItens) {

            if (item == null) {
                continue;
            }

            /*
             * ====================================================
             * TIPO
             * ====================================================
             */

            String tipoItem = item.getType();

            if (tipoItem == null) {
                continue;
            }

            if (!tipoItem.equalsIgnoreCase(tipoAlvo)) {
                continue;
            }

            /*
             * ====================================================
             * TÍTULO
             * ====================================================
             */

            String tituloCru = item.getTitle();

            if (tituloCru == null ||
                    tituloCru.trim().isEmpty()) {

                continue;
            }

            tituloCru = tituloCru.trim();

            /*
             * ====================================================
             * CATEGORIA
             * ====================================================
             */

            String categoria = item.getGroup();

            if (categoria == null ||
                    categoria.trim().isEmpty()) {

                categoria = "Diversos";

            } else {

                categoria = categoria.trim();
            }

            /*
             * ====================================================
             * CONTEÚDO 24 HORAS
             * ====================================================
             */

            if (ehConteudo24Horas(categoria)) {

                String nome24h =
                        limparNome(tituloCru);

                String chave24h =
                        chaveGrupo(nome24h);

                SerieGroup grupo24h =
                        mapaAgrupado.get(chave24h);

                if (grupo24h == null) {

                    grupo24h = new SerieGroup(
                            nome24h,
                            item.getLogo(),
                            tipoAlvo
                    );

                    mapaAgrupado.put(
                            chave24h,
                            grupo24h
                    );
                }

                grupo24h.addEpisode(
                        "Transmissão Contínua",
                        item
                );

                continue;
            }

            /*
             * ====================================================
             * LIMPEZA
             * ====================================================
             */

            String tituloLimpo =
                    limparTituloParaAnalise(tituloCru);

            /*
             * ====================================================
             * ANÁLISE
             * ====================================================
             */

            InformacaoEpisodio info =
                    analisarTitulo(tituloCru);

            String nomeBase;

            if (info.temEstrutura()) {

                nomeBase =
                        extrairNomeDaSerie(
                                tituloLimpo,
                                info
                        );

            } else {

                nomeBase =
                        extrairNomePrincipal(
                                tituloLimpo
                        );
            }

            nomeBase =
                    limparNome(nomeBase);

            /*
             * ====================================================
             * ÓRFÃOS
             * ====================================================
             */

            boolean ehOrfao =
                    nomeBase.equalsIgnoreCase(
                            limparNome(tituloLimpo)
                    );

            if (ehOrfao
                    && serieAtivaNaMemoria != null
                    && categoriaAtivaNaMemoria != null
                    && categoria.equalsIgnoreCase(
                            categoriaAtivaNaMemoria)
                    && info.temEstrutura()) {

                nomeBase =
                        serieAtivaNaMemoria;
            }

            /*
             * ====================================================
             * MEMÓRIA
             * ====================================================
             */

            if (!ehNomeDesconhecido(nomeBase)) {

                serieAtivaNaMemoria =
                        nomeBase;

                categoriaAtivaNaMemoria =
                        categoria;
            }

            /*
             * ====================================================
             * TEMPORADA
             * ====================================================
             */

            String temporada =
                    info.getTemporada();

            if (temporada == null ||
                    temporada.trim().isEmpty()) {

                temporada =
                        "Episódios Soltos / Extras";
            }

            /*
             * ====================================================
             * GRUPO
             *
             * NÃO usamos computeIfAbsent aqui.
             *
             * Isso elimina o problema de variável capturada
             * dentro de lambda.
             * ====================================================
             */

            final String chave =
                    chaveGrupo(nomeBase);

            SerieGroup grupo =
                    mapaAgrupado.get(chave);

            if (grupo == null) {

                grupo = new SerieGroup(
                        nomeBase,
                        item.getLogo(),
                        tipoAlvo
                );

                mapaAgrupado.put(
                        chave,
                        grupo
                );
            }

            /*
             * ====================================================
             * EPISÓDIO
             * ====================================================
             */

            grupo.addEpisode(
                    temporada,
                    item
            );
        }

        return ordenarGrupos(mapaAgrupado);
    }

    /*
     * ============================================================
     * ANÁLISE DO TÍTULO
     * ============================================================
     */

    private static InformacaoEpisodio analisarTitulo(
            String titulo) {

        if (titulo == null ||
                titulo.trim().isEmpty()) {

            return new InformacaoEpisodio();
        }

        Matcher matcher =
                PADRAO_TEMP_EP.matcher(titulo);

        if (matcher.find()) {

            try {

                int temporada =
                        Integer.parseInt(
                                matcher.group(1)
                        );

                int episodio =
                        Integer.parseInt(
                                matcher.group(2)
                        );

                return new InformacaoEpisodio(
                        temporada,
                        episodio,
                        matcher.start(),
                        matcher.end()
                );

            } catch (NumberFormatException ignored) {
            }
        }

        matcher =
                PADRAO_1X01.matcher(titulo);

        if (matcher.find()) {

            try {

                int temporada =
                        Integer.parseInt(
                                matcher.group(1)
                        );

                int episodio =
                        Integer.parseInt(
                                matcher.group(2)
                        );

                return new InformacaoEpisodio(
                        temporada,
                        episodio,
                        matcher.start(),
                        matcher.end()
                );

            } catch (NumberFormatException ignored) {
            }
        }

        matcher =
                PADRAO_TEMPORADA.matcher(titulo);

        if (matcher.find()) {

            try {

                int temporada =
                        Integer.parseInt(
                                matcher.group(1)
                        );

                return new InformacaoEpisodio(
                        temporada,
                        -1,
                        matcher.start(),
                        matcher.end()
                );

            } catch (NumberFormatException ignored) {
            }
        }

        matcher =
                PADRAO_EPISODIO.matcher(titulo);

        if (matcher.find()) {

            try {

                int episodio =
                        Integer.parseInt(
                                matcher.group(1)
                        );

                return new InformacaoEpisodio(
                        -1,
                        episodio,
                        matcher.start(),
                        matcher.end()
                );

            } catch (NumberFormatException ignored) {
            }
        }

        return new InformacaoEpisodio();
    }

    /*
     * ============================================================
     * NOME DA SÉRIE
     * ============================================================
     */

    private static String extrairNomeDaSerie(
            String titulo,
            InformacaoEpisodio info) {

        if (titulo == null ||
                titulo.trim().isEmpty()) {

            return "Série Desconhecida";
        }

        int inicio =
                info.getInicio();

        if (inicio >= 0 &&
                inicio <= titulo.length()) {

            String antes =
                    titulo.substring(0, inicio);

            antes =
                    limparSufixos(antes);

            if (!antes.isEmpty()) {
                return antes;
            }
        }

        return extrairNomePrincipal(titulo);
    }

    private static String extrairNomePrincipal(
            String tituloLimpo) {

        if (tituloLimpo == null ||
                tituloLimpo.trim().isEmpty()) {

            return "Série Desconhecida";
        }

        Matcher matcher =
                PADRAO_TEMP_EP.matcher(
                        tituloLimpo
                );

        if (matcher.find()) {

            return limparNome(
                    tituloLimpo.substring(
                            0,
                            matcher.start()
                    )
            );
        }

        matcher =
                PADRAO_1X01.matcher(
                        tituloLimpo
                );

        if (matcher.find()) {

            return limparNome(
                    tituloLimpo.substring(
                            0,
                            matcher.start()
                    )
            );
        }

        matcher =
                PADRAO_TEMPORADA.matcher(
                        tituloLimpo
                );

        if (matcher.find()) {

            return limparNome(
                    tituloLimpo.substring(
                            0,
                            matcher.start()
                    )
            );
        }

        matcher =
                PADRAO_EPISODIO.matcher(
                        tituloLimpo
                );

        if (matcher.find()) {

            return limparNome(
                    tituloLimpo.substring(
                            0,
                            matcher.start()
                    )
            );
        }

        return limparNome(tituloLimpo);
    }

    /*
     * ============================================================
     * TEMPORADA
     * ============================================================
     */

    private static String extrairTemporada(
            String titulo) {

        InformacaoEpisodio info =
                analisarTitulo(titulo);

        return info.getTemporada();
    }

    /*
     * ============================================================
     * LIMPEZA
     * ============================================================
     */

    private static String limparTituloParaAnalise(
            String titulo) {

        if (titulo == null) {
            return "";
        }

        String resultado =
                titulo.trim();

        resultado =
                PADRAO_TAGS.matcher(
                        resultado
                ).replaceAll(" ");

        resultado =
                resultado.replaceAll(
                        "\\s+",
                        " "
                ).trim();

        return resultado;
    }

    private static String limparNome(
            String nome) {

        if (nome == null) {
            return "Série Desconhecida";
        }

        String temp =
                nome.trim();

        temp =
                temp.replaceAll(
                        "^[\\s\\-_\\|\\.:]+",
                        ""
                );

        temp =
                limparSufixos(temp);

        temp =
                temp.replaceAll(
                        "\\s+",
                        " "
                ).trim();

        if (temp.isEmpty()) {
            return "Série Desconhecida";
        }

        return temp;
    }

    private static String limparSufixos(
            String nome) {

        if (nome == null) {
            return "Série Desconhecida";
        }

        String temp =
                nome.trim();

        boolean alterou = true;

        while (alterou &&
                !temp.isEmpty()) {

            String anterior =
                    temp;

            temp =
                    temp.replaceAll(
                            "[\\s\\-_\\|\\.:]+$",
                            ""
                    ).trim();

            alterou =
                    !temp.equals(anterior);
        }

        return temp.isEmpty()
                ? "Série Desconhecida"
                : temp;
    }

    /*
     * ============================================================
     * 24 HORAS
     * ============================================================
     */

    private static boolean ehConteudo24Horas(
            String categoria) {

        if (categoria == null) {
            return false;
        }

        String normalizada =
                categoria
                        .toUpperCase(Locale.ROOT)
                        .replaceAll(
                                "\\s+",
                                " "
                        )
                        .trim();

        return normalizada.contains("24 HORAS")
                || normalizada.contains("24H")
                || normalizada.contains("24 H");
    }

    /*
     * ============================================================
     * NOME DESCONHECIDO
     * ============================================================
     */

    private static boolean ehNomeDesconhecido(
            String nome) {

        if (nome == null ||
                nome.trim().isEmpty()) {

            return true;
        }

        return nome.equalsIgnoreCase(
                "Série Desconhecida"
        );
    }

    /*
     * ============================================================
     * CHAVE DO GRUPO
     * ============================================================
     */

    private static String chaveGrupo(
            String nome) {

        if (nome == null) {
            return "serie_desconhecida";
        }

        return nome
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll(
                        "\\s+",
                        " "
                );
    }

    /*
     * ============================================================
     * ORDENAÇÃO
     * ============================================================
     */

    private static Map<String, SerieGroup> ordenarGrupos(
            Map<String, SerieGroup> mapa) {

        return new LinkedHashMap<>(mapa);
    }

    /*
     * ============================================================
     * INFORMAÇÃO DO EPISÓDIO
     * ============================================================
     */

    private static class InformacaoEpisodio {

        private final int temporada;
        private final int episodio;
        private final int inicio;
        private final int fim;

        InformacaoEpisodio() {

            this(
                    -1,
                    -1,
                    -1,
                    -1
            );
        }

        InformacaoEpisodio(
                int temporada,
                int episodio,
                int inicio,
                int fim) {

            this.temporada =
                    temporada;

            this.episodio =
                    episodio;

            this.inicio =
                    inicio;

            this.fim =
                    fim;
        }

        boolean temEstrutura() {

            return temporada > 0
                    || episodio > 0;
        }

        String getTemporada() {

            if (temporada > 0) {

                return "Temporada "
                        + temporada;
            }

            return null;
        }

        int getTemporadaNumero() {
            return temporada;
        }

        int getEpisodioNumero() {
            return episodio;
        }

        int getInicio() {
            return inicio;
        }

        int getFim() {
            return fim;
        }
    }
}