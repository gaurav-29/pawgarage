package com.nextsavy.pawgarage.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.nextsavy.pawgarage.fragments.AllNotificationsFragment
import com.nextsavy.pawgarage.fragments.DewormingNotificationsFragment
import com.nextsavy.pawgarage.fragments.NewNotificationsFragment
import com.nextsavy.pawgarage.fragments.ProfileLeadsNotificationsFragment
import com.nextsavy.pawgarage.fragments.ReleasedNotificationsFragment
import com.nextsavy.pawgarage.fragments.VaccinationNotificationsFragment

class MyFragmentStateAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int {
        return 6
    }

    override fun createFragment(position: Int): Fragment {
        when(position) {
            0 -> return AllNotificationsFragment()
            1 -> return NewNotificationsFragment()
            2 -> return VaccinationNotificationsFragment()
            3 -> return DewormingNotificationsFragment()
            4 -> return ReleasedNotificationsFragment()
            5 -> return ProfileLeadsNotificationsFragment()
            else -> return AllNotificationsFragment()
        }
    }
}