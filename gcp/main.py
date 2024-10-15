import tensorflow as tf
import numpy as np
from google.cloud import storage
from PIL import Image

model = None
class_names = ["Busuk Awal", "Sehat", "Busuk"]
BUCKET_NAME = "drewjd27-cnn-models"

# Daftar format gambar atau file yang diterima
ALLOWED_EXTENSIONS = {'jpg', 'jpeg', 'tiff'}


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
        return {"error": "No file part in the request"}, 400

    file = request.files['file']

    # Check if the file is selected and has an allowed extension
    if file.filename == '':
        return {"error": "No file selected for uploading"}, 400
    if not allowed_file(file.filename):
        return {"error": "File format not supported. Please upload a JPG, JPEG, or TIFF file."}, 400

    try:
        # Try to open the file as an image
        image = Image.open(file).convert("RGB").resize((256, 256))
        image = np.array(image)
    except Exception as e:
        return {"error": f"Error processing the image: {str(e)}"}, 400

    # Prepare image for model
    img_array = tf.expand_dims(image, 0)

    # Run model prediction
    try:
        detections = model.predict(img_array)
        print("Detections:", detections)
        detected_class = class_names[np.argmax(detections[0])]
    except Exception as e:
        return {"error": f"Error during prediction: {str(e)}"}, 500

    # Return response
    return {"class": detected_class}, 200
