package com.norasoderlund.ridetrackerapp.presentation

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.health.services.client.data.ExerciseState
import com.norasoderlund.ridetrackerapp.R
import com.norasoderlund.ridetrackerapp.RecorderCallbacks
import com.norasoderlund.ridetrackerapp.RecorderDistanceEvent
import com.norasoderlund.ridetrackerapp.RecorderDurationEvent
import com.norasoderlund.ridetrackerapp.RecorderElevationEvent
import com.norasoderlund.ridetrackerapp.RecorderLocationEvent
import com.norasoderlund.ridetrackerapp.RecorderSessionEndEvent
import com.norasoderlund.ridetrackerapp.RecorderSpeedEvent
import com.norasoderlund.ridetrackerapp.RecorderStateInfoEvent
import org.greenrobot.eventbus.EventBus
import kotlin.math.round

class StatsPageFragment : Fragment(), RecorderCallbacks {
    private lateinit var speedValue: TextView;

    private lateinit var elevationValue: TextView;
    private lateinit var elevationUnit: TextView;

    private lateinit var distanceValue: TextView;
    private lateinit var distanceUnit: TextView;

    private lateinit var activity: MainActivity;

    private var sliderLayout: LinearLayout? = null;
    private var buttonSlider: LinearLayout? = null;

    private var finishButton: LinearLayout? = null;

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.stats_page, container, false);
    }

    override fun onStart() {
        super.onStart();

        activity = requireActivity() as MainActivity;

        activity.recorder.addCallback(this);
    }

    override fun onStop() {
        super.onStop();

        activity.recorder.removeCallback(this);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState);

        activity = requireActivity() as MainActivity;
        var context = requireContext();

        sliderLayout = view.findViewById<LinearLayout>(R.id.sliderLayout);
        buttonSlider = view.findViewById<LinearLayout>(R.id.buttonSlider);

        finishButton = view.findViewById<LinearLayout>(R.id.statsFinishButton);

        var brandColor = ContextCompat.getColor(context, R.color.brand);
        var textColor = ContextCompat.getColor(context, R.color.color);

        var elevationLabel = view.findViewById<TextView>(R.id.elevationLabel);
        elevationValue = view.findViewById<TextView>(R.id.elevationValue);
        elevationUnit = view.findViewById<TextView>(R.id.elevationUnit);

        var speedLabel = view.findViewById<TextView>(R.id.speedLabel);
        speedValue = view.findViewById<TextView>(R.id.speedValue);
        var speedUnit = view.findViewById<TextView>(R.id.speedUnit);

        var distanceLabel = view.findViewById<TextView>(R.id.distanceLabel);
        distanceValue = view.findViewById<TextView>(R.id.distanceValue);
        distanceUnit = view.findViewById<TextView>(R.id.distanceUnit);

        view.findViewById<LinearLayout>(R.id.statsRecordingButton)?.setOnClickListener {
            activity.recorder.toggle();
        }

        view.findViewById<LinearLayout>(R.id.statsFinishButton)?.setOnClickListener {
            activity.recorder.finish();
        }

        view.findViewById<ConstraintLayout>(R.id.elevationButton)?.setOnClickListener {
            setSliderAnimation(sliderLayout!!, currentSliderDp, 66f);

            currentSliderDp = 66f;

            setScaleAnimation(view.findViewById<ConstraintLayout>(R.id.elevationButton), 1f);
            setScaleAnimation(view.findViewById<ConstraintLayout>(R.id.speedButton), .66f);
            setScaleAnimation(view.findViewById<ConstraintLayout>(R.id.distanceButton), .66f);

            setButtonColor(elevationLabel, elevationValue, elevationUnit, brandColor);
            setButtonColor(speedLabel, speedValue, speedUnit, textColor);
            setButtonColor(distanceLabel, distanceValue, distanceUnit, textColor);
        }

        view.findViewById<ConstraintLayout>(R.id.speedButton)?.setOnClickListener {
            setSliderAnimation(sliderLayout!!, currentSliderDp, 0f);

            currentSliderDp = 0f;

            setScaleAnimation(view.findViewById<ConstraintLayout>(R.id.elevationButton), .66f);
            setScaleAnimation(view.findViewById<ConstraintLayout>(R.id.speedButton), 1f);
            setScaleAnimation(view.findViewById<ConstraintLayout>(R.id.distanceButton), .66f);

            setButtonColor(elevationLabel, elevationValue, elevationUnit, textColor);
            setButtonColor(speedLabel, speedValue, speedUnit, brandColor);
            setButtonColor(distanceLabel, distanceValue, distanceUnit, textColor);
        }

        view.findViewById<ConstraintLayout>(R.id.distanceButton)?.setOnClickListener {
            setSliderAnimation(sliderLayout!!, currentSliderDp, -66f);

            currentSliderDp = -66f;

            setScaleAnimation(view.findViewById<ConstraintLayout>(R.id.elevationButton), .66f);
            setScaleAnimation(view.findViewById<ConstraintLayout>(R.id.speedButton), .66f);
            setScaleAnimation(view.findViewById<ConstraintLayout>(R.id.distanceButton), 1f);

            setButtonColor(elevationLabel, elevationValue, elevationUnit, textColor);
            setButtonColor(speedLabel, speedValue, speedUnit, textColor);
            setButtonColor(distanceLabel, distanceValue, distanceUnit, brandColor);
        }

        updateRecordingButtonState(false);

        if(activity.recorder.lastSpeedEvent != null) onSpeedEvent(activity.recorder.lastSpeedEvent!!);
        if(activity.recorder.lastElevationEvent != null) onElevationEvent(activity.recorder.lastElevationEvent!!);
        if(activity.recorder.lastDistanceEvent != null) onDistanceEvent(activity.recorder.lastDistanceEvent!!);

        //if(activity.recorder.started && !activity.recorder.paused)
        //    setPageState(false, false);
    }

    fun setButtonColor(label: TextView, value: TextView, unit: TextView, color: Int) {
        label.setTextColor(color);
        value.setTextColor(color);
        unit.setTextColor(color);
    }

    var currentSliderDp: Float = 0f;

    fun setSliderAnimation(slider: View, fromDp: Float, toDp: Float): ObjectAnimator {
        var fromPixels = getDpPixels(fromDp);
        var toPixels = getDpPixels(toDp);

        var animator = ObjectAnimator.ofFloat(slider, "translationX", fromPixels, toPixels).apply {
            duration = 500
            start()
        };

        return animator;
    }

    fun setScaleAnimation(button: ConstraintLayout, scale: Float) {
        var speedButtonScaleX = PropertyValuesHolder.ofFloat("scaleX", scale);
        var speedButtonScaleY = PropertyValuesHolder.ofFloat("scaleY", scale);

        ObjectAnimator.ofPropertyValuesHolder(button, speedButtonScaleX, speedButtonScaleY).apply {
            duration = 500
            start()
        }
    }

    fun getDpPixels(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, requireContext().resources.displayMetrics);
    }

    fun updateRecordingButtonState(animate: Boolean) {
        val recordingButton = view?.findViewById<LinearLayout>(R.id.statsRecordingButton)?: return;
        val recordingButtonImage = view?.findViewById<ImageView>(R.id.statsRecordingButtonImage)?: return;
        val recordingButtonText = view?.findViewById<TextView>(R.id.statsRecordingButtonText)?: return;

        val context = requireContext();

        if(!activity.recorder.started) {
            recordingButtonImage.setImageResource(R.drawable.baseline_play_arrow_24);
            recordingButtonImage.setColorFilter(ContextCompat.getColor(context, R.color.color));
            recordingButton.background.setTint(ContextCompat.getColor(context, R.color.brand));

            recordingButtonText.text = "Start";
        }
        else if(activity.recorder.lastStateInfoEvent?.stateInfo?.state == ExerciseState.USER_PAUSED) {
            recordingButtonImage.setImageResource(R.drawable.baseline_play_arrow_24);
            recordingButtonImage.setColorFilter(ContextCompat.getColor(context, R.color.color));
            recordingButton.background.setTint(ContextCompat.getColor(context, R.color.brand));

            recordingButtonText.text = "Resume";
        }
        else {
            recordingButtonImage.setImageResource(R.drawable.baseline_stop_24);
            recordingButtonImage.setColorFilter(ContextCompat.getColor(context, R.color.color));
            recordingButton.background.setTint(ContextCompat.getColor(context, R.color.button));

            recordingButtonText.text = "Pause";
        }

        setPageState(activity.recorder.started, activity.recorder.lastStateInfoEvent?.stateInfo?.state == ExerciseState.USER_PAUSED, animate);
    }

    private fun setPageState(started: Boolean, paused: Boolean, animate: Boolean) {
        val sliderLayoutValues = mutableListOf<Float>(getDpPixels(-24f), 0f);
        val buttonSliderValues = mutableListOf<Float>(0f, getDpPixels(24f + 15f));
        val finishButtonValues = mutableListOf<Float>(1f, 0f);

        if(paused) {
            sliderLayoutValues.reverse();
            buttonSliderValues.reverse();
            finishButtonValues.reverse();
        }

        if(!animate) {
            sliderLayout?.translationY = sliderLayoutValues[1];
            buttonSlider?.translationY = buttonSliderValues[1];
            finishButton?.alpha = finishButtonValues[1];
        }
        else {
            if(sliderLayout?.translationY != sliderLayoutValues[1]) {
                ObjectAnimator.ofFloat(sliderLayout!!,  "translationY", sliderLayoutValues[0], sliderLayoutValues[1]).apply {
                    duration = 500
                    start()
                }
            }

            if(buttonSlider?.translationY != buttonSliderValues[1]) {
                ObjectAnimator.ofFloat(buttonSlider!!,  "translationY", buttonSliderValues[0], buttonSliderValues[1]).apply {
                    duration = 500
                    start()
                }
            }

            if(finishButton?.alpha != finishButtonValues[1]) {
                ObjectAnimator.ofFloat(finishButton!!,  "alpha", finishButtonValues[0], finishButtonValues[1]).apply {
                    duration = 500
                    start()
                }
            }
        }
    }

    override fun onLocationUpdate(event: RecorderLocationEvent) {
    }

    override fun onStateInfoEvent(event: RecorderStateInfoEvent) {
        updateRecordingButtonState(true);
    }

    override fun onSpeedEvent(event: RecorderSpeedEvent) {
        speedValue.text = (round((event.metersPerSecond * 3.6) * 10.0) / 10.0).toString();
    }

    override fun onDistanceEvent(event: RecorderDistanceEvent) {
        distanceValue.text = event.formattedDistance.text;
        distanceUnit.text = event.formattedDistance.unit;
    }

    override fun onElevationEvent(event: RecorderElevationEvent) {
        elevationValue.text = event.formattedDistance.text;
        elevationUnit.text = event.formattedDistance.unit;
    }

    override fun onSessionEndEvent(event: RecorderSessionEndEvent) {
    }

    override fun onDurationEvent(event: RecorderDurationEvent) {
    }
}
