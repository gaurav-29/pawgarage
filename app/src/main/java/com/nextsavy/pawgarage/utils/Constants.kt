package com.nextsavy.pawgarage.utils

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.WindowManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextsavy.pawgarage.R
import kotlin.math.roundToInt

class Constants {

    companion object {

        fun getDeviceWidth(context: Context): Int {
            val displayMetrics = DisplayMetrics()
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            return displayMetrics.widthPixels
        }

        fun getIntFromDp(value: String, context: Context): Int {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value.toFloat(),
                context.resources.displayMetrics
            ).roundToInt()
        }

        fun getDeviceHeight(context: Context): Int {
            val displayMetrics = DisplayMetrics()
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            return displayMetrics.heightPixels
        }

        fun showAlertWithListeners(
            context: Context,
            title: String,
            message: String,
            positiveTitle: String,
            positiveListener: DialogInterface.OnClickListener?,
            negativeTitle: String,
            negativeListener: DialogInterface.OnClickListener?
        ) {

            val builder = AlertDialog.Builder(context)
            builder.setTitle(title)
            builder.setMessage(message)

            if (positiveListener == null && positiveTitle.isBlank()) {
                builder.setPositiveButton("OK") { _, _ -> }
            } else {
                builder.setPositiveButton(positiveTitle, positiveListener)
            }

            negativeListener?.let {
                builder.setNegativeButton(negativeTitle, it)
            }

            builder.show()
        }
    }

}