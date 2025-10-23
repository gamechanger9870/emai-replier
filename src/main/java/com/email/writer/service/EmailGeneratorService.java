package com.email.writer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.email.writer.dtos.EmailRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class EmailGeneratorService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    private final WebClient webClient;

    public EmailGeneratorService(WebClient.Builder builder) {
        this.webClient = builder.build();
    }


    public String generateEmailReply(EmailRequest emailRequest)
    {
        // we need to give a prompt
        String prompt=buildPrompt(emailRequest);

        // buiding the request
        Map<String,Object> requestBody=Map.of("contents",new Object[]{
                Map.of("parts",new Object[]{
                        Map.of("text",prompt)
                })
        });

        // let hit request and get response
        String response =webClient.post()
                .uri(geminiApiUrl)
                .header("x-goog-api-key", geminiApiKey)
                .header("Content-Type","application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();



        // returning the response
        return extractfromJson(response);
    }

    private String extractfromJson(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);

            // Navigate: candidates[0] -> content -> parts[0] -> text
            JsonNode textNode = rootNode
                    .path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text");

            if (textNode.isMissingNode()) {
                return "Key 'text' not found in JSON.";
            }
            return textNode.asText();
        }catch (Exception e){
            return "Error processing request : "+e.getMessage();
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt=new StringBuilder();
        prompt.append("Please reply on this Email");
        if(!emailRequest.getTone().isEmpty())
            prompt.append("Use ").append(emailRequest.getTone()).append("to reply the mail");
        prompt.append("\n Original mail is").append(emailRequest.getEmailContent());
        return prompt.toString();
    }
}
