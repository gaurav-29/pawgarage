package com.nextsavy.pawgarage.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.nextsavy.pawgarage.fragments.AllRemindersFragment
import com.nextsavy.pawgarage.fragments.TodayRemindersFragment
import com.nextsavy.pawgarage.fragments.TomorrowRemindersFragment
import com.nextsavy.pawgarage.fragments.TurnedOffRemindersFragment

class MyReminderStateAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int {
        return 4
    }

    override fun createFragment(position: Int): Fragment {
        when(position) {
            0 -> return AllRemindersFragment()
            1 -> return TodayRemindersFragment()
            2 -> return TomorrowRemindersFragment()
            3 -> return TurnedOffRemindersFragment()
            else -> return AllRemindersFragment()
        }
    }
}