package com.college.guide;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReturnDocument;
import org.bson.Document;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Updates.inc;

/** Owns the single MongoDB connection and creates the starter data once. */
public final class Database {
  private static final String URI = System.getenv().getOrDefault("MONGODB_URI", "mongodb://localhost:27017");
  private static final String NAME = System.getenv().getOrDefault("MONGODB_DATABASE", "faculty_guide_db");
  private static MongoClient client;

  private Database() { }

  public static synchronized MongoDatabase get() {
    if (client == null) client = MongoClients.create(URI);
    return client.getDatabase(NAME);
  }

  static int nextId(String collection) {
    Document counter = get().getCollection("counters").findOneAndUpdate(
        eq("_id", collection), inc("value", 1),
        new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
    return counter.getInteger("value");
  }

  public static void initialize() {
    MongoDatabase db = get();
    db.getCollection("users").createIndex(ascending("email"), new IndexOptions().unique(true));
    db.getCollection("students").createIndex(ascending("registrationNo"), new IndexOptions().unique(true));
    db.getCollection("projects").createIndex(ascending("studentId"), new IndexOptions().unique(true));
    db.getCollection("guideRequests").createIndex(ascending("studentId", "facultyId", "domainId"), new IndexOptions().unique(true));

    MongoCollection<Document> users = db.getCollection("users");
    if (users.countDocuments() != 0) return;

    users.insertOne(new Document("_id", 1).append("name", "Aarav Student").append("email", "student1@college.edu")
        .append("passwordHash", Passwords.hash("password")).append("role", "STUDENT"));
    users.insertOne(new Document("_id", 2).append("name", "Dr. Priya Sharma").append("email", "faculty1@college.edu")
        .append("passwordHash", Passwords.hash("password")).append("role", "FACULTY"));
    users.insertOne(new Document("_id", 3).append("name", "Department Coordinator").append("email", "admin@college.edu")
        .append("passwordHash", Passwords.hash("password")).append("role", "ADMIN"));

    db.getCollection("students").insertOne(new Document("_id", 1).append("registrationNo", "23BCE1001")
        .append("department", "Computer Science").append("semester", 5));
    db.getCollection("faculty").insertOne(new Document("_id", 2).append("department", "Computer Science")
        .append("maxStudents", 4).append("currentStudents", 0).append("domainIds", java.util.List.of(1, 2, 4)));

    String[] domainNames = {"Artificial Intelligence", "Machine Learning", "Blockchain", "Data Science", "Cyber Security", "Cloud Computing", "IoT"};
    for (int i = 0; i < domainNames.length; i++) db.getCollection("domains").insertOne(new Document("_id", i + 1).append("name", domainNames[i]));
    db.getCollection("settings").insertOne(new Document("_id", "allocation").append("locked", false));
    db.getCollection("counters").insertMany(java.util.List.of(
        new Document("_id", "users").append("value", 3), new Document("_id", "guideRequests").append("value", 0),
        new Document("_id", "projects").append("value", 0), new Document("_id", "milestones").append("value", 0),
        new Document("_id", "announcements").append("value", 0)));
  }
}
