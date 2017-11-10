package com.tsbridge.fragment

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cn.bmob.v3.BmobUser
import cn.bmob.v3.datatype.BmobFile
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.SaveListener
import cn.bmob.v3.listener.UploadFileListener
import com.tsbridge.R
import com.tsbridge.activity.NetworkActivity
import com.tsbridge.activity.PermissionActivity
import com.tsbridge.entity.Bulletin
import com.tsbridge.utils.Utils
import kotlinx.android.synthetic.main.send_fragment.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import java.io.File

class SendFragment : Fragment(), View.OnClickListener {
    private val SELECT_PIC_LOW = 121
    private val SELECT_PIC_KITKAT = 122

    private var mSendName = ""
    private var mSendContent = ""
    private var mSendImageUri: Uri? = null

    private var mIsBackFromNetwork = false
    private var mIsBackFromPermission = false

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?)
            : View? {
        Utils.showLog("SendFragment onCreateView")

        return inflater.inflate(R.layout.send_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Utils.showLog("SendFragment onActivityCreated")

//        initialization()

        send_image_sel.onClick {
            selectImageBtn()
        }
        send_image_del.onClick {
            clearImageBtn()
        }
        send_btn.onClick {
            sendBulletinBtn()
        }
    }

//    private fun initialization() {
//        send_image_sel.setOnClickListener(this@SendFragment)
//        send_image_del.setOnClickListener(this@SendFragment)
//        send_btn.setOnClickListener(this@SendFragment)
//    }

    override fun onClick(view: View) {
        val id = view.id
        when (id) {
            R.id.send_image_sel -> selectImageBtn()
            R.id.send_image_del -> clearImageBtn()
            R.id.send_btn -> sendBulletinBtn()
            else -> {
            }
        }
    }

    /**
     * 选择图片: Intent.ACTION_GET_CONTENT、ACTION_OPEN_DOCUMENT、ACTION_PICK
     * 相机拍照：MediaStore.ACTION_IMAGE_CAPTURE
     */
    private fun selectImageBtn() {
        var storagePermission = Utils.checkPermission(activity,
                Utils.EXTERNAL_STORAGE_PERMISSION)
        if (storagePermission != PackageManager.PERMISSION_GRANTED) {
            Utils.showLog("Has not External storage Permission")

            var permissionAccessTimes = Utils.getPermissionAccessTimes(activity,
                    Utils.EXTERNAL_STORAGE_PERMISSION_ACCESS_TIMES_KEY,
                    1)
            /** 用户拒绝了权限申请并选择了不再显示 */
            if (permissionAccessTimes > 1 &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity,
                            Utils.EXTERNAL_STORAGE_PERMISSION)) {
                mIsBackFromPermission = true
                val intent = Intent(activity, PermissionActivity::class.java)
                intent.putExtra(Utils.PERMISSION_TITLE,
                        getString(R.string.permission_external_storage_title))
                intent.putExtra(Utils.PERMISSION_EXPLAIN,
                        getString(R.string.permission_external_storage_explain))
                intent.putExtra(Utils.PERMISSION_NAME, Utils.EXTERNAL_STORAGE_PERMISSION)
                startActivity(intent)
                return
            }
            if (permissionAccessTimes == 1) {
                permissionAccessTimes++
                Utils.setPermissionAccessTimes(activity,
                        Utils.EXTERNAL_STORAGE_PERMISSION_ACCESS_TIMES_KEY,
                        permissionAccessTimes)
            }
            Utils.showLog("Show system dialog to allow access storage")

            requestPermissions(arrayOf(Utils.EXTERNAL_STORAGE_PERMISSION),
                    Utils.REQUEST_CODE_ASK_EXTERNAL_STORAGE_PERMISSION)
            return
        }
        selectImage()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            Utils.REQUEST_CODE_ASK_EXTERNAL_STORAGE_PERMISSION ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Utils.showLog("Manifest.permission.WRITE_EXTERNAL_STORAGE access succeed")

                    selectImage()
                } else {
                    Utils.showLog("Manifest.permission.WRITE_EXTERNAL_STORAGE access failed")
                    Utils.showToast(activity, activity.getString(R.string.permission_denied))
                }
            else ->
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    fun selectImage() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            var intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            startActivityForResult(intent, SELECT_PIC_KITKAT)
        } else {
            var intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, SELECT_PIC_LOW)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        Utils.showLog("Back to onActivityResult from getting image")

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                SELECT_PIC_KITKAT, SELECT_PIC_LOW -> {
                    mSendImageUri = intent?.data
                    if (mSendImageUri != null)
                        imagePreview(mSendImageUri!!)
                    else
                        Utils.showLog("Uri with returned intent is null")
                }
                else -> {
                }
            }
        } else {
            Utils.showLog("No selected image")
            Utils.showToast(activity, activity.getString(R.string.no_selected_image))
        }
    }

    private fun imagePreview(uri: Uri) {
        var picturePath = Utils.getPath(activity, uri)
        Utils.showLog(picturePath)
        Utils.setImageToView(activity, null, picturePath, send_image)
    }

    private fun clearImageBtn() {
        if (mSendImageUri != null) {
            mSendImageUri = null
            send_image.setImageResource(R.drawable.black)
        }
    }

    /** 添加数据到云上之前先进行网络判断 */
    private fun sendBulletinBtn() {
        if (Utils.isNetWorkConnected(activity))
            insertItem()
        else {
            mIsBackFromNetwork = true
            startActivity(Intent(activity, NetworkActivity::class.java))
        }
    }

    private fun insertItem() {
        /** 发送者名称需要从其用户信息中读取，故只有注册用户才能发送 */
        if (BmobUser.getCurrentUser() != null)
            mSendName = BmobUser.getCurrentUser().username
        mSendContent = send_content.text.toString()
        if (TextUtils.isEmpty(mSendName)) {
            Utils.showToast(activity, activity.getString(R.string.sender_info))
            return
        } else if (TextUtils.isEmpty(mSendContent) && mSendImageUri == null) {
            Utils.showToast(activity, activity.getString(R.string.no_inputted_content))
            return
        }
        if (TextUtils.isEmpty(mSendContent))
            mSendContent = activity.getString(R.string.no_content_text)
        if (mSendImageUri != null) {
            /** 获取路径一定要用 Utils 中定义的方法，如果使用 uri.path 不同 SDK 结果不同 */
            val file = BmobFile(File(Utils.getPath(activity, mSendImageUri!!)))
            file.uploadblock(object : UploadFileListener() {
                override fun done(e: BmobException?) {
                    if (e == null) {
                        Utils.showLog("Upload image succeed")

                        insertItemToBulletin(file)
                    } else {
                        Utils.showLog("Upload image failed: " + e.message + " Error code: " + e.errorCode)
                        Utils.showToast(activity,
                                activity.getString(R.string.send_failed))
                    }
                }
            })
        } else
            insertItemToBulletin(null)
    }

    fun insertItemToBulletin(file: BmobFile?) {
        val bulletin = Bulletin(mSendName, mSendContent, file)
        bulletin.save(object : SaveListener<String>() {
            override fun done(objectId: String, e: BmobException?) {
                if (e == null) {
                    Utils.showLog("Insert bulletin succeed: " + objectId)
                    Utils.showToast(activity,
                            activity.getString(R.string.send_succeed))
                } else {
                    Utils.showLog("Insert bulletin failed: " + e.message + " Error code: " + e.errorCode)
                    Utils.showToast(activity,
                            activity.getString(R.string.send_failed))
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        Utils.showLog("SendFragment onResume")

        /** 新打开的 Activity 在当前 onResume 执行后才开始创建 */
        if (mIsBackFromNetwork && Utils.mIsBackFromSetNetwork) {
            mIsBackFromNetwork = false
            Utils.mIsBackFromSetNetwork = false
            if (Utils.isNetWorkConnected(activity))
                insertItem()
            else
                Utils.showToast(activity,
                        activity.getString(R.string.no_connected_network))
        }

        if (mIsBackFromPermission && Utils.mIsBackFromSetPermission) {
            mIsBackFromPermission = false
            Utils.mIsBackFromSetPermission = false
            var storagePermission = Utils.checkPermission(activity,
                    Utils.EXTERNAL_STORAGE_PERMISSION)
            if (storagePermission == PackageManager.PERMISSION_GRANTED)
                selectImage()
            else {
                Utils.showLog("Storage permission access failed")
                Utils.showToast(activity, activity.getString(R.string.permission_denied))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Utils.showLog("SendFragment onDestroyView")
    }
}