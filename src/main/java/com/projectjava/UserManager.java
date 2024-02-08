package com.projectjava;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

import org.bson.Document;

public class UserManager {

    private final MongoCollection<Document> userCollection;
    private final Key jwtKey;

    public UserManager(Key jwtKey) {
        this.jwtKey = jwtKey;

        MongoClient mongoClient = MongoClients
                .create("mongodb+srv://Guillaume:test@cluster0.pfjuogp.mongodb.net/javaserver");
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

    public boolean logoutUser(String username, String authToken) {
        if (isValidAuthToken(username, authToken)) {
            System.out.println("User logged out successfully!");
            return true;
        } else {
            System.err.println("Invalid authentication token. Logout failed.");
            return false;
        }
    }

    public String generateAuthToken(String username) {
        Date expiration = new Date(System.currentTimeMillis() + 3600000); // Expiration dans 1 heure

        return Jwts.builder()
                .setSubject(username)
                .setExpiration(expiration)
                .signWith(jwtKey)
                .compact();
    }

    public boolean authenticateUser(String username, String password) {
        Document query = new Document("username", username).append("password", password);

        long count = userCollection.countDocuments(query);

        return count == 1;
    }

    public boolean deleteUser(String username, String authToken) {
        if (isValidAuthToken(username, authToken)) {
            Document query = new Document("username", username);
            userCollection.deleteOne(query);
            System.out.println("User deleted successfully!");
            return true;
        } else {
            System.err.println("Invalid authentication token. User not deleted.");
            return false;
        }
    }

    private boolean isValidAuthToken(String username, String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(jwtKey).build().parseClaimsJws(authToken);
            return true;
        } catch (Exception e) {
            System.err.println("Invalid authentication token. Error: " + e.getMessage());
            return false;
        }
    }
}
