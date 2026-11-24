# Booki!

Aplicativo Android para gerenciar uma biblioteca pessoal. O usuário cria uma conta, registra os livros que já leu, está lendo ou quer ler, adiciona capas, dá notas, edita e exclui obras, pesquisa preços para montar sua lista de desejos e recebe recomendações com base no seu histórico de leitura.

Projeto final da disciplina **Desenvolvimento Mobile II** (UPF, 2026).

## Integrantes

| Nome | Matrícula |
| --- | --- |
| Millena Rosa | 164651 |
| Matheus Versa | 204090 |

## Funcionalidades

- **Login local** com cadastro de usuário e sessão que lembra quem está logado.
- **Minha estante**: lista de livros com destaque para leitura atual (*Lendo agora*) e filtro por status (*Lidos*, *Lendo*, *Quero ler*).
- **CRUD de livros**: adicionar, ver detalhes, editar e excluir (com confirmação), com suporte a capa automática via API ou foto manual (galeria/câmera).
- **Regras de negócio no formulário**:
  - título e autor obrigatórios;
  - nota (0,5 a 5 estrelas) e data de conclusão só para livros com status **Lido**;
  - datas de início e conclusão não podem estar no futuro, e a conclusão não pode ser antes do início;
  - livros em **Quero ler** não têm datas de leitura.
- **Preenchimento pelo Buscar**: o campo de busca por palavras-chave preenche título, autor, páginas e capa automaticamente via Google Books API (em português, com fallback da Open Library).
- **Lista de desejos e consulta de preços**: pesquisa de livros e valores na Google Books API (com link para a loja) e lista local para salvar os livros de interesse.
- **Compartilhar** um livro com outros apps (Intent implícita `ACTION_SEND`).
- **Para você**: recomendações com base nos livros mais bem avaliados.
- **Perfil** com os dados do usuário e o botão Sair.

## Telas

As capturas de tela do aplicativo estão em [docs/telas.md](docs/telas.md).

## Tecnologias utilizadas

- **Kotlin** e **Android Studio**, com layouts **XML** (Empty Views Activity, sem Compose)
- **Android Gradle Plugin 9.5** (Kotlin embutido), Gradle 9.8, compileSdk/targetSdk 36, minSdk 26
- **Material Components 3** (Toolbar, BottomNavigationView, TextInputLayout, Chips, Cards)
- **Activities + Fragments**: 5 Activities (Login, Cadastro, Principal, Formulário e Detalhe do livro) e 4 Fragments (Estante, Desejos, Para você e Perfil)
- **Room 2.8** com KSP para a persistência local (tabelas `usuarios`, `livros` e `desejos`)
- **Retrofit 2 + Gson** para as APIs REST:
  - [Google Books API](https://developers.google.com/books) para busca por palavras-chave, capas e consulta de preços
  - [Open Library Search API](https://openlibrary.org/dev/docs/api/search) como fallback de busca e para as recomendações
- **Coroutines** (`lifecycleScope`) para banco, rede e carregamento de imagens fora da thread principal
- **JUnit 4** para os testes unitários das regras de negócio

## Como executar

### Pré-requisitos

- Android Studio (versão com suporte ao AGP 9.5) ou JDK 17 + Android SDK 36
- Emulador ou aparelho com Android 8.0 (API 26) ou superior

### Passo a passo

1. Clone o repositório:
   ```bash
   git clone https://github.com/MatheusSVZ/Booki.git
   ```
2. Abra a pasta `Booki` no Android Studio e aguarde o Gradle sincronizar.
   > Evite pastas com `!` no caminho (ex.: `BOOKI!`): o Gradle/Java não funciona direito nelas.
3. *(Opcional, para consulta de preços no Google Books)* Crie uma chave grátis da Google Books API no
   [Google Cloud Console](https://console.cloud.google.com/apis/library/books.googleapis.com)
   e adicione ao arquivo `local.properties` na raiz do projeto (ele não vai para o Git):
   ```properties
   GOOGLE_BOOKS_API_KEY=sua_chave_aqui
   ```
   Sem a chave, o app continua funcionando normalmente usando o fallback automático da Open Library e o link para consulta de preço na loja.
4. Rode o app no emulador ou celular (botão **Run**) ou pela linha de comando:
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```
5. Na primeira abertura, toque em **Cadastre-se**, crie a conta e entre.

### Testes

```bash
./gradlew testDebugUnitTest
```

Os testes ficam em `app/src/test` e cobrem as validações de usuário, de livro e a formatação de preço.
