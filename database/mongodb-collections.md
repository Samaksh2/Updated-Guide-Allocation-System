# MongoDB collection design

The Java application creates the `faculty_guide_db` database and these collections automatically on its first start:

- `users`: `_id`, `name`, `email`, `passwordHash`, `role`
- `students`: `_id` (same value as the user's ID), `registrationNo`, `department`, `semester`
- `faculty`: `_id` (same value as the user's ID), `department`, `maxStudents`, `currentStudents`, `domainIds`
- `domains`: `_id`, `name`
- `guideRequests`: `_id`, `studentId`, `facultyId`, `domainId`, `status`, `requestDate`
- `projects`: `_id`, `studentId`, `facultyId`, `domainId`, `title`, `description`, `status`, `createdAt`
- `milestones`: `_id`, `projectId`, `title`, `submissionDate`, `status`, `facultyRemark`
- `announcements`: `_id`, `authorId`, `title`, `body`, `createdAt`
- `settings`: one document storing whether allocations are locked
- `counters`: document-ID counters used by the Java application

MongoDB does not enforce foreign keys, triggers, or stored procedures. The Java backend checks the guide-allocation rules before creating or changing documents.
