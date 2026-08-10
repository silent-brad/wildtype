#!/usr/bin/env python3
"""
Train a YOLOv8n-cls model on the ViratGarg/animal_species dataset
and export to ONNX for use in Wildtype.

Loads HF_TOKEN from .env for authenticated Hugging Face access.
"""

import os
import shutil
from pathlib import Path

from huggingface_hub import snapshot_download
from ultralytics import YOLO
from PIL import Image


def load_token() -> str:
    env_path = Path(".env")
    if env_path.exists():
        for line in env_path.read_text().splitlines():
            if line.startswith("HF_TOKEN="):
                return line.split("=", 1)[1].strip()
    return os.environ.get("HF_TOKEN", "")


def prepare_dataset(root: Path = Path("datasets/animal_species_yolo")) -> Path:
    """Download full HF dataset via authenticated snapshot into YOLO classification layout."""
    token = load_token()
    print(f"Downloading dataset with token={token[:10]}..." if token else "Downloading dataset (no token)")

    raw_dir = Path("datasets/animal_species_raw")
    snapshot_download(
        repo_id="ViratGarg/animal_species",
        repo_type="dataset",
        local_dir=str(raw_dir),
        token=token if token else None,
        max_workers=2,
    )
    print(f"Raw dataset downloaded to {raw_dir}")

    # Discover class folders
    class_dirs = [d for d in raw_dir.iterdir() if d.is_dir() and not d.name.startswith(".")]
    labels = sorted(d.name for d in class_dirs)
    print(f"Classes: {labels}")

    for split_name in ["train", "val"]:
        for label in labels:
            (root / split_name / label).mkdir(parents=True, exist_ok=True)

    counts = {label: 0 for label in labels}
    total = 0
    for class_dir in class_dirs:
        label = class_dir.name
        images = sorted(class_dir.glob("*.jpg"))
        for img_path in images:
            split_name = "train" if counts[label] % 10 != 0 else "val"
            dest = root / split_name / label / f"img_{counts[label]}.jpg"
            if not dest.exists():
                img = Image.open(img_path)
                if img.mode in ("RGBA", "P", "L", "LA", "1", "CMYK", "YCbCr", "I", "F"):
                    img = img.convert("RGB")
                img.save(dest)
            counts[label] += 1
            total += 1
            if total % 500 == 0:
                print(f"  reorganized {total} images...")

    print(f"Dataset ready: {total} images")
    for label, count in sorted(counts.items()):
        print(f"  {label}: {count}")
    return root


def main() -> None:
    data_root = prepare_dataset()

    model = YOLO("yolov8n-cls.pt")
    model.train(data=str(data_root), epochs=50, imgsz=224, batch=16, device="cpu")

    best = Path(model.trainer.best)
    print(f"Best checkpoint: {best}")

    model.export(format="onnx", imgsz=224, dynamic=False)
    onnx_path = best.with_suffix(".onnx")
    target = Path("app/src/main/resources/models/animal_species_cls.onnx")
    shutil.copy(onnx_path, target)
    print(f"ONNX model copied to {target}")


if __name__ == "__main__":
    main()
