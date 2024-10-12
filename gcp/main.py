import tensorflow as tf
import numpy as np
from google.cloud import storage
from PIL import Image


model = None

class_names = ["Busuk Awal", "Sehat", "Busuk"]

BUCKET_NAME = "drewjd27-cnn-models"


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

    image = request.files["file"]

    image = np.array(
        Image.open(image).convert("RGB").resize((256, 256))
    )

    img_array = tf.expand_dims(image, 0)
    detections = model.predict(img_array)

    print("Detections:",detections)

    detected_class = class_names[np.argmax(detections[0])]
    confidence = round(100 * (np.max(detections[0])), 2)

    return {"class": detected_class, "confidence": confidence}
