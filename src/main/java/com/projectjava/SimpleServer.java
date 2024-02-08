package com.projectjava;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.bson.Document;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.IndexOptions;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.Key;
import java.util.Map;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

public class SimpleServer {

    private static final Key jwtKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final UserManager userManager = new UserManager(jwtKey);
    private static MongoClient mongoClient;

    public static void main(String[] args) throws IOException {
        int port = 3001;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new MyHandler());
        server.createContext("/register", new RegisterHandler());
        server.createContext("/login", new LoginHandler());
        server.createContext("/logout", new LogoutHandler());
        server.createContext("/delete", new DeleteHandler());
        server.setExecutor(null);
        System.out.println("Server is running on port " + port);

        try {
            mongoClient = MongoClients.create("mongodb+srv://Guillaume:test@cluster0.pfjuogp.mongodb.net/javaserver");
            MongoDatabase database = mongoClient.getDatabase("javaserver");

            MongoCollection<Document> userCollection = database.getCollection("users");
            userCollection.createIndex(Indexes.ascending("username"), new IndexOptions().unique(true));

            System.out.println("Connected to MongoDB successfully!");
        } catch (Exception e) {
            System.err.println("Failed to connect to MongoDB. Error: " + e.getMessage());
            e.printStackTrace();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (mongoClient != null) {
                mongoClient.close();
            }
        }));

        server.start();
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "Hello, this is your HTTP server!";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equals(t.getRequestMethod())) {
                InputStreamReader isr = new InputStreamReader(t.getRequestBody(), "utf-8");
                BufferedReader br = new BufferedReader(isr);
                String query = br.readLine();

                Map<String, String> params = HttpUtils.getParams(query);

                String username = params.get("username");
                String password = params.get("password");
                String email = params.get("email");

                System.out.println("Received POST request to register user:");
                System.out.println("Username: " + username);
                System.out.println("Password: " + password);
                System.out.println("Email: " + email);

                UserManager userManager = SimpleServer.userManager;
                userManager.createUser(username, password, email);

                String response = "User registration logic goes here.";

                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else if ("GET".equals(t.getRequestMethod())) {
                String response = "This is the registration page.";
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                t.sendResponseHeaders(405, 0);
                t.close();
            }
        }
    }

    // Modify the LoginHandler class
    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equals(t.getRequestMethod())) {
                InputStreamReader isr = new InputStreamReader(t.getRequestBody(), "utf-8");
                BufferedReader br = new BufferedReader(isr);
                String query = br.readLine();

                Map<String, String> params = HttpUtils.getParams(query);

                String username = params.get("username");
                String password = params.get("password");

                System.out.println("Received POST request to login user:");
                System.out.println("Username: " + username);
                System.out.println("Password: " + password);

                UserManager userManager = SimpleServer.userManager;
                boolean loginSuccessful = userManager.authenticateUser(username, password);

                String response;
                if (loginSuccessful) {
                    // Générer un jeton d'authentification lors de la connexion réussie
                    String authToken = userManager.generateAuthToken(username);
                    response = "Login successful! AuthToken: " + authToken;
                } else {
                    response = "Login failed. Please check your username and password.";
                }

                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else if ("GET".equals(t.getRequestMethod())) {
                String response = "This is the login page.";
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                t.sendResponseHeaders(405, 0);
                t.close();
            }
        }
    }

    static class LogoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equals(t.getRequestMethod())) {
                InputStreamReader isr = new InputStreamReader(t.getRequestBody(), "utf-8");
                BufferedReader br = new BufferedReader(isr);
                String query = br.readLine();

                Map<String, String> params = HttpUtils.getParams(query);

                String username = params.get("username");
                String authToken = params.get("authToken");

                // Appelez la méthode de déconnexion de l'utilisateur avec authentification
                boolean logoutSuccessful = SimpleServer.userManager.logoutUser(username, authToken);

                // Envoyez la réponse en fonction du succès de la déconnexion
                String response;
                if (logoutSuccessful) {
                    response = "Logout successful!";
                } else {
                    response = "Logout failed. Please check the username or authentication token.";
                }

                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                t.sendResponseHeaders(405, 0);
                t.close();
            }
        }
    }

    static class DeleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("DELETE".equals(t.getRequestMethod())) {
                InputStreamReader isr = new InputStreamReader(t.getRequestBody(), "utf-8");
                BufferedReader br = new BufferedReader(isr);
                String query = br.readLine();

                Map<String, String> params = HttpUtils.getParams(query);

                // Récupérez le nom d'utilisateur à supprimer et le jeton d'authentification
                String usernameToDelete = params.get("username");
                String authToken = params.get("authToken");

                // Appelez la méthode de suppression de l'utilisateur avec authentification
                boolean deletionSuccessful = SimpleServer.userManager.deleteUser(usernameToDelete, authToken);

                // Envoyez la réponse en fonction du succès de la suppression
                String response;
                if (deletionSuccessful) {
                    response = "User deletion successful!";
                } else {
                    response = "User deletion failed. Please check the username or authentication token.";
                }

                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                t.sendResponseHeaders(405, 0);
                t.close();
            }
        }
    }

}
