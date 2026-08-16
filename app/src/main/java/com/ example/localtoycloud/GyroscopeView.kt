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

    private var sensorManager: SensorManager? = null
    private var rotationVectorSensor: Sensor? = null

    init {
        try {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        } catch (e: Exception) {
            // Suppress sensor initialization failures on emulators or restricted hardware
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        try {
            rotationVectorSensor?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        } catch (e: Exception) {
            // Suppress registration exceptions
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try {
            sensorManager?.unregisterListener(this)
        } catch (e: Exception) {
            // Suppress unregistration exceptions
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            try {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                
                val orientationValues = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientationValues)

                val roll = orientationValues[2]
                val pitch = orientationValues[1]

                val targetTranslationX = -roll * 40f
                val targetTranslationY = pitch * 40f

                animate()
                    .translationX(targetTranslationX)
                    .translationY(targetTranslationY)
                    .setDuration(50)
                    .start()
            } catch (e: Exception) {
                // Suppress calculation errors
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No action required
    }
}
