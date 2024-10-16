import tensorflow as tf
import numpy as np
from google.cloud import storage
from PIL import Image

model = None
class_names = ["Busuk Awal", "Sehat", "Busuk"]
BUCKET_NAME = "drewjd27-cnn-models"
MAX_FILE_SIZE = 1.2 * 1024 * 1024  # Maximal ukuran file 1.2MB

# Daftar format gambar atau file yang diterima
ALLOWED_EXTENSIONS = {'jpg', 'jpeg', 'tiff', 'png'}

def allowed_file(filename):
    # Cek apakah file memiliki ekstensi yang diizinkan
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def download_blob(bucket_name, source_blob_name, destination_file_name):
    storage_client = storage.Client()
    bucket = storage_client.get_bucket(bucket_name)
    blob = bucket.blob(source_blob_name)

    blob.download_to_filename(destination_file_name)
    print(f"Blob {source_blob_name} downloaded to {destination_file_name}.")

def detect(request):
    global model
    if model is None:
        download_blob(
            BUCKET_NAME,
            "models/model_e.keras",
            "/tmp/model_e.keras",
        )
        model = tf.keras.models.load_model("/tmp/model_e.keras")

    # Try to get the file from request
    if 'file' not in request.files:
        return {
            "error": True,
            "message": "tidak ada file part pada request"  # Pesan error
        }, 400

    file = request.files['file']

    # Check file size
    file.seek(0, 2)  # Pindah ke akhir file
    file_length = file.tell()
    file.seek(0)  # Reset pointer file
    if file_length > MAX_FILE_SIZE:
        return {
            "error": True,
            "message": "Ukuran file terlalu besar. Maksimal 1.2MB."
        }, 400

    # Check if the file is selected and has an allowed extension
    if file.filename == '':
        return {
            "error": True,
            "message": "Tidak ada file untuk diupload"  # Pesan error
        }, 400
    if not allowed_file(file.filename):
        return {
            "error": True,
            "message": "Upload file dengan format JPG, JPEG, TIFF, atau PNG saja."  # Pesan error
        }, 400

    try:
        # Try to open the file as an image
        image = Image.open(file).convert("RGB").resize((256, 256))
        image = np.array(image)
    except Exception as e:
        return {
            "error": True,
            "message": f"Error saat memproses gambar: {str(e)}"  # Pesan error
        }, 400

    # Prepare image for model
    img_array = tf.expand_dims(image, 0)

    # Run model prediction
    try:
        detections = model.predict(img_array)
        detected_class = class_names[np.argmax(detections[0])]
    except Exception as e:
        return {
            "error": True,
            "message": f"Error saat mengklasifikasi: {str(e)}"  # Pesan error
        }, 500

    # Return response tanpa error
    return {
        "error": False,
        "message": detected_class  # Ditampilkan setelahnya
    }, 200
