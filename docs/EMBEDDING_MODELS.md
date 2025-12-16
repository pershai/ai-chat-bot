# Embedding Model Configuration

The AI Chat Bot supports multiple embedding model providers that can be configured via `application.yml` or environment variables.

## Supported Providers

### 1. Hugging Face (Default)

Use any embedding model from the Hugging Face Hub via their Inference API.

**Configuration:**

```yaml
langchain4j:
  embedding:
    provider: huggingface
    huggingface:
      api-key: ${HUGGINGFACE_API_KEY}
      model-name: sentence-transformers/all-MiniLM-L6-v2
      timeout: 30
```

**Environment Variables:**

```bash
HUGGINGFACE_API_KEY=hf_xxxxxxxxxxxxx
EMBEDDING_MODEL=sentence-transformers/all-MiniLM-L6-v2  # Optional, defaults to all-MiniLM-L6-v2
```

**Popular Models:**

- `sentence-transformers/all-MiniLM-L6-v2` (Default, 384 dimensions, fast)
- `sentence-transformers/all-mpnet-base-v2` (768 dimensions, better quality)
- `BAAI/bge-small-en-v1.5` (384 dimensions, optimized for retrieval)
- `BAAI/bge-base-en-v1.5` (768 dimensions, better quality)
- `intfloat/e5-small-v2` (384 dimensions)
- `intfloat/e5-base-v2` (768 dimensions)

**Get API Key:**
1. Go to https://huggingface.co/settings/tokens
2. Create a new token with "Read" permissions
3. Set it as `HUGGINGFACE_API_KEY` environment variable

---

### 2. Google AI (Gemini)

Use Google's text-embedding-004 model (same provider as your LLM).

**Configuration:**

```yaml
langchain4j:
  embedding:
    provider: google
    google:
      model-name: text-embedding-004
```

**Environment Variables:**

```bash
# Uses the same GOOGLE_API_KEY as your Gemini chat model
GOOGLE_API_KEY=your_google_api_key
```

**Model Details:**
- Model: `text-embedding-004`
- Dimensions: 768
- Optimized for semantic similarity and retrieval

---

## Switching Between Providers

### Option 1: Via application.yml

Edit `src/main/resources/application.yml`:

```yaml
langchain4j:
  embedding:
    provider: huggingface  # or "google"
```

### Option 2: Via Environment Variable

```bash
# Use Hugging Face
export EMBEDDING_PROVIDER=huggingface

# Use Google
export EMBEDDING_PROVIDER=google
```

### Option 3: Via Docker Compose

Edit `docker-compose.yml`:

```yaml
environment:
  - EMBEDDING_PROVIDER=huggingface
  - HUGGINGFACE_API_KEY=hf_xxxxxxxxxxxxx
  - EMBEDDING_MODEL=BAAI/bge-base-en-v1.5
```

---

## Important Notes

### ⚠️ Vector Dimension Compatibility

**CRITICAL:** If you change the embedding model, you **MUST** recreate your Qdrant collection because different models produce different vector dimensions.

**Steps to change models:**

1. Stop your application
2. Delete the Qdrant collection:
   ```bash
   # Using Qdrant API
   curl -X DELETE http://localhost:6333/collections/documents
   
   # Or delete Qdrant data volume
   docker-compose down -v
   ```
3. Update your embedding configuration
4. Restart the application (collection will be recreated automatically)
5. Re-upload your documents

### 📊 Model Comparison

| Model | Dimensions | Speed | Quality | Use Case |
|-------|-----------|-------|---------|----------|
| all-MiniLM-L6-v2 | 384 | ⚡⚡⚡ | ⭐⭐ | Fast, general purpose |
| all-mpnet-base-v2 | 768 | ⚡⚡ | ⭐⭐⭐ | Better quality |
| bge-small-en-v1.5 | 384 | ⚡⚡⚡ | ⭐⭐⭐ | Optimized for RAG |
| bge-base-en-v1.5 | 768 | ⚡⚡ | ⭐⭐⭐⭐ | Best quality RAG |
| text-embedding-004 | 768 | ⚡⚡ | ⭐⭐⭐⭐ | Google, consistent with Gemini |

### 💰 Cost Considerations

- **Hugging Face**: Free tier available, pay-per-use for higher limits
- **Google AI**: Pay-per-use, pricing at https://ai.google.dev/pricing

---

## Example Configurations

### Development (Fast & Free)

```yaml
langchain4j:
  embedding:
    provider: huggingface
    huggingface:
      api-key: ${HUGGINGFACE_API_KEY}
      model-name: sentence-transformers/all-MiniLM-L6-v2
```

### Production (High Quality)

```yaml
langchain4j:
  embedding:
    provider: huggingface
    huggingface:
      api-key: ${HUGGINGFACE_API_KEY}
      model-name: BAAI/bge-base-en-v1.5
```

### All Google Stack

```yaml
langchain4j:
  embedding:
    provider: google
    google:
      model-name: text-embedding-004
```

---

## Troubleshooting

### "HUGGINGFACE_API_KEY is required"

Set your Hugging Face API key:
```bash
export HUGGINGFACE_API_KEY=hf_xxxxxxxxxxxxx
```

### "Dimension mismatch" error

You changed embedding models without recreating the Qdrant collection. Follow the steps in "Vector Dimension Compatibility" above.

### Slow embedding generation

Consider using a smaller model (384 dimensions) or switch to a faster provider.
