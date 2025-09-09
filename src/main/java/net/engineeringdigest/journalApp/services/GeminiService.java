package net.engineeringdigest.journalApp.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;

@Service
@Slf4j
public class GeminiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model}")
    private String model;

    @Value("${gemini.api.base-url}")
    private String baseUrl;

    @Value("${gemini.api.max-tokens}")
    private int maxTokens;

    @Value("${gemini.api.temperature}")
    private double temperature;

    @Value("${gemini.api.request-timeout}")
    private int requestTimeout;

    public GeminiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Analyze sentiment of a single journal entry
     */
    public SentimentAnalysisResult analyzeSentiment(String title, String content) {
        try {
            String prompt = buildSentimentAnalysisPrompt(title, content);
            String response = callGeminiAPI(prompt);
            return parseSentimentResponse(response);
        } catch (Exception e) {
            log.error("Error analyzing sentiment for entry: {}", e.getMessage());
            return createFallbackSentimentResult();
        }
    }

    /**
     * Generate comprehensive bi-weekly report
     */
    public String generateBiweeklyReport(List<Map<String, Object>> entriesData, String userName) {
        try {
            String prompt = buildReportGenerationPrompt(entriesData, userName);
            return callGeminiAPI(prompt);
        } catch (Exception e) {
            log.error("Error generating bi-weekly report for user {}: {}", userName, e.getMessage());
            return createFallbackReport(userName);
        }
    }

    private String callGeminiAPI(String prompt) {
        String url = baseUrl + "/models/" + model + ":generateContent?key=" + apiKey;
        
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> contents = new HashMap<>();
        Map<String, Object> parts = new HashMap<>();
        parts.put("text", prompt);
        contents.put("parts", Arrays.asList(parts));
        requestBody.put("contents", Arrays.asList(contents));
        
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("maxOutputTokens", maxTokens);
        generationConfig.put("temperature", temperature);
        requestBody.put("generationConfig", generationConfig);

        try {
            String response = webClient.post()
                    .uri(url)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(requestTimeout))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(throwable -> throwable instanceof WebClientResponseException &&
                                    ((WebClientResponseException) throwable).getStatusCode().value() == 429))
                    .block();

            return extractTextFromResponse(response);
        } catch (WebClientResponseException e) {
            log.error("Gemini API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Gemini API call failed", e);
        } catch (Exception e) {
            log.error("Unexpected error calling Gemini API: {}", e.getMessage());
            throw new RuntimeException("Gemini API call failed", e);
        }
    }

    private String extractTextFromResponse(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode candidates = jsonNode.get("candidates");
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).get("content");
                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray() && parts.size() > 0) {
                        JsonNode text = parts.get(0).get("text");
                        if (text != null) {
                            return text.asText();
                        }
                    }
                }
            }
            log.warn("Unexpected response format from Gemini API: {}", response);
            return "Unable to process response";
        } catch (Exception e) {
            log.error("Error parsing Gemini API response: {}", e.getMessage());
            return "Error processing response";
        }
    }

    private String buildSentimentAnalysisPrompt(String title, String content) {
        return String.format(
            "Analyze the sentiment of this journal entry and return a JSON response with the following structure:\n" +
            "{\n" +
            "    \"sentimentScore\": <number between -1.0 and 1.0>,\n" +
            "    \"sentimentLabel\": \"<positive|negative|neutral>\",\n" +
            "    \"emotions\": [\"<emotion1>\", \"<emotion2>\", ...],\n" +
            "    \"keywords\": [\"<keyword1>\", \"<keyword2>\", ...]\n" +
            "}\n\n" +
            "Guidelines:\n" +
            "- sentimentScore: -1.0 = very negative, 0.0 = neutral, 1.0 = very positive\n" +
            "- sentimentLabel: \"positive\" (>0.1), \"negative\" (<-0.1), \"neutral\" (-0.1 to 0.1)\n" +
            "- emotions: List 2-5 primary emotions detected (joy, sadness, anxiety, anger, fear, excitement, etc.)\n" +
            "- keywords: Extract 3-8 key themes, topics, or significant words\n\n" +
            "Journal Entry:\n" +
            "Title: %s\n" +
            "Content: %s\n\n" +
            "Return only the JSON response, no additional text.",
            title, content);
    }

    private String buildReportGenerationPrompt(List<Map<String, Object>> entriesData, String userName) {
        StringBuilder entriesText = new StringBuilder();
        for (Map<String, Object> entry : entriesData) {
            entriesText.append(String.format("Date: %s\nTitle: %s\nContent: %s\nSentiment: %s (%.2f)\nEmotions: %s\nKeywords: %s\n\n",
                    entry.get("date"), entry.get("title"), entry.get("content"),
                    entry.get("sentimentLabel"), entry.get("sentimentScore"),
                    entry.get("emotions"), entry.get("keywords")));
        }

        return String.format(
            "Generate a comprehensive bi-weekly mental health report for %s based on their journal entries.\n\n" +
            "Create a warm, supportive, and insightful analysis that includes:\n" +
            "1. Overall mood trajectory (improving/declining/stable)\n" +
            "2. Emotional pattern analysis (triggers, cycles, peaks/valleys)\n" +
            "3. Positive/negative sentiment ratio with trend analysis\n" +
            "4. Behavioral insights and personalized recommendations\n" +
            "5. Key life themes and events mentioned\n" +
            "6. Supportive observations and encouragement\n\n" +
            "Write in a friendly, caring tone as if you're a supportive friend who genuinely cares about their wellbeing.\n" +
            "Keep the report between 300-500 words.\n\n" +
            "Journal Entries from the past 14 days:\n%s\n\n" +
            "Generate the report in HTML format suitable for email, with proper headings and formatting.",
            userName, entriesText.toString());
    }

    private SentimentAnalysisResult parseSentimentResponse(String response) {
        try {
            // Try to extract JSON from the response
            String jsonStr = response.trim();
            if (jsonStr.startsWith("```json")) {
                jsonStr = jsonStr.substring(7);
            }
            if (jsonStr.endsWith("```")) {
                jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
            }
            
            JsonNode jsonNode = objectMapper.readTree(jsonStr);
            
            double sentimentScore = jsonNode.get("sentimentScore").asDouble();
            String sentimentLabel = jsonNode.get("sentimentLabel").asText();
            
            List<String> emotions = new ArrayList<>();
            JsonNode emotionsNode = jsonNode.get("emotions");
            if (emotionsNode != null && emotionsNode.isArray()) {
                for (JsonNode emotion : emotionsNode) {
                    emotions.add(emotion.asText());
                }
            }
            
            List<String> keywords = new ArrayList<>();
            JsonNode keywordsNode = jsonNode.get("keywords");
            if (keywordsNode != null && keywordsNode.isArray()) {
                for (JsonNode keyword : keywordsNode) {
                    keywords.add(keyword.asText());
                }
            }
            
            return new SentimentAnalysisResult(sentimentScore, sentimentLabel, emotions, keywords);
        } catch (Exception e) {
            log.error("Error parsing sentiment analysis response: {}", e.getMessage());
            return createFallbackSentimentResult();
        }
    }

    private SentimentAnalysisResult createFallbackSentimentResult() {
        return new SentimentAnalysisResult(0.0, "neutral", 
                Arrays.asList("unknown"), Arrays.asList("journal", "entry"));
    }

    private String createFallbackReport(String userName) {
        return String.format(
            "<h2>Your Bi-weekly Mental Health Report</h2>\n" +
            "<p>Hi %s,</p>\n" +
            "<p>We encountered a temporary issue generating your personalized report, but we wanted to reach out anyway!</p>\n" +
            "<p>Thank you for continuing to journal and prioritize your mental health. Regular journaling is a powerful tool for self-reflection and emotional well-being.</p>\n" +
            "<p>We'll have your next report ready in two weeks. Keep up the great work!</p>\n" +
            "<p>Take care,<br>Your Journal App Team</p>",
            userName);
    }

    // Inner class for sentiment analysis results
    public static class SentimentAnalysisResult {
        private final double sentimentScore;
        private final String sentimentLabel;
        private final List<String> emotions;
        private final List<String> keywords;

        public SentimentAnalysisResult(double sentimentScore, String sentimentLabel, 
                                     List<String> emotions, List<String> keywords) {
            this.sentimentScore = sentimentScore;
            this.sentimentLabel = sentimentLabel;
            this.emotions = emotions;
            this.keywords = keywords;
        }

        public double getSentimentScore() { return sentimentScore; }
        public String getSentimentLabel() { return sentimentLabel; }
        public List<String> getEmotions() { return emotions; }
        public List<String> getKeywords() { return keywords; }
    }
}
