package com.nextsavy.pawgarage.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.nextsavy.pawgarage.fragments.AllAnimalsFragment
import com.nextsavy.pawgarage.fragments.IPDAnimalsFragment
import com.nextsavy.pawgarage.fragments.OPDAnimalsFragment

class AnimalsViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle): FragmentStateAdapter(fragmentManager, lifecycle){
    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        when(position) {
            0 -> return AllAnimalsFragment()
            1 -> return IPDAnimalsFragment()
            2 -> return OPDAnimalsFragment()
            else -> return AllAnimalsFragment()
        }
    }

}