import os,sys,random,numpy as np,cv2,mediapipe as mp
import tensorflow as tf
D="D:/MAJOR PROJECT/model/Dataset"
hands=mp.solutions.hands.Hands(static_image_mode=True,max_num_hands=1,min_detection_confidence=0.7)
it=tf.lite.Interpreter(model_path="D:/MAJOR PROJECT/model/gesture_model.tflite");it.allocate_tensors()
i=it.get_input_details()[0]["index"];o=it.get_output_details()[0]["index"]
random.seed(1);ok=n=miss=tot=0;conf=[]
for c in range(10):
    fs=os.listdir(f"{D}/{c}");random.shuffle(fs)
    for f in fs[:40]:
        tot+=1
        im=cv2.imread(f"{D}/{c}/{f}")
        if im is None: continue
        r=hands.process(cv2.cvtColor(im,cv2.COLOR_BGR2RGB))
        if not r.multi_hand_landmarks: miss+=1;continue
        v=np.array([[p.x,p.y] for p in r.multi_hand_landmarks[0].landmark],dtype=np.float32).flatten()[None]
        it.set_tensor(i,v);it.invoke();p=it.get_tensor(o)[0]
        n+=1;ok+=int(p.argmax()==c);conf.append(float(p.max()))
print("total",tot,"no-hand",miss,"evaluated",n,"acc",ok/max(n,1),"sum of output",float(p.sum()),"median conf",float(np.median(conf)))
