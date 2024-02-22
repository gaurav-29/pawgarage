package com.nextsavy.pawgarage.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.adapters.MyFragmentStateAdapter
import com.nextsavy.pawgarage.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {

    private lateinit var binding: FragmentNotificationsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false)

        binding.toolbarOne.titleToolbarOne.text = "Notifications"
        binding.toolbarOne.generalImgToolbarOne.visibility = View.VISIBLE
        binding.toolbarOne.generalImgToolbarOne.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.primary_color)

        configureTabLayoutWithViewPager2()
        onClickListeners()

        return binding.root
    }

    private fun configureTabLayoutWithViewPager2() {
        val tabTitle = arrayOf("All", "New", "Vaccination", "Deworming", "Status", "Profile Leads")

        binding.categoryVP.offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
        binding.categoryVP.adapter = MyFragmentStateAdapter(childFragmentManager, lifecycle) // we should use childFragmentManager
        // instead of supportFragmentManager whenever we use viewpager in fragment. (this is to solve the below problem,
        // onResume method not called when fragment is in viewpager2 and coming back from fragment other than viewpager2 fragment)
        TabLayoutMediator(binding.categoryTL, binding.categoryVP) { tab, position ->
            tab.text = tabTitle[position]
        }.attach()
    }

    private fun onClickListeners() {
        binding.toolbarOne.backToolbarOne.setOnClickListener {
            it.findNavController().navigateUp()
        }
    }
}