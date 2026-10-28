import streamlit as st
import cv2
import numpy as np
import mediapipe as mp
import pickle
import time
from collections import deque

# App setup
st.set_page_config(page_title="Gesture Recognition", layout="wide")
st.title("🤚 Real-time Hand Gesture Recognition")

# Load model
@st.cache_resource
def load_model():
    try:
        with open("gesture_model.pkl", "rb") as f:
            data = pickle.load(f)
        return data["model"], data["class_names"]
    except Exception as e:
        st.error(f"Model load error: {e}")
        return None, None

model, class_names = load_model()

# Initialize MediaPipe
@st.cache_resource
def init_hands():
    mp_hands = mp.solutions.hands
    hands = mp_hands.Hands(static_image_mode=False, max_num_hands=1,
                           min_detection_confidence=0.7,
                           min_tracking_confidence=0.6)
    return hands, mp_hands

hands, mp_hands = init_hands()
mp_draw = mp.solutions.drawing_utils

# Prediction buffer
pred_buffer = deque(maxlen=5)

def predict(image):
    results = hands.process(cv2.cvtColor(image, cv2.COLOR_BGR2RGB))
    if results.multi_hand_landmarks:
        lm = results.multi_hand_landmarks[0]
        coords = np.array([[pt.x, pt.y] for pt in lm.landmark]).flatten().reshape(1, -1)
        prediction = model.predict(coords)[0]
        confidence = np.max(model.predict_proba(coords))
        return int(prediction), confidence, lm
    return None, 0.0, None

# Start camera
run = st.checkbox("📷 Start Camera")

FRAME_WINDOW = st.image([])

if run:
    cap = cv2.VideoCapture(0)

    while run:
        ret, frame = cap.read()
        if not ret:
            st.warning("Failed to read from webcam.")
            break

        frame = cv2.flip(frame, 1)
        pred, conf, lm = predict(frame)

        if lm:
            mp_draw.draw_landmarks(frame, lm, mp_hands.HAND_CONNECTIONS)

        if pred is not None:
            pred_buffer.append((pred, conf))
            # Majority vote smoothing
            pred_smooth = max(set([p[0] for p in pred_buffer]), key=[p[0] for p in pred_buffer].count)
            conf_avg = np.mean([p[1] for p in pred_buffer])
            label = class_names[pred_smooth]
            cv2.putText(frame, f'{label} ({conf_avg:.2f})', (10, 40),
                        cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)

        FRAME_WINDOW.image(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB))
        time.sleep(0.03)
    cap.release()
