# FreelanceHub 🚀

A production-structured **freelancing marketplace** built as a portfolio project demonstrating
Software Engineering, AWS Cloud, and DevOps/CI-CD skills — using only free-tier tools.

> **Clients** post projects with a budget and deadline. **Freelancers** browse, submit proposals,
> and compete for work. Clients review bids and hire the best fit.

---

## 🌐 Live Demo

| URL | Notes |
|-----|-------|
| `http://<EC2-IP>:8080` | Backend API + Frontend (served by Spring Boot) |
| `http://<EC2-IP>:8080/api/auth/register` | REST API entry point |

> *(Replace `<EC2-IP>` with your EC2 instance's public IP after deployment.)*

---

## ✨ Features

### Authentication
- JWT-based stateless authentication (no server sessions)
- BCrypt password hashing
- Two roles: **Client** and **Freelancer**
- Token stored in localStorage, sent in `Authorization: Bearer` header

### Client Features
- Post projects (title, description, budget, deadline)
- View all bids received on each project
- Accept a bid (marks winner, rejects others, sets project `IN_PROGRESS`)
- Delete projects

### Freelancer Features
- Browse all open projects with search and pagination
- View project details
- Submit bid (proposal + amount) — one bid per project
- Dashboard showing all submitted bids and their statuses

### Profile
- View profile stats (projects posted / bids submitted)
- Upload profile image to **AWS S3**

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17, Spring Boot 3.2, Spring Security |
| Database ORM | Spring Data JPA (Hibernate) |
| Authentication | JWT (jjwt), BCrypt |
| Frontend | HTML5, CSS3, Bootstrap 5, Vanilla JS |
| Database | MySQL 8 |
| Cloud | AWS EC2 (compute), AWS S3 (file storage) |
| CI/CD | GitHub Actions |
| Build | Maven |
| Testing | JUnit 5, Mockito, MockMvc, H2 in-memory |

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  Developer  →  GitHub  →  GitHub Actions (CI/CD)           │
│                                  │                          │
│                           ┌──────┴──────┐                  │
│                           │  SSH Deploy │                   │
│                           └──────┬──────┘                   │
└──────────────────────────────────┼──────────────────────────┘
                                   ▼
┌─────────────────────────────────────────────────────────────┐
│  AWS EC2 Instance (Ubuntu)                                  │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Spring Boot App (port 8080)                         │   │
│  │                                                      │   │
│  │  Controllers → Services → Repositories → MySQL       │   │
│  │                    │                                 │   │
│  │                    └──→  AWS S3 (profile images)     │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘

Browser ──→ Static HTML/JS (served by Spring Boot)
       ──→ REST API calls with JWT token
```

### Package Structure

```
backend/src/main/java/com/freelancehub/
├── FreelanceHubApplication.java     # Entry point
├── config/
│   ├── SecurityConfig.java          # Spring Security + CORS + JWT
│   └── AwsS3Config.java             # S3 client bean
├── controller/                      # REST API handlers
│   ├── AuthController.java          # POST /api/auth/register|login
│   ├── ProjectController.java       # /api/projects/**
│   ├── BidController.java           # /api/bids/**
│   └── UserController.java          # /api/users/**
├── service/                         # Business logic
│   ├── AuthService.java
│   ├── ProjectService.java
│   ├── BidService.java
│   └── UserService.java
├── repository/                      # JPA database access
│   ├── UserRepository.java
│   ├── ProjectRepository.java
│   └── BidRepository.java
├── entity/                          # JPA entities (DB tables)
│   ├── User.java
│   ├── Project.java
│   └── Bid.java
├── dto/
│   ├── request/                     # Incoming request shapes
│   └── response/                    # Outgoing response shapes
├── security/                        # JWT filter + UserDetailsService
│   ├── JwtUtil.java
│   ├── JwtAuthenticationFilter.java
│   └── CustomUserDetailsService.java
└── exception/                       # Global error handling
    ├── GlobalExceptionHandler.java
    ├── ResourceNotFoundException.java
    ├── BadRequestException.java
    └── ForbiddenException.java
```

---

## 🗄 Database Schema

```sql
-- Users table (clients and freelancers share one table)
CREATE TABLE users (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  name             VARCHAR(100)  NOT NULL,
  email            VARCHAR(150)  NOT NULL UNIQUE,
  password         VARCHAR(255)  NOT NULL,   -- BCrypt hash
  role             ENUM('CLIENT','FREELANCER') NOT NULL,
  profile_image_url VARCHAR(500),
  created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Projects table
CREATE TABLE projects (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  title       VARCHAR(200) NOT NULL,
  description TEXT         NOT NULL,
  budget      DECIMAL(10,2) NOT NULL,
  deadline    DATE          NOT NULL,
  client_id   BIGINT        NOT NULL,   -- FK → users.id
  status      ENUM('OPEN','IN_PROGRESS','COMPLETED','CANCELLED') DEFAULT 'OPEN',
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (client_id) REFERENCES users(id)
);

-- Bids table (one bid per freelancer per project enforced by unique key)
CREATE TABLE bids (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  project_id    BIGINT        NOT NULL,   -- FK → projects.id
  freelancer_id BIGINT        NOT NULL,   -- FK → users.id
  proposal      TEXT          NOT NULL,
  bid_amount    DECIMAL(10,2) NOT NULL,
  status        ENUM('PENDING','ACCEPTED','REJECTED') DEFAULT 'PENDING',
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (project_id)    REFERENCES projects(id),
  FOREIGN KEY (freelancer_id) REFERENCES users(id),
  UNIQUE KEY uq_bid_project_freelancer (project_id, freelancer_id)
);
```
*(Hibernate auto-creates these tables on first startup via `spring.jpa.hibernate.ddl-auto=update`)*

---

## 🔌 REST API Reference

### Auth (public)
| Method | URL | Body | Description |
|--------|-----|------|-------------|
| POST | `/api/auth/register` | `{name, email, password, role}` | Create account |
| POST | `/api/auth/login` | `{email, password}` | Get JWT token |

### Projects
| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| GET | `/api/projects?page=0&size=10` | None | Browse open projects |
| GET | `/api/projects/{id}` | None | Get project details |
| GET | `/api/projects/search?keyword=X` | None | Search projects |
| POST | `/api/projects` | CLIENT | Post a new project |
| GET | `/api/projects/my` | CLIENT | My projects |
| DELETE | `/api/projects/{id}` | CLIENT | Delete project |

### Bids
| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| POST | `/api/bids/projects/{projectId}` | FREELANCER | Submit a bid |
| GET | `/api/bids/projects/{projectId}` | CLIENT (owner) | View bids |
| GET | `/api/bids/my` | FREELANCER | My bids |
| PUT | `/api/bids/{bidId}/accept` | CLIENT (owner) | Accept a bid |

### Users
| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| GET | `/api/users/me` | Any | Get profile |
| POST | `/api/users/me/profile-image` | Any | Upload image to S3 |

---

## 🚀 Running Locally

### Prerequisites
- Java 17 (`java -version`)
- Maven 3.8+ (`mvn -version`)
- MySQL 8 running locally

### 1. Clone the repository
```bash
git clone https://github.com/<your-username>/freelancehub.git
cd freelancehub
```

### 2. Create the database
```sql
CREATE DATABASE freelancehub CHARACTER SET utf8mb4;
```

### 3. Set environment variables and run
```bash
# Option A: use the convenience script
chmod +x scripts/run-local.sh
./scripts/run-local.sh

# Option B: set vars manually, then run Maven
export DB_URL="jdbc:mysql://localhost:3306/freelancehub?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
export DB_USERNAME="root"
export DB_PASSWORD="your_mysql_password"
export JWT_SECRET="AnyLongSecretStringAtLeast32CharsLong"

cd backend
mvn spring-boot:run
```

### 4. Open the app
```
http://localhost:8080
```

### 5. Run tests only
```bash
cd backend
mvn test
```

---

## ☁️ AWS Deployment

### Step 1 — Launch EC2 Instance
1. Go to **AWS Console → EC2 → Launch Instance**
2. Choose **Ubuntu Server 24.04 LTS** (free tier eligible)
3. Instance type: **t2.micro** (free tier)
4. Create or select a Key Pair — download the `.pem` file
5. **Security Group** — add these inbound rules:
   - SSH (22) — from your IP only
   - Custom TCP (8080) — from Anywhere (0.0.0.0/0)
6. Launch the instance

### Step 2 — Run the Setup Script
```bash
# Connect to EC2
ssh -i your-key.pem ubuntu@<EC2-PUBLIC-IP>

# Upload and run setup script
scp -i your-key.pem scripts/ec2-setup.sh ubuntu@<EC2-IP>:~
./ec2-setup.sh
```

### Step 3 — Configure GitHub Secrets
Go to your GitHub repo → **Settings → Secrets and variables → Actions → New repository secret**

| Secret Name | Value |
|-------------|-------|
| `EC2_HOST` | Your EC2 public IP address |
| `EC2_USERNAME` | `ubuntu` |
| `EC2_SSH_KEY` | Contents of your `.pem` key file |
| `DB_URL` | `jdbc:mysql://localhost:3306/freelancehub?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` | `freelancehub` |
| `DB_PASSWORD` | Password you set during ec2-setup.sh |
| `JWT_SECRET` | A long random string (min 32 chars) |
| `AWS_ACCESS_KEY_ID` | *(optional — for S3)* |
| `AWS_SECRET_ACCESS_KEY` | *(optional — for S3)* |

### Step 4 — Push to Deploy
```bash
git add .
git commit -m "Initial deployment"
git push origin main
```

The GitHub Actions pipeline runs automatically:
1. ✅ Builds the JAR on a free GitHub runner VM
2. ✅ Runs all unit tests against H2 in-memory DB
3. ✅ Copies the JAR to EC2 via SSH
4. ✅ Restarts the application on EC2
5. ✅ Health check confirms the app is live

### View logs on EC2
```bash
ssh -i your-key.pem ubuntu@<EC2-IP>
tail -f ~/freelancehub/app.log
```

---

## 🪣 AWS S3 Setup (Optional — for profile images)

1. Go to **AWS Console → S3 → Create Bucket**
2. Name: `freelancehub-profiles` (or any unique name)
3. Region: `us-east-1` (match `AWS_REGION` env var)
4. **Object ownership**: ACLs disabled → Bucket owner enforced
5. **Block public access**: Uncheck all (for public image URLs)
   > ⚠️ For production: use pre-signed URLs instead of public buckets.
6. Create an IAM user with `AmazonS3FullAccess`, download access keys
7. Add `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` to GitHub Secrets

---

## 🔄 CI/CD Pipeline Explained

```
Push to main
     │
     ▼
┌─────────────────────────────────────┐
│  JOB 1: Build & Test (CI)           │
│                                     │
│  1. actions/checkout@v4             │
│     → clone repo onto runner VM     │
│                                     │
│  2. actions/setup-java@v4           │
│     → install Java 17 + cache Maven │
│                                     │
│  3. mvn clean verify                │
│     → compile + run all tests       │
│     → uses H2 (no real DB needed)   │
│                                     │
│  4. upload-artifact                 │
│     → save JAR for deploy job       │
└───────────────┬─────────────────────┘
                │ only if CI passes
                ▼
┌─────────────────────────────────────┐
│  JOB 2: Deploy (CD)                 │
│                                     │
│  1. download-artifact               │
│     → fetch JAR from CI job         │
│                                     │
│  2. appleboy/scp-action             │
│     → copy JAR to EC2 via SCP       │
│                                     │
│  3. appleboy/ssh-action             │
│     → SSH into EC2                  │
│     → stop old process (pkill)      │
│     → start new JAR (nohup java)    │
│                                     │
│  4. curl health check               │
│     → verify app is responding      │
└─────────────────────────────────────┘
```

---

## 🧪 Testing

Tests are in `backend/src/test/java/com/freelancehub/`:

| Test Class | Type | What it tests |
|-----------|------|--------------|
| `AuthServiceTest` | Unit | Registration, login, BCrypt hashing, duplicate email |
| `ProjectServiceTest` | Unit | Role enforcement, ownership checks |
| `AuthControllerTest` | Integration | HTTP status codes, JSON structure, validation |

Run tests:
```bash
cd backend
mvn test

# With coverage report
mvn verify
# Report at: backend/target/site/jacoco/index.html
```

---

## 📂 Project Structure

```
freelancehub/
├── .github/
│   └── workflows/
│       └── ci-cd.yml            # GitHub Actions pipeline
├── backend/
│   ├── pom.xml                  # Maven dependencies
│   └── src/
│       ├── main/
│       │   ├── java/com/freelancehub/  # All Java source code
│       │   └── resources/
│       │       ├── application.properties
│       │       └── static/      # Frontend (HTML/CSS/JS)
│       └── test/
│           ├── java/com/freelancehub/ # Unit + integration tests
│           └── resources/application-test.properties
├── scripts/
│   ├── ec2-setup.sh             # One-time EC2 provisioning
│   └── run-local.sh             # Local development shortcut
├── .gitignore
└── README.md
```

---

## 💼 Resume Talking Points

This project demonstrates:

**Software Engineering**
- Layered architecture (Controller → Service → Repository → Entity)
- DTOs to decouple API contracts from database entities
- Global exception handling with consistent JSON error responses
- Input validation with Bean Validation annotations
- Spring Security with JWT stateless authentication
- Role-based access control (`@PreAuthorize`)

**Cloud (AWS)**
- EC2 instance configuration and management
- S3 bucket for binary file storage with AWS SDK v2
- IAM security best practices (least privilege)
- Environment variables for secrets management

**DevOps / CI-CD**
- GitHub Actions pipeline triggered on push to main
- Separate CI and CD jobs with dependency chain (`needs`)
- Test isolation using H2 in-memory database
- SSH-based zero-downtime deployment
- Automated health check after deployment

---

## 📸 Screenshots

| Page | Screenshot |
|------|-----------|
| Home | *(add screenshot)* |
| Browse Projects | *(add screenshot)* |
| Project Detail + Bid | *(add screenshot)* |
| Client Dashboard | *(add screenshot)* |
| Freelancer Dashboard | *(add screenshot)* |

---

## 📄 License

MIT License — free to use, modify, and distribute.

---

*Built with ❤️ as a portfolio project — demonstrating Java, Spring Boot, AWS, and GitHub Actions.*
