package com.nextsavy.pawgarage.fragments

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import androidx.activity.addCallback
import androidx.navigation.findNavController
import androidx.navigation.navGraphViewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.nextsavy.pawgarage.MainActivity
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.adapters.AnimalsViewPagerAdapter
import com.nextsavy.pawgarage.databinding.FragmentAnimalsBinding
import com.nextsavy.pawgarage.utils.SearchViewModel

class AnimalsFragment : Fragment() {

    private lateinit var binding: FragmentAnimalsBinding

    private val viewModel: SearchViewModel by navGraphViewModels<SearchViewModel>(R.id.animal_graph)

    // Search String
    var searchedText = ""

    // Search Handler
    var searchHandler: Handler? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAnimalsBinding.inflate(inflater, container, false)

        binding.toolbarMain.titleToolbarMain.text = "Animals"
        onClickListeners()
        configureTabLayoutWithViewPager2()

        binding.cancelSearchButton.setOnClickListener {
            this.hideKeyboard(it)
            it.clearFocus()
            binding.searchView.clearFocus()
            binding.searchView.setQuery("", true)
            binding.cancelSearchButton.visibility = View.GONE
            searchedText = ""
        }

        binding.searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                val currentText = p0 ?: ""
                if (currentText.isNotBlank()) {
//                    searchedText = currentText.trim()
                    searchedText = currentText.trim()
                    searchHandler?.removeCallbacksAndMessages(null)
                    searchHandler = Handler(Looper.getMainLooper())
                    searchHandler?.postDelayed({ viewModel.searchQuery(searchedText) }, 700)
                    binding.cancelSearchButton.visibility = View.VISIBLE
                } else {
                    searchedText = ""
                    viewModel.searchQuery(searchedText)
                }
                return false
            }
        })

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            (requireActivity() as MainActivity?)?.binding?.bottomNav?.selectedItemId = R.id.homeFragment
        }

        return binding.root
    }

    private fun hideKeyboard(view: View?) {
        val inputMethodManager = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        activity?.currentFocus?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
            view?.clearFocus()
        }
    }

    private fun configureTabLayoutWithViewPager2() {

        val tabTitle = arrayOf("ALL", "IPD", "OPD")

        binding.categoryVP.offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
        binding.categoryVP.adapter = AnimalsViewPagerAdapter(childFragmentManager, lifecycle) // we should use childFragmentManager
        // instead of supportFragmentManager whenever we use viewpager in fragment. (this is to solve the below problem,
        // onResume method not called when fragment is in viewpager2 and coming back from fragment other than viewpager2 fragment)
        TabLayoutMediator(binding.categoryTL, binding.categoryVP) { tab, position ->
            tab.text = tabTitle[position]
        }.attach()
    }
    private fun onClickListeners() {
        binding.toolbarMain.notificationsIV.setOnClickListener {
            it.findNavController().navigate(R.id.notificationsFragment)
        }
    }
}