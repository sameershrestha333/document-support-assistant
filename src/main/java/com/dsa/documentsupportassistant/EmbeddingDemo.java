package com.dsa.documentsupportassistant;

import com.dsa.documentsupportassistant.service.EmbeddingService;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

public class EmbeddingDemo {
    public static void main(String[] args) {
        try (var context = new SpringApplicationBuilder(
                DocumentSupportAssistantApplication.class)
                .web(WebApplicationType.NONE)
                .run(args)) {
            var service = context.getBean(EmbeddingService.class);

            float[] question = service.embed("What's the largest file I can send?");
            float[] uploadLimit = service.embed("The maximum upload size is 10 MB.");
            float[] fileTypes = service.embed("The service accepts only PDF and TXT files.");

            System.out.printf("Dimensions: %d, %d, %d%n",
                    question.length, uploadLimit.length, fileTypes.length);
            System.out.printf("Upload limit: %.4f%n", cosineSimilarity(question, uploadLimit));
            System.out.printf("File types:   %.4f%n", cosineSimilarity(question, fileTypes));
        }
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0;
        double normA = 0;
        double normB = 0;

        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}