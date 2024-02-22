package com.nextsavy.pawgarage.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.nextsavy.pawgarage.MainActivity
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.adapters.MyReminderStateAdapter
import com.nextsavy.pawgarage.databinding.FragmentRemindersBinding


class RemindersFragment : Fragment() {

    private lateinit var binding: FragmentRemindersBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRemindersBinding.inflate(inflater, container, false)

        binding.toolbarMain.titleToolbarMain.text = "Reminders"

        onClickListeners()

        configureTabLayoutWithViewPager2()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            (requireActivity() as MainActivity?)?.binding?.bottomNav?.selectedItemId = R.id.homeFragment
        }

        return binding.root
    }

    private fun onClickListeners() {
        binding.toolbarMain.notificationsIV.setOnClickListener {
            it.findNavController().navigate(R.id.notificationsFragment)
        }
    }

    private fun configureTabLayoutWithViewPager2() {
        val tabTitle = arrayOf("All", "Today", "Tomorrow", "Turned Off")

        binding.categoryVP.offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
        binding.categoryVP.adapter = MyReminderStateAdapter(childFragmentManager, lifecycle) // we should use childFragmentManager
        // instead of supportFragmentManager whenever we use viewpager in fragment. (this is to solve the below problem,
        // onResume method not called when fragment is in viewpager2 and coming back from fragment other than viewpager2 fragment)
        TabLayoutMediator(binding.categoryTL, binding.categoryVP) { tab, position ->
            tab.text = tabTitle[position]
        }.attach()
    }
}