# VisionVault

A face-recognition-based photo retrieval system. Upload a collection of photos, index the faces in them, then scan your face with a webcam (or upload a selfie) to instantly find every photo you appear in.

## How It Works

1. **Upload** — Add photos to the collection via drag-and-drop or file picker.
2. **Index** — The system scans every uploaded photo, detects faces, and generates a numerical embedding (face code) for each one using a pretrained deep-learning model.
3. **Search** — Capture a selfie or upload one. The system encodes your face the same way and compares it against the stored embeddings to find matches.

## Architecture

```
Frontend (HTML/CSS/JS)  ⟷  Backend (Spring Boot / Java)  ⟷  Face Brain (DJL)
```

| Layer | Responsibility |
|---|---|
| **Frontend** | Upload page with drag-and-drop, webcam capture, results gallery with lightbox |
| **Backend** | REST API for uploads, photo storage, thumbnail generation, indexing pipeline, face search |
| **Face Brain** | Face detection, embedding extraction, and similarity matching via DJL (Deep Java Library) |

## API Endpoints

The frontend communicates with the backend through these endpoints:

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/upload` | Upload images (multipart, field name `files`) |
| `POST` | `/api/index/start` | Begin indexing uploaded photos for faces |
| `GET` | `/api/index/progress` | Polling endpoint — returns `{ done, total }` or a text string like `"4000 of 10000 photos done"` |
| `POST` | `/api/search` | Upload a selfie (field name `image`) and receive matching photos as JSON |
| `GET` | `/api/photos/:id/thumbnail` | Retrieve a photo thumbnail |
| `GET` | `/api/photos/:id/image` | Retrieve the full-resolution photo |


### Features

- **Upload tab** — Drag-and-drop zone with batch uploads, file previews, and a progress bar.
- **Scan tab** — Webcam viewfinder with capture, retake, and a fallback file picker for devices without a camera.
- **Results tab** — Responsive photo gallery with a lightbox viewer, keyboard navigation, and a match count badge.
- Fully accessible with ARIA roles, keyboard support, and reduced-motion media queries.


## Tech Stack

- **Frontend** — HTML5, CSS3, vanilla JavaScript
- **Backend** — Java, Spring Boot
- **ML** — DJL (Deep Java Library) with a pretrained face detection & recognition model
- **Database** — Stores photo metadata and face embeddings

## Project Structure

```
VisionVault/
├── Frontend/
│   ├── index.html      # Single-page app shell
│   ├── style.css       # All styles
│   └── app.js          # Client-side logic
└── README.md
```

## Privacy

- All photos are stored on the server they are uploaded to — nothing is sent to third-party services.
- Only upload photos of people who have given their consent.

