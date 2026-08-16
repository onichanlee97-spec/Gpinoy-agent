package com.example.localtoycloud

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.widget.FrameLayout

class GyroscopeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private var targetTranslationX = 0f
    private var targetTranslationY = 0f

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        rotationVectorSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            
            val orientationValues = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationValues)

            // orientationValues[2] is roll, orientationValues[1] is pitch
            val roll = orientationValues[2]
            val pitch = orientationValues[1]

            // Calculate parallax translation offset based on device tilt
            targetTranslationX = -roll * 40f
            targetTranslationY = pitch * 40f

            // Apply smooth translation to simulate floating dimensional depth
            animate()
                .translationX(targetTranslationX)
                .translationY(targetTranslationY)
                .setDuration(50)
                .start()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No action required
    }
}
