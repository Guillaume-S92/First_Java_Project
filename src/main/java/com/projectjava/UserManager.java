package com.projectjava;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class UserManager {

    private static final MongoClient mongoClient;

    static {
        mongoClient = MongoClients.create("mongodb+srv://Guillaume:test@cluster0.pfjuogp.mongodb.net/javaserver");
    }

    private final MongoCollection<Document> userCollection;

    public UserManager() {
        MongoDatabase database = mongoClient.getDatabase("javaserver");
        userCollection = database.getCollection("users");
    }

    public void createUser(String username, String password, String email) {
        Document user = new Document("username", username)
                .append("password", password)
                .append("email", email);

        try {
            userCollection.insertOne(user);
            System.out.println("User created successfully!");
        } catch (Exception e) {
            System.err.println("Failed to insert user. Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}