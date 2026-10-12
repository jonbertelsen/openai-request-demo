# Harry Potter-quiz med OpenAI

Et lille Java-projekt, der sender en systemprompt og en userprompt til OpenAI og får 10 Harry Potter-spørgsmål med svar og varierende sværhedsgrad. Java `HttpClient` sender requestet, og Jackson konverterer JSON-svaret til en `QuizDTO`.

## Opsætning i IntelliJ IDEA

Du skal bruge **JDK 25**, IntelliJ IDEA og en OpenAI API-nøgle med adgang til modellen `gpt-5.6-luna`.

1. Klon repositoryet fra GitHub, og åbn projektmappen i IntelliJ.
2. Indlæs projektet som et Maven-projekt via `pom.xml`, og lad IntelliJ hente afhængighederne.
3. Vælg JDK 25 som **Project SDK** under **File → Project Structure → Project**.
4. Åbn `src/main/java/app/Main.java`. Kør `Main` via den grønne pil ved `main` for at oprette en kørselskonfiguration. Første kørsel vil give en fejl, hvis API-nøglen endnu ikke er sat.
5. Åbn **Run → Edit Configurations**, og vælg konfigurationen for `Main`.
6. Find **Environment variables**, og tilføj:

   | Navn | Værdi |
   | --- | --- |
   | `OPEN-AI-KEY` | Din OpenAI API-nøgle |

   Hvis feltet er skjult, kan du tilføje det via **Modify options → Environment variables**.

7. Gem med **Apply / OK**, og kør `Main` igen.

Navnet `OPEN-AI-KEY` skal skrives præcis som vist, da koden læser det med `System.getenv("OPEN-AI-KEY")`. Hold nøglen i din lokale kørselskonfiguration, og del eller commit aldrig en konfiguration med nøglen til GitHub. API-kald bruger din OpenAI-kontos API-forbrug.

## Resultat og kode

Quizzen gemmes i variablen `quiz` som en `QuizDTO` og udskrives som formateret JSON i konsollen. Hvert spørgsmål har felterne `question`, `answer` og `difficulty`.

- `Main`: Definerer prompts og starter kaldet.
- `app.services.OpenAiQuizService`: Sender requestet og konverterer og validerer svaret.
- `app.dtos.QuizDTO` og `QuestionDTO`: Indeholder quizzen og dens spørgsmål.

Du kan ændre prompts i `Main`. Servicen kræver præcis 10 spørgsmål fordelt på 2 meget lette, 3 lette, 3 middel og 2 svære; ændrer du antal eller fordeling, skal servicens JSON Schema og validering også tilpasses.
