package app;

import app.dtos.QuizDTO;
import app.services.OpenAiQuizService;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {
    public static void main(String[] args) throws Exception {
        String systemPrompt = """
                Du laver quizzer på dansk med entydige spørgsmål og korte, korrekte svar.
                Brug Harry Potter-bøgerne som grundlag. Undgå tvetydige spørgsmål og lange citater.
                Returnér kun JSON i det krævede format.
                """;
        String userPrompt = """
                Lav præcis 10 forskellige spørgsmål og svar om Harry Potter.
                Fordel sværhedsgraderne sådan: 2 meget lette, 3 lette, 3 middel og 2 svære.
                Brug difficulty-værdierne: meget let, let, middel og svær.
                """;
        QuizDTO quiz = new OpenAiQuizService().generateQuiz(systemPrompt, userPrompt);
        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(quiz));
    }
}
