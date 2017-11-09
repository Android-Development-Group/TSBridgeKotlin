package com.tsbridge.activity

import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import com.tsbridge.R
import com.tsbridge.utils.Utils
import kotlinx.android.synthetic.main.permission_activity.*
import org.jetbrains.anko.sdk25.coroutines.onClick

class PermissionActivity : AppCompatActivity() {
    private var mPermissionName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Utils.showLog("PermissionActivity created")

        setContentView(R.layout.permission_activity)

        initialization()
    }

    private fun initialization() {
        val bundle = intent.extras
        mPermissionName = bundle.getString(Utils.PERMISSION_NAME)
        permission_title.text = bundle.getString(Utils.PERMISSION_TITLE)
        permission_explain.text = bundle.getString(Utils.PERMISSION_EXPLAIN)
        permission_button.onClick {
            Utils.showLog("Go to system settings to set permission")

            Utils.showInstalledAppDetails(this@PermissionActivity)
        }

        Utils.mIsBackFromSetPermission = true
    }

    public override fun onResume() {
        super.onResume()
        Utils.showLog("PermissionActivity Resume")

        val hasCameraPermission = ContextCompat.checkSelfPermission(this@PermissionActivity,
                mPermissionName)
        if (hasCameraPermission == PackageManager.PERMISSION_GRANTED)
            finish()
    }

    public override fun onDestroy() {
        super.onDestroy()
        Utils.showLog("PermissionActivity destroyed")
    }
}