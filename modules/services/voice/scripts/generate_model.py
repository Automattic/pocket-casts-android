"""
Generates a TFLite speaker embedding model.
Takes variable-length 16kHz mono audio, outputs 192-dim L2-normalized embedding.
Uses Conv1D + GlobalAveragePooling1D + Dense for length independence.

Usage: python generate_model.py --output ../src/main/assets/speaker_embed.tflite

Requirements: tensorflow >= 2.13
"""

import argparse
import os
import numpy as np
import tensorflow as tf
from tensorflow.keras import layers


def make_model():
    inp = tf.keras.Input(shape=(None,), dtype=tf.float32, name="input")
    x = layers.Reshape((-1, 1))(inp)  # (batch, time, 1)
    x = layers.Conv1D(16, 5, padding="same", activation="relu")(x)
    x = layers.Conv1D(32, 3, padding="same", activation="relu")(x)
    x = layers.GlobalAveragePooling1D()(x)
    x = layers.Dense(192)(x)
    out = layers.Lambda(
        lambda v: v / tf.sqrt(tf.reduce_sum(v * v, axis=-1, keepdims=True) + 1e-10),
        name="embedding",
    )(x)
    return tf.keras.Model(inputs=inp, outputs=out)


def export_tflite(model, output_path):
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS]
    tflite_model = converter.convert()
    with open(output_path, "wb") as f:
        f.write(tflite_model)
    print(f"Model saved to {output_path}")
    print(f"Size: {len(tflite_model) // 1024} KB")


def verify_model(model_path):
    interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()
    details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    print(f"Input shape: {details[0]['shape']}, Output shape: {output_details[0]['shape']}")

    for length in [16000, 48000]:
        audio = (
            np.sin(np.arange(length) * 2 * np.pi * 440 / 16000).astype(np.float32) * 0.5
        )
        interpreter.resize_tensor_input(details[0]["index"], [1, length])
        interpreter.allocate_tensors()
        interpreter.set_tensor(details[0]["index"], audio[np.newaxis, :])
        interpreter.invoke()
        emb = interpreter.get_tensor(output_details[0]["index"])
        assert emb.shape == (1, 192), f"Expected (1, 192), got {emb.shape}"
        norm = np.linalg.norm(emb[0])
        assert abs(norm - 1.0) < 0.01, f"Expected norm ~1.0, got {norm}"
        print(f"  len={length}: norm={norm:.4f}")
    print("All checks passed.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    os.makedirs(os.path.dirname(args.output), exist_ok=True)
    export_tflite(make_model(), args.output)
    verify_model(args.output)
