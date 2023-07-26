package com.norasoderlund.ridetrackerapp.presentation

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.norasoderlund.ridetrackerapp.R
import com.norasoderlund.ridetrackerapp.RecorderStateEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class StatsPageFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.stats_page, container, false);
    }

    override fun onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }

    override fun onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState);

        var activity = requireActivity() as MainActivity;
        var context = requireContext();

        var brandColor = ContextCompat.getColor(context, R.color.brand);
        var textColor = ContextCompat.getColor(context, R.color.color);

        var elevationLabel = view.findViewById<TextView>(R.id.elevationLabel);
        var elevationValue = view.findViewById<TextView>(R.id.elevationValue);
        var elevationUnit = view.findViewById<TextView>(R.id.elevationUnit);

        var speedLabel = view.findViewById<TextView>(R.id.speedLabel);
        var speedValue = view.findViewById<TextView>(R.id.speedValue);
        var speedUnit = view.findViewById<TextView>(R.id.speedUnit);

        var distanceLabel = view.findViewById<TextView>(R.id.distanceLabel);
        var distanceValue = view.findViewById<TextView>(R.id.distanceValue);
        var distanceUnit = view.findViewById<TextView>(R.id.distanceUnit);

        view.findViewById<LinearLayout>(R.id.statsRecordingButton)?.setOnClickListener {
            activity.recorder.toggle();
        }

        view.findViewById<ConstraintLayout>(R.id.elevationButton)?.setOnClickListener {
            setSliderAnimation(view.findViewById<LinearLayout>(R.id.sliderLayout), 100f);

            setScaleAnimation(view.findViewById<ConstraintLayout>(R.id.elevationButton), 1f);
            setScaleAnimation(view.findViewById<ConstraintLayout>(R.id.speedButton), .66f);
            setScaleAnimation(view.findViewById<ConstraintLayout>(R.id.distanceButton), .66f);

            setButtonColor(elevationLabel, elevationValue, elevationUnit, brandColor);
            setButtonColor(speedLabel, speedValue, speedUnit, textColor);
            setButtonColor(distanceLabel, distanceValue, distanceUnit, textColor);
        }

        view.findViewById<ConstraintLayout>(R.id.speedButton)?.setOnClickListener {
            setSliderAnimation(view.findViewById<LinearLayout>(R.id.sliderLayout), 0f);

            setScaleAnimation(view.findViewById<ConstraintLayout>(R.id.elevationButton), .66f);
            setScaleAnimation(view.findViewById<ConstraintLayout>(R.id.speedButton), 1f);
            setScaleAnimation(view.findViewById<ConstraintLayout>(R.id.distanceButton), .66f);

            setButtonColor(elevationLabel, elevationValue, elevationUnit, textColor);
            setButtonColor(speedLabel, speedValue, speedUnit, brandColor);
            setButtonColor(distanceLabel, distanceValue, distanceUnit, textColor);
        }

        view.findViewById<ConstraintLayout>(R.id.distanceButton)?.setOnClickListener {
            setSliderAnimation(view.findViewById<LinearLayout>(R.id.sliderLayout), -100f);

            setScaleAnimation(view.findViewById<ConstraintLayout>(R.id.elevationButton), .66f);
            setScaleAnimation(view.findViewById<ConstraintLayout>(R.id.speedButton), .66f);
            setScaleAnimation(view.findViewById<ConstraintLayout>(R.id.distanceButton), 1f);

            setButtonColor(elevationLabel, elevationValue, elevationUnit, textColor);
            setButtonColor(speedLabel, speedValue, speedUnit, textColor);
            setButtonColor(distanceLabel, distanceValue, distanceUnit, brandColor);
        }
    }

    fun setButtonColor(label: TextView, value: TextView, unit: TextView, color: Int) {
        label.setTextColor(color);
        value.setTextColor(color);
        unit.setTextColor(color);
    }

    var currentSliderDp: Float = 0f;

    fun setSliderAnimation(slider: LinearLayout, dp: Float) {
        var pixels = getDpPixels(dp);

        ObjectAnimator.ofFloat(slider, "translationX", currentSliderDp, pixels).apply {
            duration = 500
            start()
        };

        currentSliderDp = pixels;
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onRecorderStateEvent(event: RecorderStateEvent) {
        val recordingButton = view?.findViewById<LinearLayout>(R.id.statsRecordingButton)?: return;
        val recordingButtonImage = view?.findViewById<ImageView>(R.id.statsRecordingButtonImage)?: return;
        val recordingButtonText = view?.findViewById<TextView>(R.id.statsRecordingButtonText)?: return;

        val context = requireContext();

        if(event.started && !event.paused) {
            recordingButtonImage.setImageResource(R.drawable.baseline_stop_24);
            recordingButtonImage.setColorFilter(ContextCompat.getColor(context, R.color.color));
            recordingButton.background.setTint(ContextCompat.getColor(context, R.color.button));

            recordingButtonText.text = "Pause";
        }
        else {
            recordingButtonImage.setImageResource(R.drawable.baseline_play_arrow_24);
            recordingButtonImage.setColorFilter(ContextCompat.getColor(context, R.color.color));
            recordingButton.background.setTint(ContextCompat.getColor(context, R.color.brand));

            recordingButtonText.text = if(event.started) "Resume" else "Start";
        }
    }
}
