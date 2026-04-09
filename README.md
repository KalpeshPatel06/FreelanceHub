# FreelanceHub 🚀

A production-ready **freelancing marketplace** built with Java Spring Boot, deployed on AWS EC2 with a fully automated CI/CD pipeline using GitHub Actions.

> Clients post projects with budgets and deadlines. Freelancers browse opportunities, submit competitive bids, and clients hire the best fit — all through a clean, responsive web interface.

---

## 🌐 Live Demo

| URL | Description |
|-----|-------------|
| `http://100.48.74.127:8080` | Live application on AWS EC2 |

> **Note:** The EC2 instance used to host this application has been deleted, so the above link is no longer accessible. The application was fully functional and tested successfully prior to the deletion of the EC2 instance.
---

## ✨ Features

### Authentication & Security
- JWT-based stateless authentication
- BCrypt password hashing — passwords never stored in plain text
- Role-based access control — clients and freelancers have different permissions
- Token stored in browser, sent with every protected API request

### Client Features
- Register and login as a Client
- Post projects with title, description, budget, and deadline
- View all bids received on each project
- Accept the best bid — automatically rejects all other bids and marks project as In Progress
- Delete own projects
- Dashboard showing all posted projects with stats

### Freelancer Features
- Register and login as a Freelancer
- Browse all open projects with keyword search and pagination
- View full project details including budget and deadline
- Submit a proposal with a custom bid amount
- One bid per project enforced at database level
- Dashboard showing all submitted bids and their current status

### Profile
- View account information and activity stats
- Upload a profile photo stored in AWS S3

---

## 🛠 Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Language | Java 17 | Backend programming language |
| Framework | Spring Boot 3.2 | REST API server and dependency injection |
| Security | Spring Security + JWT | Authentication and authorization |
| Database ORM | Spring Data JPA + Hibernate | Object-relational mapping |
| Database | MySQL 8 | Persistent data storage |
| Build Tool | Maven | Dependency management and packaging |
| Frontend | HTML5, CSS3, Bootstrap 5 | Responsive user interface |
| JavaScript | Vanilla JS | API calls and dynamic page updates |
| Cloud Compute | AWS EC2 (t2.micro) | Application hosting |
| Cloud Storage | AWS S3 | Profile image storage |
| CI/CD | GitHub Actions | Automated build, test, and deploy |
| Testing | JUnit 5, Mockito, MockMvc | Unit and integration tests |
| Version Control | Git + GitHub | Source code management |

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│  Developer pushes code → GitHub → GitHub Actions triggers       │
│                                                                 │
│  CI: Build → Test → Package JAR                                 │
│  CD: Copy JAR to EC2 → Restart app → Health check               │
└──────────────────────────────┬──────────────────────────────────┘
                               │ SSH Deploy
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│  AWS EC2 Instance (Ubuntu, t2.micro — Free Tier)                │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Spring Boot Application (port 8080)                      │  │
│  │                                                           │  │
│  │  Controller → Service → Repository → MySQL 8              │  │
│  │                  │                                        │  │
│  │                  └──→ AWS S3 (profile images)             │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

Browser → Static HTML/JS/CSS (served by Spring Boot)
        → REST API calls with JWT Authorization header
```

---

## 📁 Project Structure

```
freelancehub/
├── .github/
│   └── workflows/
│       └── ci-cd.yml                  ← GitHub Actions pipeline
├── backend/
│   ├── pom.xml                        ← Maven dependencies
│   └── src/
│       ├── main/
│       │   ├── java/com/freelancehub/
│       │   │   ├── config/            ← Security + AWS S3 configuration
│       │   │   ├── controller/        ← REST API endpoints
│       │   │   │   ├── AuthController.java
│       │   │   │   ├── ProjectController.java
│       │   │   │   ├── BidController.java
│       │   │   │   └── UserController.java
│       │   │   ├── service/           ← Business logic layer
│       │   │   │   ├── AuthService.java
│       │   │   │   ├── ProjectService.java
│       │   │   │   ├── BidService.java
│       │   │   │   └── UserService.java
│       │   │   ├── repository/        ← Database access layer
│       │   │   │   ├── UserRepository.java
│       │   │   │   ├── ProjectRepository.java
│       │   │   │   └── BidRepository.java
│       │   │   ├── entity/            ← JPA database table mappings
│       │   │   │   ├── User.java
│       │   │   │   ├── Project.java
│       │   │   │   └── Bid.java
│       │   │   ├── dto/               ← Request and response data shapes
│       │   │   ├── security/          ← JWT filter and user loader
│       │   │   └── exception/         ← Global error handling
│       │   └── resources/
│       │       ├── application.properties
│       │       └── static/            ← Frontend pages
│       │           ├── index.html
│       │           ├── css/styles.css
│       │           ├── js/api.js
│       │           └── pages/
│       │               ├── login.html
│       │               ├── register.html
│       │               ├── dashboard.html
│       │               ├── projects.html
│       │               ├── project-detail.html
│       │               ├── post-project.html
│       │               └── profile.html
│       └── test/                      ← Unit and integration tests
├── scripts/
│   ├── ec2-setup.sh                   ← One-time EC2 server setup
│   └── run-local.sh                   ← Local development shortcut
└── README.md
```

---

## 🗄 Database Schema

```sql
-- Users table (clients and freelancers in one table, distinguished by role)
CREATE TABLE users (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(100)  NOT NULL,
    email             VARCHAR(150)  NOT NULL UNIQUE,
    password          VARCHAR(255)  NOT NULL,   -- BCrypt hashed
    role              ENUM('CLIENT','FREELANCER') NOT NULL,
    profile_image_url VARCHAR(500),
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Projects table
CREATE TABLE projects (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(200)  NOT NULL,
    description TEXT          NOT NULL,
    budget      DECIMAL(10,2) NOT NULL,
    deadline    DATE          NOT NULL,
    client_id   BIGINT        NOT NULL,
    status      ENUM('OPEN','IN_PROGRESS','COMPLETED','CANCELLED') DEFAULT 'OPEN',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES users(id)
);

-- Bids table (unique constraint: one bid per freelancer per project)
CREATE TABLE bids (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id    BIGINT        NOT NULL,
    freelancer_id BIGINT        NOT NULL,
    proposal      TEXT          NOT NULL,
    bid_amount    DECIMAL(10,2) NOT NULL,
    status        ENUM('PENDING','ACCEPTED','REJECTED') DEFAULT 'PENDING',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id)    REFERENCES projects(id),
    FOREIGN KEY (freelancer_id) REFERENCES users(id),
    UNIQUE KEY uq_bid_project_freelancer (project_id, freelancer_id)
);
```

> Hibernate auto-creates and manages all tables on first startup via `spring.jpa.hibernate.ddl-auto=update`

---

## 🔌 REST API Reference

### Auth — Public endpoints

| Method | Endpoint | Request Body | Description |
|--------|----------|-------------|-------------|
| POST | `/api/auth/register` | `{name, email, password, role}` | Create new account |
| POST | `/api/auth/login` | `{email, password}` | Login and receive JWT token |

### Projects

| Method | Endpoint | Auth Required | Description |
|--------|----------|--------------|-------------|
| GET | `/api/projects?page=0&size=10` | None | Browse all open projects |
| GET | `/api/projects/{id}` | None | Get project details |
| GET | `/api/projects/search?keyword=X` | None | Search projects by keyword |
| POST | `/api/projects` | CLIENT | Post a new project |
| GET | `/api/projects/my` | CLIENT | Get my posted projects |
| DELETE | `/api/projects/{id}` | CLIENT (owner) | Delete a project |

### Bids

| Method | Endpoint | Auth Required | Description |
|--------|----------|--------------|-------------|
| POST | `/api/bids/projects/{projectId}` | FREELANCER | Submit a bid |
| GET | `/api/bids/projects/{projectId}` | CLIENT (owner) | View bids on project |
| GET | `/api/bids/my` | FREELANCER | View my submitted bids |
| PUT | `/api/bids/{bidId}/accept` | CLIENT (owner) | Accept a bid |

### Users

| Method | Endpoint | Auth Required | Description |
|--------|----------|--------------|-------------|
| GET | `/api/users/me` | Any | Get my profile |
| POST | `/api/users/me/profile-image` | Any | Upload profile photo to S3 |

---

## 🚀 Running Locally

### Prerequisites

- Java 17 — download from https://adoptium.net
- Maven 3.8+ — download from https://maven.apache.org
- MySQL 8 — download from https://dev.mysql.com/downloads

### Step 1 — Clone the repository

```bash
git clone https://github.com/KalpeshPatel06/freelancehub.git
cd freelancehub
```

### Step 2 — Create the database

```bash
mysql -u root -p
```

```sql
CREATE DATABASE freelancehub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;
```

### Step 3 — Set environment variables

**Windows PowerShell:**
```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/freelancehub?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
$env:JWT_SECRET="AnyRandomStringAtLeast32CharactersLong1234"
```

**macOS / Linux:**
```bash
export DB_URL="jdbc:mysql://localhost:3306/freelancehub?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
export DB_USERNAME="root"
export DB_PASSWORD="your_mysql_password"
export JWT_SECRET="AnyRandomStringAtLeast32CharactersLong1234"
```

### Step 4 — Run the application

```bash
cd backend
mvn spring-boot:run
```

### Step 5 — Open in browser

```
http://localhost:8080
```

Hibernate automatically creates all database tables on first startup.

---

## 🧪 Running Tests

```bash
cd backend
mvn test
```

Tests use an H2 in-memory database — no MySQL setup required.

Expected output:
```
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Test coverage

| Test Class | Type | What is tested |
|-----------|------|---------------|
| `AuthServiceTest` | Unit | Registration, login, BCrypt hashing, duplicate email check |
| `ProjectServiceTest` | Unit | Role enforcement, ownership checks, project creation |
| `AuthControllerTest` | Integration | HTTP status codes, JSON responses, input validation |

---

## ☁️ AWS Deployment

### Infrastructure

| Service | Usage | Cost |
|---------|-------|------|
| EC2 t2.micro | Runs the Spring Boot application | Free tier (750 hrs/month) |
| S3 | Stores profile images | Free tier (5 GB) |
| Security Group | Controls inbound traffic on port 8080 | Free |

### EC2 Setup Steps

**1. Launch EC2 instance**
- AMI: Ubuntu Server 24.04 LTS
- Instance type: t2.micro (free tier)
- Security group: open port 22 (SSH) and port 8080 (app)

**2. Install dependencies on EC2**
```bash
sudo apt-get update -y
sudo apt-get install -y openjdk-17-jdk mysql-server
sudo systemctl start mysql
```

**3. Create database**
```bash
sudo mysql
```
```sql
CREATE DATABASE freelancehub CHARACTER SET utf8mb4;
CREATE USER 'freelancehub'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON freelancehub.* TO 'freelancehub'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

**4. Start the application**
```bash
nohup java -jar app.jar \
  "--spring.datasource.url=jdbc:mysql://localhost:3306/freelancehub" \
  "--spring.datasource.username=freelancehub" \
  "--spring.datasource.password=your_password" \
  "--app.jwt.secret=your_jwt_secret" \
  "--server.port=8080" \
  > app.log 2>&1 &
```

---

## 🔄 CI/CD Pipeline

Every push to the `main` branch triggers the automated pipeline.

```
Push to main branch
        │
        ▼
┌─────────────────────────┐
│   Job 1: Build & Test   │
│                         │
│  1. Checkout code       │
│  2. Install Java 17     │
│  3. mvn clean verify    │
│  4. Run 11 unit tests   │
│  5. Package JAR file    │
└────────────┬────────────┘
             │ Only if all tests pass
             ▼
┌─────────────────────────┐
│   Job 2: Deploy to EC2  │
│                         │
│  1. Download JAR        │
│  2. Copy JAR to EC2     │
│  3. Stop old process    │
│  4. Start new JAR       │
│  5. Health check        │
└─────────────────────────┘
```

### GitHub Secrets required

| Secret | Description |
|--------|-------------|
| `EC2_HOST` | EC2 public IP address |
| `EC2_USERNAME` | `ubuntu` |
| `EC2_SSH_KEY` | Contents of the `.pem` private key file |
| `DB_URL` | MySQL JDBC connection URL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | JWT signing secret (min 32 characters) |

---

## 💡 Key Design Decisions

**Stateless JWT Authentication**
No server-side sessions. Every request carries a signed JWT token in the `Authorization: Bearer` header. The server validates the token signature on every request without touching the database.

**DTO Separation**
Entities never leave the service layer. Separate DTO classes define exactly what data enters and leaves the API. This prevents accidentally exposing internal fields and keeps the API contract independent of the database schema.

**Global Exception Handler**
One centralized `@RestControllerAdvice` class catches all exceptions and converts them into a consistent JSON response format. Every error response has the same structure — `success`, `message`, `data`, `timestamp`.

**Role-Based Access Control**
Two layers of authorization — `@PreAuthorize` annotations on controllers check the user's role, and service-layer ownership checks verify the user owns the resource they are trying to modify.

**H2 for Tests**
Unit and integration tests use an H2 in-memory database configured via a separate `application-test.properties`. Tests run without any external database dependency, making them fast and portable across any machine.

---

## 📋 API Response Format

Every API endpoint returns the same consistent JSON structure:

**Success response:**
```json
{
  "success": true,
  "message": "Project created successfully",
  "data": { ... },
  "timestamp": "2024-01-15T10:30:00"
}
```

**Error response:**
```json
{
  "success": false,
  "message": "Project not found with id: 42",
  "timestamp": "2024-01-15T10:30:00"
}
```

**Validation error response:**
```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "email": "Please provide a valid email address",
    "password": "Password must be at least 8 characters"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

---

## 🔒 Security Implementation

- Passwords hashed with BCrypt (strength 10 — 1024 hashing rounds)
- JWT tokens signed with HMAC-SHA256
- Token expiration set to 24 hours
- CORS configured to allow frontend requests
- CSRF disabled for stateless REST API
- All sensitive configuration read from environment variables — no hardcoded secrets
- `.env` and `.pem` files excluded from Git via `.gitignore`

---

*Built with Java 17 · Spring Boot 3.2 · MySQL 8 · AWS EC2 · GitHub Actions*
