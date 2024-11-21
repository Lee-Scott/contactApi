package com.familyfirstsoftware.contactApi.resource;

import com.familyfirstsoftware.contactApi.domain.Contact;
import com.familyfirstsoftware.contactApi.repo.ContactRepo;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatClient chatClient;
    private final ContactRepo contactRepo; // Inject the ContactRepo to retrieve contact details
    private Map<String, String> conversationHistory = new HashMap<>(); // Store conversation history for each contact

    public ChatController(ChatClient.Builder builder, ContactRepo contactRepo) {
        this.chatClient = builder.build();
        this.contactRepo = contactRepo;
    }

    @PostMapping("/{contactId}")
    public String chatWithContact(@PathVariable String contactId, @RequestBody String message) {

        if (!conversationHistory.isEmpty() && !conversationHistory.containsKey(contactId)) {
            conversationHistory.clear();
        }

        // Retrieve the contact's information from the database
        Contact contact = contactRepo.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found"));

        // Ensure conversation history is initialized for the contact
        conversationHistory.putIfAbsent(contactId, "");

        // Generate the prompt based on whether this is the first message or a continuation
        String prompt = conversationHistory.get(contactId).isEmpty()
                ? generateFirstMessagePrompt(contact, message)
                : generateContinuationPrompt(contactId, message);

        // Send the request to the AI
        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        // Update the conversation history
        updateConversationHistory(contactId, message, contact.getName(), response);
        //System.out.println("Conversation history: " + conversationHistory);

        return response;
    }

    private String generateFirstMessagePrompt(Contact contact, String message) {
        return "You are " + contact.getName() + ", a well-defined character. Respond authentically as "
                + contact.getName() + ", staying true to their personality, tone, and speaking style. " +
                "Here is some background information about you:\n" +
                "Name: " + contact.getName() + "\n" +
                "Email: " + contact.getEmail() + "\n" +
                "Title: " + contact.getTitle() + "\n" +
                "Phone: " + contact.getPhone() + "\n" +
                "Address: " + contact.getAddress() + "\n" +
                "Status: " + contact.getStatus() + "\n" +
                "Photo URL: " + contact.getPhotoUrl() + "\n" +
                "Description: " + contact.getDescription() + "\n" +
                "Begin the conversation naturally as " + contact.getName() +
                " and avoid repeating generic greetings like 'Hi' or 'Hello' if the user has already greeted you. " +
                "The first user message is:\n" +
                "User: " + message + "\n" +
                contact.getName() + ":";
    }

    private String generateContinuationPrompt(String contactId, String message) {
        String previousMessages = conversationHistory.get(contactId);
        return "Continue the conversation as " + contactRepo.findById(contactId).orElseThrow().getName() +
                ", a character from a fictional universe. Respond naturally and in character without repeating initial greetings. " +
                "The conversation so far is:\n" +
                previousMessages + "\n" +
                "User: " + message + "\n" +
                contactRepo.findById(contactId).orElseThrow().getName() + ":";
    }

    private void updateConversationHistory(String contactId, String userMessage, String contactName, String response) {
        String previousMessages = conversationHistory.get(contactId);
        String updatedHistory = previousMessages + "\n" +
                "User: " + userMessage + "\n" +
                contactName + ": " + response;
        conversationHistory.put(contactId, updatedHistory);
    }


    @GetMapping("/between/{contactId1}/{contactId2}")
    public String chatBetweenContacts(@PathVariable String contactId1, @PathVariable String contactId2) {

        // Clear conversation history if it's unrelated
        if (!conversationHistory.isEmpty() &&
                (!conversationHistory.containsKey(contactId1) || !conversationHistory.containsKey(contactId2))) {
            conversationHistory.clear();
        }

        // Retrieve contact details
        Contact contact1 = contactRepo.findById(contactId1).orElseThrow(() -> new RuntimeException("Contact 1 not found"));
        Contact contact2 = contactRepo.findById(contactId2).orElseThrow(() -> new RuntimeException("Contact 2 not found"));

        String name1 = contact1.getName();
        String name2 = contact2.getName();

        // Initialize conversation prompt
        String conversation = conversationHistory.getOrDefault(contactId1 + "-" + contactId2, "");

        // Simulate conversation between contacts
        for (int i = 0; i < 5; i++) { // Example: 5 message exchanges
            String response1 = getResponseFromContact(contact1, conversation, name2);
            conversation += "\n" + name1 + ": " + response1;

            String response2 = getResponseFromContact(contact2, conversation, name1);
            conversation += "\n" + name2 + ": " + response2;
        }

        // Store conversation history
        conversationHistory.put(contactId1 + "-" + contactId2, conversation);

        System.out.println("Returning conversation for: " + contactId1 + " and " + contactId2);
        System.out.println("Conversation: " + conversation);

        return "Conversation between " + name1 + " and " + name2 + ":\n" + conversation;
    }

    private String getResponseFromContact(Contact contact, String conversationSoFar, String otherContactName) {
        String prompt = "You are " + contact.getName() + ", having a conversation with " + otherContactName + ".\n" +
                "Respond authentically and in character. The conversation so far is:\n" + conversationSoFar + "\n" +
                contact.getName() + ":";
        return chatClient.prompt().user(prompt).call().content();
    }


}


