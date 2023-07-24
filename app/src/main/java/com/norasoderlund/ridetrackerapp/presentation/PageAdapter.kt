package com.norasoderlund.ridetrackerapp.presentation

import android.graphics.Color
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.norasoderlund.ridetrackerapp.R

class PageAdapter : FragmentStateAdapter {
    var mapPageFragment: MapPageFragment;
    var statsPageFragment: StatsPageFragment;
    var fragmentActivity: FragmentActivity;

    constructor(fragmentActivity: FragmentActivity) : super(fragmentActivity) {
        this.fragmentActivity = fragmentActivity;

        mapPageFragment = MapPageFragment();
        statsPageFragment = StatsPageFragment();
    }

    override fun getItemCount(): Int {
        return 2;
    }

    override fun createFragment(position: Int): Fragment {
        if(position == 0)
            return mapPageFragment;

        if(position == 1)
            return statsPageFragment;

        throw IllegalArgumentException(String.format("Invalid position: %i", position));
    }
}