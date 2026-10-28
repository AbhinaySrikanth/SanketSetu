package com.abhinay.ISLEnglishGujaratiApp.ml

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.channels.FileChannel

/**
 * Model contract (see MODEL_CARD.md): input is the 21 MediaPipe hand landmarks of one hand as
 * 42 floats [x0,y0,x1,y1,...], normalised image coordinates, upright frame, NOT mirrored.
 * Output is 10 softmax scores; index i is the digit i.
 */
object Contract {
    const val MODEL_ASSET = "digit_landmarks_v1.tflite"
    const val HAND_ASSET = "hand_landmarker.task"
    const val INPUTS = 42
    const val CLASSES = 10
}

class DigitClassifier(context: Context) : AutoCloseable {
    private val interpreter: Interpreter

    init {
        val fd = context.assets.openFd(Contract.MODEL_ASSET)
        val buffer = FileInputStream(fd.fileDescriptor).channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
        interpreter = Interpreter(buffer)
        val inShape = interpreter.getInputTensor(0).shape()
        val outShape = interpreter.getOutputTensor(0).shape()
        check(inShape.last() == Contract.INPUTS && outShape.last() == Contract.CLASSES) { "Model does not match the app contract" }
    }

    fun scores(landmarks: FloatArray): FloatArray {
        require(landmarks.size == Contract.INPUTS)
        val out = Array(1) { FloatArray(Contract.CLASSES) }
        interpreter.run(arrayOf(landmarks), out)
        return out[0]
    }

    override fun close() = interpreter.close()
}

/** Camera frame -> hand landmarks -> digit scores. Owns the native models; call [close] when done. */
class HandSignEngine private constructor(private val landmarker: HandLandmarker, private val classifier: DigitClassifier) : AutoCloseable {

    /** [scores] is null unless exactly one hand is visible. */
    class Frame(val hands: Int, val scores: FloatArray?)

    fun analyze(image: ImageProxy): Frame {
        val mp = BitmapImageBuilder(image.toBitmap()).build()
        val options = ImageProcessingOptions.builder().setRotationDegrees(image.imageInfo.rotationDegrees).build()
        val hands = landmarker.detect(mp, options).landmarks()
        if (hands.size != 1) return Frame(hands.size, null)
        val v = FloatArray(Contract.INPUTS)
        hands[0].forEachIndexed { i, p -> v[2 * i] = p.x(); v[2 * i + 1] = p.y() }
        return Frame(1, classifier.scores(v))
    }

    override fun close() { landmarker.close(); classifier.close() }

    companion object {
        /** Both model files must be bundled; otherwise the feature is reported as unavailable instead of faking a result. */
        fun isAvailable(context: Context): Boolean {
            val names = context.assets.list("").orEmpty()
            return Contract.MODEL_ASSET in names && Contract.HAND_ASSET in names
        }

        fun create(context: Context): HandSignEngine {
            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath(Contract.HAND_ASSET).build())
                .setRunningMode(RunningMode.IMAGE)
                .setNumHands(2)
                .setMinHandDetectionConfidence(0.7f)
                .build()
            return HandSignEngine(HandLandmarker.createFromOptions(context, options), DigitClassifier(context))
        }
    }
}

/** Runs on the analysis executor, keeps only the latest frame, and always closes the frame. */
class HandSignAnalyzer(
    private val engine: HandSignEngine,
    private val policy: RecognitionPolicy,
    private val onResult: (Recognition) -> Unit,
    private val onFailure: (Throwable) -> Unit,
) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        try {
            val f = engine.analyze(image)
            onResult(policy.onFrame(f.hands, f.scores))
        } catch (t: Throwable) {
            onFailure(t)
        } finally {
            image.close()
        }
    }
}
