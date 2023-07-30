package com.norasoderlund.ridetrackerapp.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import com.norasoderlund.ridetrackerapp.R

private const val MAX_ICON_PROGRESS = 0.65f

class CustomScrollingLayoutCallback : WearableLinearLayoutManager.LayoutCallback() {

    private var progressToCenter: Float = 0f

    override fun onLayoutFinished(child: View, parent: RecyclerView) {
        child.apply {
            // Figure out % progress from top to bottom.
            val centerOffset = height.toFloat() / 2.0f / parent.height.toFloat()
            val yRelativeToCenterOffset = y / parent.height + centerOffset

            // Normalize for center.
            progressToCenter = Math.abs(0.5f - yRelativeToCenterOffset)
            // Adjust to the maximum scale.
            progressToCenter = Math.min(progressToCenter, MAX_ICON_PROGRESS)

            scaleX = 1 - progressToCenter
            scaleY = 1 - progressToCenter
        }
    }
}

class CustomSnapHelper : LinearSnapHelper() {

    override fun findTargetSnapPosition(layoutManager: RecyclerView.LayoutManager?, velocityX: Int, velocityY: Int): Int {
        if (layoutManager == null) return RecyclerView.NO_POSITION

        val centerView = findSnapView(layoutManager) ?: return RecyclerView.NO_POSITION
        val position = layoutManager.getPosition(centerView)

        if (position == RecyclerView.NO_POSITION) return RecyclerView.NO_POSITION

        val centerOffset = centerView.height.toFloat() / 2.0f / layoutManager.height.toFloat()
        val yRelativeToCenterOffset = centerView.y / layoutManager.height + centerOffset
        val progressToCenter = Math.abs(0.5f - yRelativeToCenterOffset)
        val snapThreshold = 0.25f // Adjust this threshold as needed

        return if (progressToCenter < snapThreshold) {
            position
        } else {
            if (yRelativeToCenterOffset > 0.5f) position + 1 else position
        }
    }
}

class MenuPageFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.menu_page, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState);

        val wearableRecyclerView = view.findViewById<WearableRecyclerView>(R.id.wearableRecyclerView);
        wearableRecyclerView.isEdgeItemsCenteringEnabled = true;

        wearableRecyclerView.layoutManager = WearableLinearLayoutManager(requireContext(), CustomScrollingLayoutCallback())

        val snapHelper = CustomSnapHelper()
        snapHelper.attachToRecyclerView(wearableRecyclerView)

        val itemList: MutableList<MenuItem> = ArrayList()

        itemList.add(MenuItem("Set ride goals", R.drawable.baseline_auto_awesome_24, ContextCompat.getColor(requireContext(), R.color.yellow)) {

        })

        itemList.add(MenuItem("Settings", R.drawable.baseline_settings_24, ContextCompat.getColor(requireContext(), androidx.wear.input.R.color.grey)) {

        })

        itemList.add(MenuItem("Sign out", R.drawable.baseline_logout_24, ContextCompat.getColor(requireContext(), R.color.red)) {
            val activity = activity as MainActivity;

            activity.tokenStore.deleteKey();

            activity.setLoginView();
        })

        val myAdapter = MyAdapter(itemList)
        wearableRecyclerView.adapter = myAdapter

    }

    override fun onStart() {
        super.onStart();
    }

    override fun onStop() {
        super.onStop();
    }
}
