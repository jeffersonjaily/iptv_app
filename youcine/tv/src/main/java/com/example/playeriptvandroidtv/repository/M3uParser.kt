// src/main/java/com/seuprojeto/playeriptv/repository/M3uParser.kt
package com.seuprojeto.playeriptv.repository

import com.seuprojeto.playeriptv.data.Canal
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.regex.Pattern

class M3uParser {

    private val client = OkHttpClient()

    // Este método é baseado na lógica do seu ChannelListView.java
    fun encontrarListasNoArquivo(file: File): Map<String, String> {
        val listasEncontradas = mutableMapOf<String, String>()
        val content = file.readText()

        // Padrão para links diretos M3U
        val linkPattern = Pattern.compile("http[s]?://[^\\s]+(?:m3u_plus|m3u)")
        val linkMatcher = linkPattern.matcher(content)
        var linkCount = 0
        while (linkMatcher.find()) {
            val link = linkMatcher.group()
            val nome = "Link M3U #${++linkCount} (${link.split("/")[2]})"
            listasEncontradas[nome] = link
        }

        // Padrão para credenciais (servidor, usuário, senha)
        val blockPattern = Pattern.compile(
            "(?:Servidor|\\p{So}\\p{L}\\p{M}*|\\p{So})[\\s➤:]+(?<host>[\\w.:-]+)[\\s\\S]*?(?:Usuário|\\p{So}\\p{L}\\p{M}*|\\p{So})[\\s➤:]+(?<user>[\\w-]+)[\\s\\S]*?(?:Senha|\\p{So}\\p{L}\\p{M}*|\\p{So})[\\s➤:]+(?<pass>[\\w-]+)"
        )
        val blockMatcher = blockPattern.matcher(content)
        while (blockMatcher.find()) {
            val host = blockMatcher.group("host")
            val user = blockMatcher.group("user")
            val password = blockMatcher.group("pass")
            val link = "http://$host/get.php?username=$user&password=$password&type=m3u_plus"
            val nome = "Servidor: $host | Usuário: $user"
            listasEncontradas[nome] = link
        }

        return listasEncontradas
    }

    // Este método fará a requisição para a URL e processará o conteúdo M3U
    fun parseM3uContent(content: String): List<Canal> {
        val canais = mutableListOf<Canal>()
        val lines = content.split("\n")

        for (i in lines.indices) {
            if (lines[i].trim().startsWith("#EXTINF:")) {
                try {
                    val infoLine = lines[i]
                    val urlLine = lines[i + 1].trim()

                    if (urlLine.isNotEmpty() && !urlLine.startsWith("#")) {
                        val name = infoLine.substring(infoLine.lastIndexOf(",") + 1).trim()
                        val groupMatcher = Pattern.compile("group-title=\"(.*?)\"").matcher(infoLine)
                        val group = if (groupMatcher.find()) groupMatcher.group(1) else "Geral"

                        canais.add(Canal(name, group!!, urlLine))
                    }
                } catch (ignored: Exception) {}
            }
        }
        return canais
    }

    // Função para baixar a lista de forma assíncrona
    suspend fun baixarLista(url: String): String {
        val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Falha ao baixar lista: ${response.code}")
        }
        return response.body?.string() ?: throw Exception("Resposta vazia")
    }
}