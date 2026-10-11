# Faculty Guide Allocation and Project Management System

A Java web application for managing faculty guide allocation, student projects, milestones, and announcements.

The system helps prevent faculty over-allocation and ensures that a student receives only one approved guide.

## Features

### Student

- Login
- View available faculty and remaining seats
- View faculty research domains
- Submit a guide request
- Check request status
- View assigned project
- Submit project milestones
- View announcements

### Faculty

- Login
- View incoming guide requests
- Accept or reject requests
- View supervised projects
- Post announcements

### Department Coordinator/Admin

- Add students and faculty
- Assign research domains to faculty
- View faculty allocation report
- Lock or unlock new guide requests
- Post announcements

## Technology Used

- Backend: Java
- Frontend: HTML, CSS, JavaScript
- Database: MongoDB Atlas / MongoDB Community Server
- Database Viewer: MongoDB Compass
- Database Driver: MongoDB Java Driver

## Project Structure
project/
│
├── database/
│   └── mongodb-collections.md
│
├── lib/
│   └── MongoDB Java driver JAR files
│
├── scripts/
│   └── setup-mongodb-driver.ps1
│
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── college/
│                   └── guide/
│                       ├── App.java
│                       ├── Database.java
│                       ├── MongoApi.java
│                       └── Passwords.java
│
├── out/
│   └── Compiled Java class files
│
└── README.md


## MongoDB Collections

The application creates the following MongoDB collections automatically on first startup:


users
students
faculty
domains
guideRequests
projects
milestones
announcements
settings
counters
`
