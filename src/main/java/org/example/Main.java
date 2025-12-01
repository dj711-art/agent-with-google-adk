package org.example;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.GoogleSearchTool;


//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    static void main(String[] args) {
        LlmAgent rootAgent = LlmAgent.builder()
                .name("search_assistant")
                .description("An assistant that can search the web.")
                .model("gemini-2.0-flash") // Or your preferred models
                .instruction("You are a helpful assistant. Answer user questions using Google Search when needed.")
                .tools(new GoogleSearchTool())
                .build();
    }

}
