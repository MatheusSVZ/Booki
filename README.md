# Booki!

Aplicativo Android para gerenciar uma biblioteca pessoal. O usuário cria uma conta, registra os livros que já leu, está lendo ou quer ler/comprar, dá notas, edita e exclui obras, consulta o preço dos livros de interesse e recebe recomendações com base no seu histórico de leitura.

Projeto final da disciplina **Desenvolvimento Mobile II** (UPF, 2026).

## Integrantes

| Nome | Matrícula |
| --- | --- |
| Millena Rosa | 164651 |
| Matheus Versa | 204090 |

## Funcionalidades

- **Login local** com cadastro de usuário (senha guardada como hash SHA-256) e sessão que lembra quem está logado.
- **Minha estante**: lista de livros com filtro por status (Lido, Lendo, Quero ler/comprar).
- **CRUD de livros**: adicionar, ver detalhes, editar e excluir (com confirmação).
- **Regras de negócio no formulário**:
  - título e autor obrigatórios;
  - ISBN opcional, mas, se informado, precisa ter 10 ou 13 dígitos com dígito verificador válido e não pode se repetir na estante;
  - nota (0,5 a 5 estrelas) e data de conclusão só para livros com status **Lido**;
  - datas de início e conclusão não podem estar no futuro, e a conclusão não pode ser antes do início;
  - livros em **Quero ler/comprar** não têm datas de leitura.
- **Preenchimento pelo ISBN**: a lupa no campo ISBN busca título, autor, categoria e páginas na Open Library.
- **Consulta de preço** no Google Books (Google Play Livros), com link para a loja.
- **Compartilhar** um livro com outros apps (Intent implícita `ACTION_SEND`).
- **Recomendações** com base nos livros mais bem avaliados (aba Recomendações).
- **Perfil** com os dados do usuário e o botão Sair.
- Sem internet o app não trava: mostra um aviso e continua funcionando com os dados locais.

## Telas

As capturas de tela do emulador estão em [docs/telas.md](docs/telas.md).

## Tecnologias utilizadas

- **Kotlin** e **Android Studio**, com layouts **XML** (Empty Views Activity, sem Compose)
- **Android Gradle Plugin 9.5** (Kotlin embutido), Gradle 9.8, compileSdk/targetSdk 36, minSdk 26
- **Material Components 3** (Toolbar, BottomNavigationView, TextInputLayout, Chips, Cards)
- **Activities + Fragments**: 5 Activities (Login, Cadastro, Principal, Formulário e Detalhe do livro) e 3 Fragments (Estante, Recomendações, Perfil)
- **Room 2.8** com KSP para a persistência local (tabelas `usuarios` e `livros`)
- **Retrofit 2 + Gson** para as APIs REST:
  - [Google Books API](https://developers.google.com/books) para o preço
  - [Open Library Search API](https://openlibrary.org/dev/docs/api/search) para os dados por ISBN e as recomendações
- **Coroutines** (`lifecycleScope`) para banco e rede fora da thread principal
- **JUnit 4** para os testes unitários das regras de negócio

### Organização do código

```
app/src/main/java/br/upf/booki/
├── MainActivity.kt          # BottomNavigation que troca os fragments
├── api/                     # Retrofit: Google Books, Open Library e tratamento de erros
├── data/                    # Room: AppDatabase (singleton), DAOs e conversores
├── model/                   # Classes de domínio (User, Livro, StatusLeitura) e validadores
├── ui/                      # Activities (login, cadastro, formulário e detalhe do livro)
│   └── fragments/           # Estante, Recomendações, Perfil e o adapter da lista
└── util/                    # Util (dialogoAlerta/dialogoConfirma, datas), Sessao, Senha, Conexao
```

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
3. *(Opcional, para a consulta de preço)* Crie uma chave grátis da Google Books API no
   [Google Cloud Console](https://console.cloud.google.com/apis/library/books.googleapis.com)
   e adicione ao arquivo `local.properties` na raiz do projeto (ele não vai para o Git):
   ```properties
   GOOGLE_BOOKS_API_KEY=sua_chave_aqui
   ```
   Sem a chave, o app funciona normalmente, mas a consulta de preço mostra "indisponível",
   porque o Google Books não aceita mais consultas anônimas.
4. Rode o app no emulador (botão **Run**) ou pela linha de comando:
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```
5. Na primeira abertura, toque em **Cadastre-se**, crie a conta e entre.

### Testes

```bash
./gradlew testDebugUnitTest
```

Os testes ficam em `app/src/test` e cobrem as validações de usuário, de livro (ISBN, nota e datas) e a formatação de preço.

## Fluxo de trabalho no Git

- `main`: versão estável, recebe apenas merges vindos de `develop`.
- `develop`: integração das funcionalidades.
- `feature/*` e `docs/*`: uma branch por funcionalidade, com Pull Request para `develop`.

Divisão das tarefas da dupla: [docs/TAREFAS-MILLENA.md](docs/TAREFAS-MILLENA.md).
