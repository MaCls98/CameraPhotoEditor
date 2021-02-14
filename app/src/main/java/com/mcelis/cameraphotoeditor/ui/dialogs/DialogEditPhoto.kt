package com.mcelis.cameraphotoeditor.ui.dialogs

import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.DialogFragment
import com.mcelis.cameraphotoeditor.R
import com.mcelis.cameraphotoeditor.utils.*
import kotlinx.android.synthetic.main.dialog_edit_photo.*
import kotlinx.android.synthetic.main.menu_controls.*
import java.io.File

class DialogEditPhoto: DialogFragment(), View.OnClickListener {

    private lateinit var photoPath: String
    private lateinit var currentMenuSelected: String
    private lateinit var currentOptionSelected: String

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.dialog_edit_photo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        photoPath = arguments?.getString("photo")!!
        setPhotoToImageView()
        initMenuOptionsView()
        setOnClicks()
        showRotateOptions()
        ib_close.setOnClickListener {
            dismiss()
        }
    }

    private fun initMenuOptionsView() {
        fl_menu_options.removeAllViews()
        fl_menu_options.addView(
                LayoutInflater.from(requireContext()).inflate(R.layout.menu_controls, cl_photo_options, false)
        )
    }

    private fun showRotateOptions() {
        currentMenuSelected = MENU_ROTATE
        currentOptionSelected = OPTION_ONE
        fl_menu_options.visibility = View.VISIBLE
        tv_sb_title.visibility = View.INVISIBLE
        tv_sb_progress.visibility = View.INVISIBLE
        sb_value.visibility = View.INVISIBLE
        ib_first_option.setImageResource(R.drawable.icon_rotate_left)
        tv_first_option.text = getString(R.string.left)
        ib_second_option.setImageResource(R.drawable.icon_rotate_right)
        tv_second_option.text = getString(R.string.right)
        ib_third_option.setImageResource(R.drawable.icon_horizontal)
        tv_third_option.text = getString(R.string.horizontal)
        ib_fourth_option.setImageResource(R.drawable.icon_vertical)
        tv_fourth_option.text = getString(R.string.vertical)
    }

    private fun showAdjustOptions() {
        currentMenuSelected = MENU_ADJUST
        fl_menu_options.visibility = View.INVISIBLE
    }

    private fun showColorOptions() {
        currentMenuSelected = MENU_COLOR
        currentOptionSelected = OPTION_ONE
        fl_menu_options.visibility = View.VISIBLE
        sb_value.visibility = View.VISIBLE
        tv_sb_title.visibility = View.VISIBLE
        tv_sb_progress.visibility = View.VISIBLE
        tv_sb_title.text = getString(R.string.brightness)
        ib_first_option.setImageResource(R.drawable.icon_brightness)
        tv_first_option.text = getString(R.string.brightness)
        ib_second_option.setImageResource(R.drawable.icon_contrast)
        tv_second_option.text = getString(R.string.contrast)
        ib_third_option.setImageResource(R.drawable.icon_saturation)
        tv_third_option.text = getString(R.string.saturation)
        ib_fourth_option.setImageResource(R.drawable.icon_temperature)
        tv_fourth_option.text = getString(R.string.temperature)
        setSeekBarListener()
    }

    private fun setSeekBarListener() {
        sb_value.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                when (currentMenuSelected) {
                    MENU_COLOR -> {
                        when (currentOptionSelected) {
                            OPTION_ONE -> {
                                tv_sb_progress.text = "$p1"
                                iv_photo.colorFilter = MyBitmapUtils.setBrightness(p1)
                            }
                            OPTION_TWO -> {
                                tv_sb_progress.text = "$p1"
                                iv_photo.setImageBitmap(MyBitmapUtils.changeImageViewContrast(
                                        BitmapFactory.decodeFile(photoPath),
                                        p1 / 100f))
                            }
                        }
                    }
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) = Unit

            override fun onStopTrackingTouch(p0: SeekBar?) = Unit

        })
    }

    private fun showFilterOptions() {
        currentMenuSelected = MENU_FILTER
        fl_menu_options.visibility = View.VISIBLE
    }

    private fun initFirstOption() {
        currentOptionSelected = OPTION_ONE
        when(currentMenuSelected){
            MENU_ROTATE -> MyBitmapUtils.rotateImage(iv_photo, -90f, Z_AXIS)
            MENU_COLOR -> {
                tv_sb_title.text = getString(R.string.brightness)
                sb_value.isEnabled = true
                sb_value.max = 200
                sb_value.progress = 100
                tv_sb_progress.text = "100"
            }
        }
    }

    private fun initSecondOption() {
        currentOptionSelected = OPTION_TWO
        when(currentMenuSelected){
            MENU_ROTATE -> MyBitmapUtils.rotateImage(iv_photo, 90f, Z_AXIS)
            MENU_COLOR -> {
                tv_sb_title.text = getString(R.string.contrast)
                sb_value.isEnabled = true
                sb_value.max = 200
                sb_value.progress = 100
                tv_sb_progress.text = "100"
            }
        }
    }

    private fun initThirdOption() {
        currentOptionSelected = OPTION_THREE
        when(currentMenuSelected){
            MENU_ROTATE -> MyBitmapUtils.rotateImage(iv_photo, -180f, Y_AXIS)
            MENU_COLOR -> {
                tv_sb_title.text = getString(R.string.saturation)
                sb_value.isEnabled = false
                sb_value.progress = 100
                tv_sb_progress.text = "100"
            }
        }
    }

    private fun initFourthOption() {
        currentOptionSelected = OPTION_FOURTH
        when(currentMenuSelected){
            MENU_ROTATE -> MyBitmapUtils.rotateImage(iv_photo, 180f, X_AXIS)
            MENU_COLOR -> {
                tv_sb_title.text = getString(R.string.temperature)
                sb_value.isEnabled = false
                sb_value.progress = 100
                tv_sb_progress.text = "100"
            }
        }
    }

    private fun setOnClicks() {
        btn_rotate.setOnClickListener(this)
        btn_adjust.setOnClickListener(this)
        btn_contrast.setOnClickListener(this)
        btn_filter.setOnClickListener(this)
        ib_first_option.setOnClickListener(this)
        ib_second_option.setOnClickListener(this)
        ib_third_option.setOnClickListener(this)
        ib_fourth_option.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        when(view.id){
            R.id.btn_rotate -> showRotateOptions()
            R.id.btn_adjust -> showAdjustOptions()
            R.id.btn_contrast -> showColorOptions()
            R.id.btn_filter -> showFilterOptions()
            R.id.ib_first_option -> initFirstOption()
            R.id.ib_second_option -> initSecondOption()
            R.id.ib_third_option -> initThirdOption()
            R.id.ib_fourth_option -> initFourthOption()
        }
    }

    private fun setPhotoToImageView() {
        iv_photo.setImageURI(Uri.fromFile(File(photoPath)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.MyCustomFullScreenDialog)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }
}