package com.familyfirstsoftware.contactApi.resource;

import com.familyfirstsoftware.contactApi.domain.Contact;
import com.familyfirstsoftware.contactApi.repo.ContactRepo;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final ContactRepo contactRepo; // Inject the ContactRepo to retrieve contact details
    private Map<String, String> conversationHistory = new HashMap<>(); // Store conversation history for each contact

    public ChatController(ChatClient.Builder builder, ContactRepo contactRepo) {
        this.chatClient = builder.build();
        this.contactRepo = contactRepo;
    }

    @PostMapping("/chat/{contactId}")
    public String chatWithContact(@PathVariable String contactId, @RequestBody String message) {
        // Retrieve the contact's name dynamically from the database
        Contact contact = contactRepo.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found"));

        String contactName = contact.getName(); // Get the contact's name (e.g., Gene)
        String previousMessages = conversationHistory.getOrDefault(contactId, ""); // Retrieve previous conversation or empty string

        // Construct the prompt dynamically using the contact's name
        String prompt = previousMessages + "\n" + "User: " + message + "\n" + contactName + " (Bob's Burgers):";

        // Send the request to the AI
        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        // Update the conversation history with the new message and response
        conversationHistory.put(contactId, previousMessages + "\n" + "User: " + message + "\n" + contactName + " (Bob's Burgers): " + response);

        return response;
    }
}


