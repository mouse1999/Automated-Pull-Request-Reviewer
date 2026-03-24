# 🚀 AI-Powered Automated PR Reviewer

[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0-blue?style=flat-square&logo=spring)](https://spring.io/projects/spring-ai)

### **The "First Responder" for Your Pull Requests.**

An automated code review engine built as a GitHub App that uses Generative AI to perform preliminary audits on code changes. It identifies security vulnerabilities, performance bottlenecks, and style inconsistencies before a human reviewer even touches the code.

---

## 🌟 Why This Exists

Manual code reviews are a bottleneck. This tool acts as a **pre-check layer**, catching common pitfalls and technical debt instantly. This allows senior engineers to focus on high-level architectural decisions rather than repetitive syntax or security "sanity checks."

---

## 🏗️ High-Level Architecture

The system follows an **event-driven, reactive architecture** to handle high-velocity code pushes without blocking threads.

1. **Ingestion:** Listens for GitHub Webhooks (`opened`, `synchronize`).
2. **Security:** Validates payload integrity using **HMAC SHA-256** signatures.
3. **Orchestration:** Fetches PR diffs and chunks them for analysis.
4. **Intelligence:** Uses **Spring AI** to process diffs against a curated `CodeReviewComment` schema.
5. **Feedback:** Posts structured, line-by-line comments back to the GitHub PR UI.

---

## 🛠️ Technical Stack

* **Backend:** Java 21, Spring Boot 3.4
* **AI Orchestration:** Spring AI (OpenAI / Anthropic / Ollama)
* **Concurrency:** Project Reactor (WebFlux) for non-blocking I/O
* **API:** GitHub REST API (v3)
* **Security:** HMAC Signature Verification & Secret Management

---

## 🚀 Key Engineering Highlights

### **1. Reactive AI Pipelines**

Instead of waiting for one file to be reviewed before starting the next, this app uses `Flux` to process multiple file diffs concurrently.

```java
public Flux<CodeReviewComment> processFiles(List<ChangedFile> files) {
    return Flux.fromIterable(files)
               .flatMap(file -> aiService.analyze(file))
               .subscribeOn(Schedulers.boundedElastic());
}
```

### **2. Structured Output & Guardrails**

The system forces the LLM to return a strict JSON schema (using Java Records), ensuring the feedback is always machine-readable and actionable.

### **3. Webhook Security**

Implemented robust HMAC validation to ensure that only legitimate requests from GitHub are processed, preventing unauthorized API usage.

---

## 🚦 Getting Started

### **Prerequisites**

* JDK 21+
* Maven 3.9+
* A GitHub App (Client ID, Secret, and Private Key)
* An OpenAI API Key (or local Ollama instance)

### **Configuration**

Create an `application.yml` or set environment variables:

```yaml
github:
  webhook-secret: ${GH_WEBHOOK_SECRET}
  app-token: ${GH_APP_TOKEN}
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
```

### 📋 **Setup & Installation**

### 1. Clone and Configure

```bash
git clone https://github.com/mouse1999/nexus-agentic-commerce.git
cd nexus-agentic-commerce
```

### 2. Generate Your Webhook Secret

You need a secure, random 40-character hexadecimal string to sign your webhook payloads. Choose the command based on your Operating System:

#### **🌐 macOS / Linux / Windows (with Git Bash)**

If you have Git installed on Windows, open **Git Bash** and run:

```bash
openssl rand -hex 20
```

#### **🪟 Windows (PowerShell)**

If you prefer using the native Windows PowerShell, run this command:

```powershell
-join ((65..90) + (97..122) + (48..57) | Get-Random -Count 40 | % {[char]$_})
```

*Alternatively, you can use a secure online generator like [1Password's Password Generator](https://1password.com/password-generator/) (set to 40 characters, letters and numbers).*

#### **🐍 Python (Cross-Platform)**

If you have Python installed, this works on any OS:

```bash
python -c "import secrets; print(secrets.token_hex(20))"
```


> **Important:** Save this generated string! You will need to paste it into the **Secret** field in your GitHub Webhook settings and add it to your `.env` file as `GITHUB_WEBHOOK_SECRET`.

### Why this matters

Using a unique secret ensures that your Spring Boot application only processes requests that actually come from GitHub. This prevents "Man-in-the-Middle" attacks where someone might try to send fake PR data to your AI agent.

**Important:** Copy this value; you will need it for both your `.env` file and your GitHub settings.

### 3. Environment Variables

Create a `.env` file in the root directory:

```env
GITHUB_WEBHOOK_SECRET=your_generated_hex_here
OPENAI_API_KEY=your_openai_key
GITHUB_TOKEN=your_personal_access_token
```

### 4. Run the Application

```bash
./mvnw spring-boot:run
```

---

## 🌐 Connecting GitHub to Your Local App

GitHub cannot "see" your `localhost:8080`. You must use **Ngrok** to create a public tunnel.

### Step 1: Start Ngrok

Open a new terminal and run:

```bash
ngrok http 8080
```

Copy the **Forwarding URL** (e.g., `https://a1b2-c3d4.ngrok-free.app`).

### Step 2: Configure GitHub Webhook

1. Go to your GitHub Repository -> **Settings** -> **Webhooks**.
2. Click **Add webhook**.
3. **Payload URL:** Paste your Ngrok URL and append your endpoint:
   `https://your-ngrok-url.app/api/v1/github`
4. **Content type:** Select `application/json`.
5. **Secret:** Paste the 40-character hex string you generated with `openssl`.
6. **Which events?** Select **Let me select individual events** and check **Pull requests**.
7. Click **Add webhook**.

---

## 🔍 How the Parser Works

The system includes a specialized `DiffParser` that converts Git patches into `ChangedFile` objects.


| Field          | Description                                            |
| :------------- | :----------------------------------------------------- |
| **filename**   | The path of the modified file.                         |
| **validLines** | List of line numbers GitHub allows comments on.        |
| **language**   | Detected language (Java, Python, etc.) for AI context. |

---

## 🗺️ Roadmap & Future Enhancements

The vision for **Nexus Agentic** is to move beyond simple code comments toward a fully autonomous "AI Teammate." The following features are planned for future releases:

### 🛠️ Phase 1: Technical Core (Next Steps)
* **Multi-Model Support:** Integrate **Ollama** for local LLM inference and **Google Gemini** via Spring AI to compare review quality across different models.
* **Checkstyle & Linting Integration:** Combine static analysis (Checkstyle/SonarQube) with AI to catch "easy" formatting wins before sending the code to the LLM.
* **Persistent Memory:** Use a **Vector Database (PGVector)** to store previous PR reviews, allowing the AI to "remember" recurring architectural patterns in your project.

### 🤖 Phase 2: Agentic Capabilities
* **Auto-Fix Suggestions:** Move from "comments only" to providing **GitHub Suggestions** (the "Commit Suggestion" blocks) that users can apply with a single click.
* **Automated Testing:** Teach the agent to write a **JUnit 5** or **Mockito** test case for any new logic it detects in a `ChangedFile`.
* **Contextual RAG:** Implement Retrieval-Augmented Generation to allow the AI to "read" other files in the repository for better context (e.g., checking if a new method call matches an existing interface).

### 📈 Phase 3: Ecosystem & Scale
* **Kafka Event Streaming:** Fully migrate the webhook processing to an event-driven architecture. The Webhook Controller will produce to a `github-events` Kafka topic, allowing multiple consumer instances to process reviews in parallel.
* **Interactive Telegram Bot:** Integrate with a Telegram bot (using your existing data-reselling bot logic) to notify you instantly when a review is complete or if a build fails.

---
