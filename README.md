# Faculty Guide Allocation and Project Management System

A Java + MongoDB web application for guide allocation, project tracking, milestones, announcements, and coordinator reports. MongoDB Compass is used to view the database documents visually.

## First-time setup on Windows

1. Install **MongoDB Community Server** from https://www.mongodb.com/try/download/community. Keep the standard local server port, `27017`.
2. Install **MongoDB Compass** from https://www.mongodb.com/try/download/compass if it was not installed with the server.
3. Open Compass and connect to `mongodb://localhost:27017`. Do not create collections manually: the Java application creates and seeds them on its first start.
4. In PowerShell in this project folder, run the driver setup script once:

```powershell
.\scripts\setup-mongodb-driver.ps1
```

If PowerShell blocks local scripts, use this one-time command instead:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\scripts\setup-mongodb-driver.ps1
```

## Run

```powershell
New-Item -ItemType Directory -Force out
javac -cp "lib/*" -d out src\main\java\com\college\guide\*.java
java -cp "out;lib/*" com.college.guide.App
```

Open http://localhost:8080. The starter documents are created automatically. Use `student1@college.edu`, `faculty1@college.edu`, or `admin@college.edu`; password: `password`.

Optional environment variables:

```powershell
$env:MONGODB_URI="mongodb://localhost:27017"
$env:MONGODB_DATABASE="faculty_guide_db"
```

The earlier PostgreSQL driver is no longer required by this MongoDB version.
