package com.nextsavy.pawgarage.fragments

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.nextsavy.pawgarage.R
import com.nextsavy.pawgarage.databinding.FragmentPhotoViewDialogBinding
import com.nextsavy.pawgarage.utils.Constants

class PhotoViewDialog: DialogFragment() {
    private lateinit var binding: FragmentPhotoViewDialogBinding

    var photoUrl = ""


    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.getString("photoUrl")?.let {
            photoUrl = it
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPhotoViewDialogBinding.inflate(inflater, container, false)

        Glide.with(binding.animalIV.context).load(photoUrl).placeholder(R.drawable.paw_placeholder).into(binding.animalIV)

        binding.closeImg.setOnClickListener {
            dialog?.dismiss()
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        dialog?.window?.setLayout(
            Constants.getDeviceWidth(requireContext()) - Constants.getIntFromDp("16", requireContext()),
            Constants.getDeviceHeight(requireContext()) - Constants.getIntFromDp("140", requireContext()))
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setGravity(Gravity.CENTER)

    }
}