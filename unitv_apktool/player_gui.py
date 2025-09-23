import tkinter as tk
from tkinter import ttk, messagebox, filedialog
import requests
import webbrowser
import threading
import re
import os
import subprocess

# Classe principal da nossa aplicação de IPTV - Versão 5.0
class IPTVPlayerApp:
    def __init__(self, root):
        self.root = root
        self.root.title("Player de Listas IPTV (v5.0 - com Categorias e Filtro)")
        self.root.geometry("800x600")

        self.listas_encontradas = {}
        self.todos_os_canais = []

        # --- Interface ---
        top_frame = tk.Frame(root)
        top_frame.pack(pady=10, padx=10, fill=tk.X)

        tk.Label(top_frame, text="Selecione uma Lista:", font=("Helvetica", 10)).pack(pady=(0, 5), anchor=tk.W)
        self.lista_combobox = ttk.Combobox(top_frame, state="readonly", width=80)
        self.lista_combobox.pack(side=tk.LEFT, expand=True, fill=tk.X, padx=(0, 5))
        
        button_frame = tk.Frame(root)
        button_frame.pack(pady=5, padx=10, fill=tk.X)
        self.load_file_button = tk.Button(button_frame, text="Abrir Arquivo de Listas (.txt)", command=self.abrir_e_processar_arquivo)
        self.load_file_button.pack(side=tk.LEFT, padx=0)
        self.load_url_button = tk.Button(button_frame, text="Carregar Canais da Lista", command=self.carregar_canais_da_lista, font=("Helvetica", 10, "bold"))
        self.load_url_button.pack(side=tk.LEFT, padx=10)
        
        # --- NOVO: Barra de Filtro ---
        filter_frame = tk.Frame(root)
        filter_frame.pack(pady=5, padx=10, fill=tk.X)
        tk.Label(filter_frame, text="Filtrar:", font=("Helvetica", 10)).pack(side=tk.LEFT)
        self.filter_entry = tk.Entry(filter_frame)
        self.filter_entry.pack(side=tk.LEFT, expand=True, fill=tk.X, padx=5)
        self.filter_entry.bind("<KeyRelease>", self.filtrar_lista) # Chama a função a cada tecla digitada

        # --- Tabela de Canais (agora como árvore) ---
        tree_frame = tk.Frame(root)
        tree_frame.pack(padx=10, pady=10, fill=tk.BOTH, expand=True)
        # Ajustamos a Treeview para não mostrar mais colunas, apenas a árvore
        self.tree = ttk.Treeview(tree_frame, columns=('Nome'), show='tree')
        self.tree.heading('#0', text='Canais por Categoria')
        
        scrollbar = ttk.Scrollbar(tree_frame, orient="vertical", command=self.tree.yview)
        self.tree.configure(yscrollcommand=scrollbar.set)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        self.tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        self.channel_links = {}
        self.tree.bind("<Double-1>", self.tocar_item_selecionado)

        self.status_label = tk.Label(root, text="Pronto. Clique em 'Abrir Arquivo de Listas' para começar.", bd=1, relief=tk.SUNKEN, anchor=tk.W)
        self.status_label.pack(fill=tk.X, side=tk.BOTTOM, ipady=2)

    def set_status(self, message):
        self.root.after(0, self.status_label.config, {'text': message})

    def abrir_e_processar_arquivo(self):
        # (Esta função permanece a mesma da versão anterior)
        filepath = filedialog.askopenfilename(title="Abrir arquivo de lista IPTV", filetypes=(("Arquivos de Texto", "*.txt"), ("Todos os arquivos", "*.*")))
        if not filepath: return
        self.set_status(f"Processando arquivo: {filepath}...")
        try:
            with open(filepath, 'r', encoding='utf-8') as f: content = f.read()
            self.listas_encontradas.clear()
            links_m3u = re.findall(r'http[s]?://[^\s]+(?:m3u_plus|m3u)', content)
            for i, link in enumerate(links_m3u):
                nome_lista = f"Link M3U #{i+1} ({link.split('/')[2]})"
                self.listas_encontradas[nome_lista] = link
            blocos = re.findall(r'(?:Servidor|𝙷𝚘𝚜𝚝|🌐)[\s➤:]+(?P<host>[\w.:-]+)[\s\S]*?(?:Usuário|𝚄𝚜𝚞𝚊𝚛𝚒𝚘|🚦)[\s➤:]+(?P<user>[\w-]+)[\s\S]*?(?:Senha|𝚂𝚎𝚗𝚑𝚊|🚥)[\s➤:]+(?P<pass>[\w-]+)', content)
            for i, bloco in enumerate(blocos):
                host, user, password = bloco
                link_construido = f"http://{host}/get.php?username={user}&password={password}&type=m3u_plus"
                nome_lista = f"Servidor: {host} | Usuário: {user}"
                self.listas_encontradas[nome_lista] = link_construido
            if not self.listas_encontradas:
                messagebox.showwarning("Nenhuma Lista", "Nenhuma lista de IPTV válida foi encontrada no arquivo.")
                return
            self.lista_combobox['values'] = list(self.listas_encontradas.keys())
            self.lista_combobox.current(0)
            self.set_status(f"{len(self.listas_encontradas)} listas encontradas! Selecione uma e carregue.")
            messagebox.showinfo("Sucesso", f"{len(self.listas_encontradas)} listas foram carregadas no menu.")
        except Exception as e:
            messagebox.showerror("Erro ao Processar Arquivo", f"Erro: {e}")

    def parse_m3u_content(self, m3u_content):
        # (Esta função permanece a mesma)
        canais = []
        linhas = m3u_content.strip().split('\n')
        for i, linha in enumerate(linhas):
            if linha.strip().startswith('#EXTINF:'):
                try:
                    info_linha, url_linha = linha, linhas[i+1].strip()
                    if url_linha and not url_linha.startswith('#'):
                        nome = info_linha.split(',')[-1].strip()
                        grupo_match = re.search(r'group-title="(.*?)"', info_linha)
                        grupo = grupo_match.group(1) if grupo_match else "Geral"
                        canais.append({'title': nome, 'group': grupo, 'url': url_linha})
                except (IndexError, AttributeError): continue
        return canais

    def popular_treeview(self, canais):
        """NOVA FUNÇÃO para popular a Treeview com categorias."""
        for i in self.tree.get_children(): self.tree.delete(i)
        self.channel_links.clear()

        category_nodes = {}
        for i, canal in enumerate(canais):
            grupo = canal['group']
            nome = canal['title']
            url = canal['url']
            
            # Se a categoria ainda não foi adicionada à árvore, adicione-a
            if grupo not in category_nodes:
                # Insere a categoria como um item pai, e a deixa fechada (open=False)
                parent_id = self.tree.insert("", tk.END, text=grupo, open=False)
                category_nodes[grupo] = parent_id
            
            # Insere o canal como um "filho" da sua categoria correspondente
            item_id = self.tree.insert(category_nodes[grupo], tk.END, text=nome)
            self.channel_links[item_id] = url

    def carregar_canais_da_lista(self):
        nome_selecionado = self.lista_combobox.get()
        if not nome_selecionado:
            messagebox.showwarning("Nenhuma Lista", "Selecione uma lista primeiro.")
            return
        
        m3u_url = self.listas_encontradas[nome_selecionado]
        self.set_status(f"Baixando lista... Aguarde.")
        self.load_url_button.config(state=tk.DISABLED)
        
        def task():
            try:
                headers = {'User-Agent': 'Mozilla/5.0'}
                response = requests.get(m3u_url, headers=headers, timeout=30)
                response.raise_for_status()
                
                self.set_status("Analisando o conteúdo da lista...")
                self.todos_os_canais = self.parse_m3u_content(response.text) # Guarda todos os canais

                if not self.todos_os_canais:
                    self.set_status("Nenhum canal encontrado. A lista pode estar vazia ou expirada.")
                    return
                
                self.popular_treeview(self.todos_os_canais) # Popula a lista com todos os canais
                self.set_status(f"{len(self.todos_os_canais)} canais carregados! Use o filtro ou dê duplo-clique.")
            except requests.exceptions.RequestException as e:
                self.set_status("Falha ao carregar a lista.")
                messagebox.showerror("Erro de Conexão", f"Não foi possível baixar a lista.\n\nErro: {e}")
            finally:
                self.root.after(0, self.load_url_button.config, {'state': tk.NORMAL})
        
        threading.Thread(target=task, daemon=True).start()

    def filtrar_lista(self, event=None):
        """NOVA FUNÇÃO para filtrar os canais com base no texto digitado."""
        filtro = self.filter_entry.get().lower()
        if not filtro:
            # Se o filtro estiver vazio, mostra todos os canais
            self.popular_treeview(self.todos_os_canais)
            return

        canais_filtrados = []
        for canal in self.todos_os_canais:
            # Procura o texto do filtro no nome do canal OU no nome do grupo
            if filtro in canal['title'].lower() or filtro in canal['group'].lower():
                canais_filtrados.append(canal)
        
        self.popular_treeview(canais_filtrados)
        # Abre todas as categorias para mostrar os resultados do filtro
        for node in self.tree.get_children():
            self.tree.item(node, open=True)

    def tocar_item_selecionado(self, event):
        # (Esta função permanece a mesma da v4.0, com a lógica do VLC)
        item_id = self.tree.focus()
        if not item_id: return
        video_url = self.channel_links.get(item_id)
        if not video_url: return # Não faz nada se clicar em uma categoria

        self.set_status(f"Procurando VLC e abrindo stream...")
        vlc_paths = [r"C:\Program Files\VideoLAN\VLC\vlc.exe", r"C:\Program Files (x86)\VideoLAN\VLC\vlc.exe"]
        vlc_executable = next((path for path in vlc_paths if os.path.exists(path)), None)

        if vlc_executable:
            try:
                subprocess.Popen([vlc_executable, video_url])
            except Exception as e:
                messagebox.showerror("Erro ao Abrir VLC", f"Erro: {e}")
        else:
            webbrowser.open(video_url)
            messagebox.showinfo("VLC não encontrado", "Instale o VLC Media Player para a melhor experiência.")

if __name__ == "__main__":
    root = tk.Tk()
    app = IPTVPlayerApp(root)
    root.mainloop()