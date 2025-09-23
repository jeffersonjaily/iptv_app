import com.seuprojeto.playeriptv.PlayerActivity

// Trecho da MainActivity.kt
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    rvCanais = findViewById(R.id.rvCanais)
    rvCanais.layoutManager = LinearLayoutManager(this)

    // Simulação do carregamento da lista.
    // Substitua esta parte pela sua lógica de download.
    val canaisDummy = listOf(
        Canal("Canal de Exemplo 1", "Geral", "http://exemplo.com/canal1"),
        Canal("Canal de Exemplo 2", "Filmes", "http://exemplo.com/canal2")
    )

    // Instanciar o Adapter
    val adapter = CanalAdapter(canaisDummy) { canal ->
        // Ação ao clicar no canal
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra("url_do_canal", canal.url)
        startActivity(intent)
    }
    rvCanais.adapter = adapter

    // ... sua lógica de download real da lista vai aqui ...
}