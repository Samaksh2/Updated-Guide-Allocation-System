package com.college.guide;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.sun.net.httpserver.HttpExchange;
import java.time.Instant;
import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

/** MongoDB implementation of every REST endpoint used by the browser. */
final class MongoApi {
  private MongoApi() { }

  static void handle(HttpExchange x, String path) throws Exception {
    Map<String, String> body = App.form(x);
    App.Session me = App.session(x);
    MongoDatabase db = Database.get();

    if (path.equals("/api/login")) { login(x, db, body); return; }
    if (path.equals("/api/logout")) { App.sessions.remove(App.cookie(x)); App.json(x, "{}"); return; }
    if (me == null) throw new Exception("Please log in");
    if (path.equals("/api/me")) { App.json(x, object("name", me.name(), "role", me.role())); return; }

    if (path.equals("/api/faculty")) { App.json(x, facultyRows(db)); return; }
    if (path.equals("/api/domains")) { App.json(x, domainRows(db)); return; }
    if (path.equals("/api/announcements")) { App.json(x, announcementRows(db)); return; }
    if (path.equals("/api/request") && me.role().equals("STUDENT")) { submitRequest(db, me, body); App.json(x, message("Request submitted")); return; }
    if (path.equals("/api/my-requests") && me.role().equals("STUDENT")) { App.json(x, requestRows(db, me.id(), true)); return; }
    if (path.equals("/api/incoming") && me.role().equals("FACULTY")) { App.json(x, requestRows(db, me.id(), false)); return; }
    if (path.equals("/api/decision") && me.role().equals("FACULTY")) { decide(db, me, body); App.json(x, message("Request updated")); return; }
    if (path.equals("/api/projects")) { App.json(x, projectRows(db, me)); return; }
    if (path.equals("/api/milestone") && me.role().equals("STUDENT")) { addMilestone(db, me, body); App.json(x, message("Milestone submitted")); return; }
    if (path.equals("/api/announcement") && (me.role().equals("ADMIN") || me.role().equals("FACULTY"))) { postAnnouncement(db, me, body); App.json(x, message("Announcement posted")); return; }
    if (path.equals("/api/admin/user") && me.role().equals("ADMIN")) { addUser(db, body); App.json(x, message("User created")); return; }
    if (path.equals("/api/admin/domain") && me.role().equals("ADMIN")) { assignDomain(db, body); App.json(x, message("Domain assigned")); return; }
    if (path.equals("/api/admin/lock") && me.role().equals("ADMIN")) { db.getCollection("settings").updateOne(eq("_id", "allocation"), set("locked", Boolean.parseBoolean(body.get("locked")))); App.json(x, message("Allocation setting updated")); return; }
    if (path.equals("/api/report") && me.role().equals("ADMIN")) { App.json(x, reportRows(db)); return; }
    throw new Exception("Not authorized");
  }

  private static void login(HttpExchange x, MongoDatabase db, Map<String, String> body) throws Exception {
    Document user = db.getCollection("users").find(eq("email", body.get("email"))).first();
    if (user == null || !Passwords.matches(body.get("password"), user.getString("passwordHash"))) throw new Exception("Invalid email or password");
    String token = UUID.randomUUID().toString();
    App.sessions.put(token, new App.Session(id(user), user.getString("name"), user.getString("role")));
    x.getResponseHeaders().add("Set-Cookie", "sid=" + token + "; Path=/; HttpOnly");
    App.json(x, object("name", user.getString("name"), "role", user.getString("role")));
  }

  private static void submitRequest(MongoDatabase db, App.Session me, Map<String, String> body) throws Exception {
    int facultyId = number(body, "facultyId"), domainId = number(body, "domainId");
    Document setting = db.getCollection("settings").find(eq("_id", "allocation")).first();
    if (setting != null && Boolean.TRUE.equals(setting.getBoolean("locked"))) throw new Exception("Guide allocations are locked by the coordinator");
    if (db.getCollection("projects").find(eq("studentId", me.id())).first() != null) throw new Exception("You already have an approved guide");
    Document faculty = db.getCollection("faculty").find(eq("_id", facultyId)).first();
    if (faculty == null || id(faculty, "currentStudents") >= id(faculty, "maxStudents")) throw new Exception("Faculty capacity is full");
    if (db.getCollection("guideRequests").find(and(eq("studentId", me.id()), eq("facultyId", facultyId), eq("domainId", domainId))).first() != null) throw new Exception("You already requested this faculty and domain");
    db.getCollection("guideRequests").insertOne(new Document("_id", Database.nextId("guideRequests")).append("studentId", me.id())
        .append("facultyId", facultyId).append("domainId", domainId).append("status", "PENDING").append("requestDate", new Date()));
  }

  private static void decide(MongoDatabase db, App.Session me, Map<String, String> body) throws Exception {
    int requestId = number(body, "requestId");
    Document request = db.getCollection("guideRequests").find(and(eq("_id", requestId), eq("facultyId", me.id()), eq("status", "PENDING"))).first();
    if (request == null) throw new Exception("Request is unavailable");
    if ("REJECTED".equals(body.get("status"))) { db.getCollection("guideRequests").updateOne(eq("_id", requestId), set("status", "REJECTED")); return; }
    if (db.getCollection("projects").find(eq("studentId", id(request, "studentId"))).first() != null) throw new Exception("Student already has an approved guide");

    Bson availableFaculty = and(eq("_id", me.id()), expr(new Document("$lt", List.of("$currentStudents", "$maxStudents"))));
    Document reserved = db.getCollection("faculty").findOneAndUpdate(availableFaculty, inc("currentStudents", 1), new FindOneAndUpdateOptions().returnDocument(ReturnDocument.BEFORE));
    if (reserved == null) throw new Exception("Faculty capacity is full");
    db.getCollection("guideRequests").updateOne(eq("_id", requestId), set("status", "ACCEPTED"));
    db.getCollection("projects").insertOne(new Document("_id", Database.nextId("projects")).append("title", "Untitled Project")
        .append("description", "").append("studentId", id(request, "studentId")).append("facultyId", me.id())
        .append("domainId", id(request, "domainId")).append("status", "ACTIVE").append("createdAt", new Date()));
    db.getCollection("guideRequests").updateMany(and(eq("studentId", id(request, "studentId")), ne("_id", requestId), eq("status", "PENDING")), set("status", "REJECTED"));
  }

  private static void addMilestone(MongoDatabase db, App.Session me, Map<String, String> body) throws Exception {
    Document project = db.getCollection("projects").find(eq("studentId", me.id())).first();
    if (project == null) throw new Exception("You need an approved project before submitting a milestone");
    db.getCollection("milestones").insertOne(new Document("_id", Database.nextId("milestones")).append("projectId", id(project))
        .append("title", body.get("title")).append("submissionDate", new Date()).append("status", "SUBMITTED").append("facultyRemark", ""));
  }

  private static void postAnnouncement(MongoDatabase db, App.Session me, Map<String, String> body) {
    db.getCollection("announcements").insertOne(new Document("_id", Database.nextId("announcements")).append("authorId", me.id())
        .append("title", body.get("title")).append("body", body.get("body")).append("createdAt", new Date()));
  }

  private static void addUser(MongoDatabase db, Map<String, String> body) throws Exception {
    String role = body.get("role");
    if (!Set.of("STUDENT", "FACULTY").contains(role)) throw new Exception("Invalid user role");
    int userId = Database.nextId("users");
    db.getCollection("users").insertOne(new Document("_id", userId).append("name", body.get("name")).append("email", body.get("email"))
        .append("passwordHash", Passwords.hash(body.get("password"))).append("role", role));
    if (role.equals("FACULTY")) db.getCollection("faculty").insertOne(new Document("_id", userId).append("department", body.get("department"))
        .append("maxStudents", number(body, "capacity")).append("currentStudents", 0).append("domainIds", new ArrayList<Integer>()));
    else db.getCollection("students").insertOne(new Document("_id", userId).append("registrationNo", body.get("registration"))
        .append("department", body.get("department")).append("semester", number(body, "semester")));
  }

  private static void assignDomain(MongoDatabase db, Map<String, String> body) {
    db.getCollection("faculty").updateOne(eq("_id", number(body, "facultyId")), addToSet("domainIds", number(body, "domainId")));
  }

  private static String facultyRows(MongoDatabase db) {
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Document faculty : db.getCollection("faculty").find()) {
      Document user = byId(db, "users", id(faculty));
      List<String> domains = new ArrayList<>();
      for (Object domainId : faculty.getList("domainIds", Object.class, List.of())) { Document domain = byId(db, "domains", ((Number) domainId).intValue()); if (domain != null) domains.add(domain.getString("name")); }
      rows.add(row("id", id(faculty), "name", user.getString("name"), "department", faculty.getString("department"), "max_students", id(faculty, "maxStudents"), "current_students", id(faculty, "currentStudents"), "seats_left", id(faculty, "maxStudents") - id(faculty, "currentStudents"), "domains", String.join(", ", domains)));
    }
    return rows(rows);
  }

  private static String domainRows(MongoDatabase db) {
    List<Map<String, Object>> rows = new ArrayList<>(); for (Document d : db.getCollection("domains").find()) rows.add(row("id", id(d), "name", d.getString("name"))); return rows(rows);
  }

  private static String announcementRows(MongoDatabase db) {
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Document a : db.getCollection("announcements").find().sort(Sorts.descending("createdAt"))) { Document author = byId(db, "users", id(a, "authorId")); rows.add(row("id", id(a), "title", a.getString("title"), "body", a.getString("body"), "created_at", date(a, "createdAt"), "author", author == null ? "Unknown" : author.getString("name"))); }
    return rows(rows);
  }

  private static String requestRows(MongoDatabase db, int accountId, boolean studentView) {
    String filterName = studentView ? "studentId" : "facultyId"; List<Map<String, Object>> rows = new ArrayList<>();
    for (Document r : db.getCollection("guideRequests").find(eq(filterName, accountId)).sort(Sorts.descending("requestDate"))) {
      Document other = byId(db, "users", id(r, studentView ? "facultyId" : "studentId")); Document domain = byId(db, "domains", id(r, "domainId"));
      if (studentView) rows.add(row("id", id(r), "faculty", other.getString("name"), "domain", domain.getString("name"), "status", r.getString("status"), "date", date(r, "requestDate")));
      else { Document student = byId(db, "students", id(r, "studentId")); rows.add(row("id", id(r), "student", other.getString("name"), "registration_no", student.getString("registrationNo"), "domain", domain.getString("name"), "status", r.getString("status"), "date", date(r, "requestDate"))); }
    }
    return rows(rows);
  }

  private static String projectRows(MongoDatabase db, App.Session me) {
    Bson filter = me.role().equals("STUDENT") ? eq("studentId", me.id()) : me.role().equals("FACULTY") ? eq("facultyId", me.id()) : new Document(); List<Map<String, Object>> rows = new ArrayList<>();
    for (Document p : db.getCollection("projects").find(filter)) { Document student = byId(db, "users", id(p, "studentId")); Document faculty = byId(db, "users", id(p, "facultyId")); Document domain = byId(db, "domains", id(p, "domainId")); rows.add(row("id", id(p), "title", p.getString("title"), "description", p.getString("description"), "status", p.getString("status"), "student", student.getString("name"), "faculty", faculty.getString("name"), "domain", domain.getString("name"))); }
    return rows(rows);
  }

  private static String reportRows(MongoDatabase db) {
    List<Map<String, Object>> rows = new ArrayList<>(); for (Document f : db.getCollection("faculty").find()) { Document user = byId(db, "users", id(f)); rows.add(row("faculty", user.getString("name"), "allocated_students", db.getCollection("projects").countDocuments(eq("facultyId", id(f))))); } return rows(rows);
  }

  private static Document byId(MongoDatabase db, String collection, int id) { return db.getCollection(collection).find(eq("_id", id)).first(); }
  private static int id(Document doc) { return ((Number) doc.get("_id")).intValue(); }
  private static int id(Document doc, String key) { return ((Number) doc.get(key)).intValue(); }
  private static int number(Map<String, String> input, String key) { try { return Integer.parseInt(input.get(key)); } catch (Exception e) { throw new IllegalArgumentException("Invalid " + key); } }
  private static String date(Document doc, String key) { Date value = doc.getDate(key); return value == null ? "" : Instant.ofEpochMilli(value.getTime()).toString(); }
  private static String message(String value) { return object("message", value); }
  private static Map<String, Object> row(Object... pairs) { Map<String, Object> row = new LinkedHashMap<>(); for (int i = 0; i < pairs.length; i += 2) row.put((String) pairs[i], pairs[i + 1]); return row; }
  private static String object(Object... pairs) { return rows(List.of(row(pairs))).replaceFirst("^\\[", "").replaceFirst("\\]$", ""); }
  private static String rows(List<Map<String, Object>> values) { StringBuilder out = new StringBuilder("["); for (Map<String, Object> value : values) { if (out.length() > 1) out.append(','); out.append('{'); boolean first = true; for (Map.Entry<String, Object> entry : value.entrySet()) { if (!first) out.append(','); first = false; out.append('\"').append(App.esc(entry.getKey())).append("\":"); Object item = entry.getValue(); if (item instanceof Number || item instanceof Boolean) out.append(item); else out.append('\"').append(App.esc(String.valueOf(item == null ? "" : item))).append('\"'); } out.append('}'); } return out.append(']').toString(); }
}
