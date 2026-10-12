/* =====================================================================
   Photo Detective - Frontend (plain HTML + CSS + JavaScript)

   Backend endpoints this file talks to (Member 2's Spring Boot app):
     POST /api/upload          form field "files" (many images)
     POST /api/index/start     starts indexing
     GET  /api/index/progress  "4000 of 10000 photos done"  (or JSON {done,total})
     POST /api/search          form field "image" (selfie) -> JSON list:
                               [{ "id": 1, "fileName": "a.jpg",
                                  "thumbnailUrl": "/api/photos/1/thumbnail",
                                  "imageUrl": "/api/photos/1/image" }, ...]
   ===================================================================== */

// Leave empty when these files are served by the same Spring Boot app.
// If the frontend runs somewhere else, use e.g. "http://localhost:8080".
const API_BASE = "";

const BATCH_SIZE = 25;   // photos sent per upload request

/* ---------------------------------------------------------------------
   Small helpers
   --------------------------------------------------------------------- */
const $ = (id) => document.getElementById(id);

function apiUrl(path) {
  if (!path) return "";
  if (/^https?:\/\//i.test(path)) return path;   // already a full URL
  return API_BASE + path;
}

let statusTimer = null;
function showStatus(message, type = "info", keepMs = 6000) {
  const box = $("status");
  box.textContent = message;
  box.className = "status" + (type === "error" ? " is-error" : type === "ok" ? " is-ok" : "");
  box.hidden = false;
  clearTimeout(statusTimer);
  if (keepMs > 0) statusTimer = setTimeout(() => { box.hidden = true; }, keepMs);
}

async function readError(response) {
  try {
    const text = await response.text();
    return text ? text.slice(0, 200) : "Server returned " + response.status;
  } catch {
    return "Server returned " + response.status;
  }
}

function formatSize(bytes) {
  if (bytes < 1024 * 1024) return Math.round(bytes / 1024) + " KB";
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + " MB";
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + " GB";
}

/* ---------------------------------------------------------------------
   Tabs
   --------------------------------------------------------------------- */
const tabNames = ["upload", "scan", "results"];

function showTab(name) {
  tabNames.forEach((t) => {
    const active = t === name;
    $("tab-" + t).hidden = !active;
    const btn = $("btn-" + t);
    btn.classList.toggle("is-active", active);
    btn.setAttribute("aria-selected", String(active));
  });
  if (name !== "scan") stopCamera();     // switch the webcam off when leaving
}

tabNames.forEach((t) => $("btn-" + t).addEventListener("click", () => showTab(t)));

/* =====================================================================
   1. UPLOAD
   ===================================================================== */
let selectedFiles = [];
let previewUrls = [];

const dropzone  = $("dropzone");
const fileInput = $("fileInput");

dropzone.addEventListener("click", () => fileInput.click());
dropzone.addEventListener("keydown", (e) => {
  if (e.key === "Enter" || e.key === " ") { e.preventDefault(); fileInput.click(); }
});

["dragenter", "dragover"].forEach((evt) =>
  dropzone.addEventListener(evt, (e) => { e.preventDefault(); dropzone.classList.add("is-over"); })
);
["dragleave", "drop"].forEach((evt) =>
  dropzone.addEventListener(evt, (e) => { e.preventDefault(); dropzone.classList.remove("is-over"); })
);
dropzone.addEventListener("drop", (e) => addFiles(e.dataTransfer.files));
fileInput.addEventListener("change", () => { addFiles(fileInput.files); fileInput.value = ""; });

function addFiles(fileList) {
  const images = Array.from(fileList).filter((f) => f.type.startsWith("image/"));
  const skipped = fileList.length - images.length;
  selectedFiles = selectedFiles.concat(images);
  renderSelection();
  if (skipped > 0) showStatus(skipped + " file(s) skipped because they are not images.", "error");
}

function renderSelection() {
  previewUrls.forEach((u) => URL.revokeObjectURL(u));
  previewUrls = [];
  const strip = $("previewStrip");
  strip.innerHTML = "";

  if (selectedFiles.length === 0) {
    $("selection").hidden = true;
    return;
  }

  const total = selectedFiles.reduce((sum, f) => sum + f.size, 0);
  $("selectionText").textContent =
    selectedFiles.length + " photo(s) selected, " + formatSize(total);

  // Show a small preview of the first few photos only (keeps the page fast)
  selectedFiles.slice(0, 12).forEach((file) => {
    const url = URL.createObjectURL(file);
    previewUrls.push(url);
    const img = document.createElement("img");
    img.src = url;
    img.alt = "";
    strip.appendChild(img);
  });

  $("selection").hidden = false;
}

$("clearBtn").addEventListener("click", () => { selectedFiles = []; renderSelection(); });

$("uploadBtn").addEventListener("click", async () => {
  if (selectedFiles.length === 0) return;

  const btn = $("uploadBtn");
  btn.disabled = true;
  $("uploadProgress").hidden = false;
  setBar("uploadBar", "uploadText", 0, selectedFiles.length, "photos uploaded");

  let uploaded = 0;
  try {
    for (let i = 0; i < selectedFiles.length; i += BATCH_SIZE) {
      const batch = selectedFiles.slice(i, i + BATCH_SIZE);
      const form = new FormData();
      batch.forEach((f) => form.append("files", f));

      const res = await fetch(apiUrl("/api/upload"), { method: "POST", body: form });
      if (!res.ok) throw new Error(await readError(res));

      uploaded += batch.length;
      setBar("uploadBar", "uploadText", uploaded, selectedFiles.length, "photos uploaded");
    }
    showStatus(uploaded + " photos uploaded. Now click \"Start indexing\".", "ok");
    selectedFiles = [];
    renderSelection();
  } catch (err) {
    showStatus("Upload stopped after " + uploaded + " photos. " + err.message, "error", 0);
  } finally {
    btn.disabled = false;
  }
});

function setBar(barId, textId, done, total, label) {
  const bar = $(barId);
  bar.max = total > 0 ? total : 100;
  bar.value = done;
  $(textId).textContent = done.toLocaleString() + " of " + total.toLocaleString() + " " + label;
}

/* =====================================================================
   2. INDEXING + PROGRESS
   ===================================================================== */
let pollTimer = null;

$("indexBtn").addEventListener("click", async () => {
  const btn = $("indexBtn");
  btn.disabled = true;
  try {
    const res = await fetch(apiUrl("/api/index/start"), { method: "POST" });
    if (!res.ok) throw new Error(await readError(res));
    $("indexProgress").hidden = false;
    setBar("indexBar", "indexText", 0, 0, "photos done");
    showStatus("Indexing started.", "ok");
    startPolling();
  } catch (err) {
    showStatus("Could not start indexing. " + err.message, "error", 0);
    btn.disabled = false;
  }
});

function startPolling() {
  clearInterval(pollTimer);
  pollTimer = setInterval(checkProgress, 1000);
  checkProgress();
}

async function checkProgress() {
  try {
    const res = await fetch(apiUrl("/api/index/progress"));
    if (!res.ok) throw new Error(await readError(res));
    const raw = await res.text();

    const { done, total } = parseProgress(raw);
    setBar("indexBar", "indexText", done, total, "photos done");

    if (total > 0 && done >= total) {
      clearInterval(pollTimer);
      $("indexBtn").disabled = false;
      showStatus("Indexing finished. You can scan your face now.", "ok");
    }
  } catch (err) {
    clearInterval(pollTimer);
    $("indexBtn").disabled = false;
    showStatus("Lost contact with the server. " + err.message, "error", 0);
  }
}

// Accepts either JSON {"done":4000,"total":10000} or "4000 of 10000 photos done"
function parseProgress(raw) {
  try {
    const obj = JSON.parse(raw);
    if (obj && typeof obj.done === "number") return { done: obj.done, total: obj.total || 0 };
  } catch { /* not JSON, fall through */ }
  const m = raw.match(/(\d+)\s*of\s*(\d+)/i);
  return m ? { done: parseInt(m[1], 10), total: parseInt(m[2], 10) } : { done: 0, total: 0 };
}

/* =====================================================================
   3. CAMERA / SELFIE
   ===================================================================== */
let stream = null;
let selfieBlob = null;

const video   = $("video");
const canvas  = $("canvas");
const preview = $("selfiePreview");

$("camStartBtn").addEventListener("click", startCamera);
$("captureBtn").addEventListener("click", capturePhoto);
$("retakeBtn").addEventListener("click", retake);
$("searchBtn").addEventListener("click", searchByFace);

$("selfieInput").addEventListener("change", (e) => {
  const file = e.target.files[0];
  e.target.value = "";
  if (!file) return;
  stopCamera();
  setSelfie(file);
});

async function startCamera() {
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    showStatus("This browser cannot use the camera. Choose a selfie from your files instead.", "error", 0);
    return;
  }
  try {
    stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: "user", width: { ideal: 1280 }, height: { ideal: 960 } },
      audio: false
    });
    video.srcObject = stream;
    video.hidden = false;
    preview.hidden = true;
    $("camPlaceholder").hidden = true;
    $("camStartBtn").hidden = true;
    $("captureBtn").hidden = false;
    $("retakeBtn").hidden = true;
    $("searchBtn").hidden = true;
  } catch (err) {
    const denied = err.name === "NotAllowedError";
    showStatus(
      denied
        ? "Camera access was blocked. Allow the camera in your browser settings, then try again."
        : "No camera found. Choose a selfie from your files instead.",
      "error", 0
    );
  }
}

function stopCamera() {
  if (stream) {
    stream.getTracks().forEach((t) => t.stop());
    stream = null;
  }
  video.srcObject = null;
}

function capturePhoto() {
  if (!stream) return;
  canvas.width  = video.videoWidth;
  canvas.height = video.videoHeight;
  // Draw the real (un-mirrored) frame so the face model gets a normal image
  canvas.getContext("2d").drawImage(video, 0, 0, canvas.width, canvas.height);

  canvas.toBlob((blob) => {
    if (!blob) { showStatus("Could not capture the photo. Try again.", "error"); return; }
    stopCamera();
    setSelfie(blob);
  }, "image/jpeg", 0.92);
}

function setSelfie(blob) {
  selfieBlob = blob;
  if (preview.dataset.url) URL.revokeObjectURL(preview.dataset.url);
  const url = URL.createObjectURL(blob);
  preview.dataset.url = url;
  preview.src = url;

  preview.hidden = false;
  video.hidden = true;
  $("camPlaceholder").hidden = true;
  $("camStartBtn").hidden = true;
  $("captureBtn").hidden = true;
  $("retakeBtn").hidden = false;
  $("searchBtn").hidden = false;
}

function retake() {
  selfieBlob = null;
  preview.hidden = true;
  $("retakeBtn").hidden = true;
  $("searchBtn").hidden = true;
  startCamera();
}

/* =====================================================================
   4. SEARCH
   ===================================================================== */
let results = [];

async function searchByFace() {
  if (!selfieBlob) return;
  const btn = $("searchBtn");
  btn.disabled = true;
  btn.textContent = "Searching...";

  try {
    const form = new FormData();
    form.append("image", selfieBlob, "selfie.jpg");

    const res = await fetch(apiUrl("/api/search"), { method: "POST", body: form });
    if (!res.ok) throw new Error(await readError(res));

    results = await res.json();
    renderGallery();
    showTab("results");
  } catch (err) {
    showStatus("Search failed. " + err.message, "error", 0);
  } finally {
    btn.disabled = false;
    btn.textContent = "Find my photos";
  }
}

/* =====================================================================
   5. RESULTS GALLERY + LIGHTBOX
   ===================================================================== */
function thumbOf(item) { return apiUrl(item.thumbnailUrl || "/api/photos/" + item.id + "/thumbnail"); }
function fullOf(item)  { return apiUrl(item.imageUrl     || "/api/photos/" + item.id + "/image"); }

function renderGallery() {
  const gallery = $("gallery");
  gallery.innerHTML = "";

  const badge = $("resultBadge");
  badge.textContent = results.length;
  badge.hidden = results.length === 0;

  if (results.length === 0) {
    $("resultsTitle").textContent = "No matching photos";
    $("resultsEmpty").textContent =
      "The detective found no match. Try a clearer selfie, or check that the photos were indexed.";
    $("resultsEmpty").hidden = false;
    return;
  }

  $("resultsTitle").textContent = results.length + " photo" + (results.length === 1 ? "" : "s") + " with you in them";
  $("resultsEmpty").hidden = true;

  results.forEach((item, index) => {
    const card = document.createElement("button");
    card.type = "button";
    card.className = "photo";
    card.setAttribute("aria-label", "Open " + (item.fileName || "photo"));

    const img = document.createElement("img");
    img.src = thumbOf(item);
    img.alt = item.fileName || "Matching photo";
    img.loading = "lazy";

    const name = document.createElement("span");
    name.textContent = item.fileName || "";

    card.append(img, name);
    card.addEventListener("click", () => openLightbox(index));
    gallery.appendChild(card);
  });
}

const lightbox = $("lightbox");
let current = 0;

function openLightbox(index) {
  current = index;
  updateLightbox();
  lightbox.showModal();
}

function updateLightbox() {
  const item = results[current];
  $("lbImg").src = fullOf(item);
  $("lbImg").alt = item.fileName || "Matching photo";
  $("lbCaption").textContent = (item.fileName || "") + "  (" + (current + 1) + " of " + results.length + ")";
}

function step(delta) {
  current = (current + delta + results.length) % results.length;
  updateLightbox();
}

$("lbClose").addEventListener("click", () => lightbox.close());
$("lbPrev").addEventListener("click", () => step(-1));
$("lbNext").addEventListener("click", () => step(1));

// Click on the dark area around the picture closes it
lightbox.addEventListener("click", (e) => { if (e.target === lightbox) lightbox.close(); });

document.addEventListener("keydown", (e) => {
  if (!lightbox.open) return;
  if (e.key === "ArrowLeft")  step(-1);
  if (e.key === "ArrowRight") step(1);
});
