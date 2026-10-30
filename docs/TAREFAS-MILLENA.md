# Tarefas da Millena

Estas tarefas ficam com a **Millena Rosa** (GitHub: `millenarrosa`). Os commits e Pull Requests delas devem sair **da conta dela**, no computador dela, para que o histórico mostre a colaboração real da dupla.

## Antes de começar (uma vez só)

```bash
git clone https://github.com/MatheusSVZ/Booki.git
cd Booki
git config user.name "Millena Rosa"
git config user.email "<e-mail da conta GitHub da Millena>"
git checkout develop
git pull origin develop
```

Abra a pasta no Android Studio. Para cada tarefa: crie a branch a partir de `develop`, faça commits pequenos, rode `./gradlew testDebugUnitTest assembleDebug` e abra um Pull Request para `develop`.

---

## Tarefa 1: Recomendações (`feature/recomendacoes`)

**Objetivo:** a aba **Recomendações** deve sugerir livros com base nos livros que o usuário mais gostou.
Hoje `RecomendacoesFragment` só mostra o aviso "Em breve".

O que já existe e pode ser usado:

| Recurso | Onde |
| --- | --- |
| `livroDao().melhoresAvaliados(usuarioId, limite)` | livros **Lidos** com as maiores notas |
| `livroDao().listar(usuarioId)` | todos os livros da estante (para não recomendar o que já está lá) |
| `RetrofitClient.openLibrary.buscar(autor = ..., sort = "rating", limite = 10)` | busca por autor (sem chave) |
| `RetrofitClient.openLibrary.buscar(assunto = ..., sort = "rating")` | busca por assunto/categoria |
| `Conexao.temInternet(context)` e `Util.dialogoAlerta(...)` | aviso de falta de internet |
| `LivroFormActivity` com `putExtra(EXTRA_TITULO / EXTRA_AUTOR / EXTRA_ISBN / EXTRA_CATEGORIA / EXTRA_PAGINAS)` | abre o formulário já preenchido |

Passo a passo sugerido (um commit por passo):

1. `git checkout -b feature/recomendacoes develop`
2. Crie `fragment_recomendacoes.xml` com um `RecyclerView`, um `LinearProgressIndicator` e um `TextView` para mensagens (lista vazia, sem internet).
3. Crie `item_recomendacao.xml` e um `RecomendacaoAdapter` (pode se inspirar no `LivroAdapter`) mostrando título, autor e ano.
4. No `RecomendacoesFragment`, no `onViewCreated`:
   - pegue os 3 a 5 livros de `melhoresAvaliados`; se a lista estiver vazia, mostre "Avalie livros lidos para receber recomendações";
   - junte os autores (e, se quiser, as categorias) desses livros;
   - verifique a internet com `Conexao.temInternet`; sem internet, mostre a mensagem e **não chame a API**;
   - para cada autor, chame a Open Library dentro de `viewLifecycleOwner.lifecycleScope.launch { ... }`, envolvendo em `try/catch (e: IOException)` e `catch (e: HttpException)`, para não travar;
   - remova da lista os livros cujo título já está na estante (compare em minúsculas) e os repetidos.
5. Ao tocar em uma recomendação, abra o `LivroFormActivity` com os extras preenchidos, para salvar como **Quero ler/comprar**.
6. Teste: cadastre 2 livros Lidos com nota (ex.: *O Hobbit*, de J.R.R. Tolkien) e abra a aba. Depois, teste em modo avião.
7. Abra o PR `feature/recomendacoes` → `develop` descrevendo como testar.

---

## Tarefa 2: Estatísticas no Perfil (`feature/perfil-estatisticas`)

**Objetivo:** mostrar no Perfil quantos livros o usuário tem em cada status e a nota média dos lidos.
O `fragment_perfil.xml` já tem um `LinearLayout` vazio com id `painelEstatisticas` para isso.

1. `git checkout -b feature/perfil-estatisticas develop`
2. No `LivroDao`, adicione:
   ```kotlin
   @Query("SELECT COUNT(*) FROM livros WHERE usuarioId = :usuarioId AND status = :status")
   suspend fun contarPorStatus(usuarioId: Int, status: StatusLeitura): Int

   @Query("SELECT AVG(nota) FROM livros WHERE usuarioId = :usuarioId AND nota IS NOT NULL")
   suspend fun mediaDasNotas(usuarioId: Int): Float?   // null quando não há notas
   ```
3. No layout, coloque três cartões (Lidos, Lendo, Quero ler) e uma linha com a nota média.
4. No `PerfilFragment`, carregue os números no `onResume` (para atualizar quando voltar da estante).
5. Teste com uma conta nova (tudo zero, média "sem notas") e depois com livros cadastrados.
6. PR `feature/perfil-estatisticas` → `develop`.

---

## Tarefa 3: Revisão, integração e prints (`docs/prints-recomendacoes`)

1. **Revise os Pull Requests do Matheus** no GitHub (aba *Files changed*), deixe comentários ou aprove, e faça o merge **nesta ordem**: #2 login, #3 navegação, #4 CRUD, #5 API, #6 documentação. Depois, os seus.
2. Depois que tudo estiver em `develop` e testado no emulador, abra o PR `develop` → `main` (versão estável para entrega).
3. Tire os prints das telas que você criou (Recomendações e Perfil com estatísticas) no emulador e adicione em `docs/prints/` e em `docs/telas.md`, na branch `docs/prints-recomendacoes`.
