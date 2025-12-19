# AI Chat Bot with Document Processing

A Spring Boot-based AI chat application that leverages Gemini AI for natural language processing and Qdrant for document embeddings and retrieval. Now features a modern React frontend.

## Features

- **AI-Powered Chat**: Interactive chat interface powered by Google's Gemini AI
- **Modern UI**: Responsive React frontend
- **Document Ingestion**: Upload and process documents for AI context
- **Asynchronous Processing**: Background job processing for document ingestion
- **Security**: Input validation and sanitization
- **Conversation Management**: Track and manage chat conversations
- **Multiple Personalities**: Choose from different AI personalities (friendly, professional, concise, detailed)
- **Active RAG**: Smart document retrieval that evaluates relevance and asks clarifying questions if needed
    - **Retrieve**: Fetches documents from Qdrant.
    - **Grade**: Evaluates if documents answer the specific question.
    - **Clarify**: Asks follow-up questions if data is missing.
- **System & User Statistics**: View usage metrics (users, conversations, messages, documents) per user or system-wide. Tracks input/output tokens.
- **Observability**: Complete monitoring stack with Prometheus (metrics), Loki (logs), and Grafana (dashboards).

## Tech Stack

- **Backend**: Spring Boot 4.0.0 (Java 17)
- **Frontend**: React 19, Vite 7, TypeScript, TailwindCSS 4
- **AI**: Google Gemini AI, LangChain4j, LangGraph4j
- **Vector Database**: Qdrant
- **Database**: PostgreSQL
- **Caching**: Redis
- **Monitoring**: Prometheus, Loki, Promtail, Grafana
- **Build Tool**: Maven

## Embedding Models

The application supports multiple embedding model providers (Hugging Face, Google AI) for document vectorization and retrieval. See [docs/EMBEDDING_MODELS.md](docs/EMBEDDING_MODELS.md) for detailed configuration options and how to switch between providers.

## Prerequisites

- **Docker & Docker Compose** (Recommended for easy setup)
- Java 17+ (For local backend dev)
- Node.js 20+ (For local frontend dev)
- Google Cloud account with Gemini API key

## Quick Start (Docker)

1.  **Clone the repository**
    ```bash
    git clone https://github.com/pershai/ai-chat-bot.git
    cd ai-chat-bot
    ```

2.  **Configure Environment**
    Create a `.env` file in the root directory:
    ```env
    GOOGLE_API_KEY=your_gemini_key_here
    POSTGRES_DB=chatbot
    POSTGRES_USER=postgres
    POSTGRES_PASSWORD=password
    ```

3.  **Run with Docker Compose**
    ```bash
    docker-compose up -d --build
    ```
    - Frontend: http://localhost:3000
    - Backend API: http://localhost:8080
    - API Documentation (Swagger): http://localhost:8080/swagger-ui.html
    - Qdrant Dashboard: http://localhost:6333/dashboard
    - Grafana (Dashboards & Logs): http://localhost:3001 (admin/admin)
    - Prometheus (Metrics): http://localhost:9090
    - Loki (Log Aggregation): http://localhost:3100

### API Usage Notes

#### Document Ingestion
- **Endpoint**: `POST /api/v1/documents/ingest`
- **Method**: `POST`
- **Content-Type**: `multipart/form-data`
- **Files Parameter**: Use the key **`files`** (the error "No files provided" indicates an incorrect key).
- **Security**: Requires a valid JWT token in the `Authorization: Bearer <token>` header. The `userId` is derived automatically from the token.

## Development

### Backend
```bash
mvn clean install
mvn spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

## Testing
Run backend tests:
```bash
mvn test
```