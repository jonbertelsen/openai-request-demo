package app.services;

import app.dtos.QuestionDTO;
import app.dtos.QuizDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

public class OpenAiQuizService {
    private static final URI ENDPOINT = URI.create("https://api.openai.com/v1/responses");
    private static final String MODEL = "gpt-5.6-luna";
    private static final Map<String, Integer> DIFFICULTIES =
            Map.of("meget let", 2, "let", 3, "middel", 3, "svær", 2);
    private static final String SCHEMA = """
            {
              "type": "object",
              "properties": {
                "questions": {
                  "type": "array", "minItems": 10, "maxItems": 10,
                  "items": {
                    "type": "object",
                    "properties": {
                      "question": {"type": "string"},
                      "answer": {"type": "string"},
                      "difficulty": {"type": "string", "enum": ["meget let", "let", "middel", "svær"]}
                    },
                    "required": ["question", "answer", "difficulty"],
                    "additionalProperties": false
                  }
                }
              },
              "required": ["questions"],
              "additionalProperties": false
            }
            """;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client;
    private final URI endpoint;
    private final String apiKey;

    public OpenAiQuizService() {
        this(System.getenv("OPEN-AI-KEY"), ENDPOINT);
    }

    // Lokal endpoint til tests uden betalte API-kald.
    OpenAiQuizService(String apiKey, URI endpoint) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Miljøvariablen OPEN-AI-KEY mangler eller er tom.");
        }
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public QuizDTO generateQuiz(String systemPrompt, String userPrompt) throws IOException, InterruptedException {
        if (systemPrompt == null || systemPrompt.isBlank() || userPrompt == null || userPrompt.isBlank()) {
            throw new IllegalArgumentException("Systemprompt og userprompt skal være udfyldt.");
        }
        var body = mapper.createObjectNode();
        body.put("model", MODEL);
        body.put("store", false);
        body.put("max_output_tokens", 3000);
        body.putObject("reasoning").put("effort", "none");
        var input = body.putArray("input");
        input.addObject().put("role", "system").put("content", systemPrompt);
        input.addObject().put("role", "user").put("content", userPrompt);
        var format = body.putObject("text").putObject("format");
        format.put("type", "json_schema").put("name", "harry_potter_quiz").put("strict", true);
        format.set("schema", mapper.readTree(SCHEMA));
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            // Udskriv ikke request, nøgle eller ukontrolleret response body.
            throw new IOException("OpenAI-request fejlede med HTTP " + response.statusCode() + ".");
        }
        return parseResponse(response.body());
    }

    QuizDTO parseResponse(String responseBody) throws IOException {
        JsonNode response = mapper.readTree(responseBody);
        if (response == null || !"completed".equals(response.path("status").asText())) {
            throw new IOException("OpenAI returnerede et ufuldstændigt eller fejlet svar.");
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode item : response.path("output")) {
            if (!"message".equals(item.path("type").asText())) continue;
            for (JsonNode content : item.path("content")) {
                if ("refusal".equals(content.path("type").asText())) {
                    throw new IOException("OpenAI afviste at generere quizzen.");
                }
                if ("output_text".equals(content.path("type").asText())) {
                    text.append(content.path("text").asText());
                }
            }
        }
        if (text.isEmpty()) throw new IOException("OpenAI-svaret indeholder ingen quiz-JSON.");
        QuizDTO quiz = mapper.readValue(text.toString(), QuizDTO.class);
        validateQuiz(quiz);
        return quiz;
    }

    private void validateQuiz(QuizDTO quiz) throws IOException {
        if (quiz == null || quiz.questions() == null || quiz.questions().size() != 10) {
            throw new IOException("Quizzen skal indeholde præcis 10 spørgsmål.");
        }
        Map<String, Integer> counts = new HashMap<>();
        var questions = new HashSet<String>();
        for (QuestionDTO question : quiz.questions()) {
            if (question == null || question.question() == null || question.question().isBlank()
                    || question.answer() == null || question.answer().isBlank()
                    || question.difficulty() == null || !DIFFICULTIES.containsKey(question.difficulty())) {
                throw new IOException("Quizzen indeholder et ugyldigt spørgsmål, svar eller en sværhedsgrad.");
            }
            if (!questions.add(question.question().strip().toLowerCase(Locale.ROOT))) {
                throw new IOException("Quizzen indeholder gentagne spørgsmål.");
            }
            counts.merge(question.difficulty(), 1, Integer::sum);
        }
        if (!counts.equals(DIFFICULTIES)) {
            throw new IOException("Quizzen har ikke den ønskede fordeling af sværhedsgrader.");
        }
    }
}
