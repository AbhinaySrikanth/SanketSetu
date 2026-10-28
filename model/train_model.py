import os
import cv2
import numpy as np
import mediapipe as mp
import pickle
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report

# Initialize MediaPipe
mp_hands = mp.solutions.hands
hands = mp_hands.Hands(static_image_mode=True, max_num_hands=1,
                       min_detection_confidence=0.7)
    
def extract_landmarks(image):
    """Extract 21 landmark (x, y) pairs from hand"""
    results = hands.process(cv2.cvtColor(image, cv2.COLOR_BGR2RGB))
    if results.multi_hand_landmarks:
        landmarks = results.multi_hand_landmarks[0]
        return np.array([[lm.x, lm.y] for lm in landmarks.landmark]).flatten()
    return None

def load_data(dataset_path):
    X, y = [], []
    classes = sorted([d for d in os.listdir(dataset_path) if os.path.isdir(os.path.join(dataset_path, d))])
    for label in classes:
        folder = os.path.join(dataset_path, label)
        for file in os.listdir(folder):
            if file.endswith(('.jpg', '.png', '.jpeg')):
                img_path = os.path.join(folder, file)
                image = cv2.imread(img_path)
                landmarks = extract_landmarks(image)
                if landmarks is not None:
                    X.append(landmarks)
                    y.append(int(label))
    return np.array(X), np.array(y), classes

def main():
    dataset_path = input("📁 Enter path to dataset folder (e.g., ./dataset): ").strip()
    X, y, classes = load_data(dataset_path)

    print(f"✅ Loaded {len(X)} samples from image dataset.")

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    model = RandomForestClassifier(n_estimators=150, random_state=42)
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)
    acc = accuracy_score(y_test, y_pred)
    print(f"🎯 Model Accuracy: {acc * 100:.2f}%")
    print("📋 Classification Report:")
    print(classification_report(y_test, y_pred, labels=list(range(len(classes))),
                                target_names=classes))

    model_data = {
        "model": model,
        "class_names": classes,
        "training_info": {
            "algorithm": "RandomForestClassifier",
            "accuracy": acc,
            "num_classes": len(classes)
        }
    }

    with open("gesture_model.pkl", "wb") as f:
        pickle.dump(model_data, f)
    print("✅ Model saved to gesture_model.pkl")

if __name__ == "__main__":
    main()
