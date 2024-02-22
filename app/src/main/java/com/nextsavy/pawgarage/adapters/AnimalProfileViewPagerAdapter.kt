package com.nextsavy.pawgarage.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.nextsavy.pawgarage.fragments.AdmissionListFragment
import com.nextsavy.pawgarage.fragments.AnimalDetailsFragment
import com.nextsavy.pawgarage.fragments.DewormingListFragment
import com.nextsavy.pawgarage.fragments.ReleaseDetailsListFragment
import com.nextsavy.pawgarage.fragments.TreatmentListFragment
import com.nextsavy.pawgarage.fragments.VaccinationListFragment

class AnimalProfileViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle): FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int {
        return 6
    }

    override fun createFragment(position: Int): Fragment {
        when(position) {
            0 -> return AnimalDetailsFragment()
            1 -> return AdmissionListFragment()
            2 -> return DewormingListFragment()
            3 -> return VaccinationListFragment()
            4 -> return ReleaseDetailsListFragment()
            5 -> return TreatmentListFragment()
            else -> return AnimalDetailsFragment()
        }
    }
}