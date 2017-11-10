package com.tsbridge.entity

import cn.bmob.v3.BmobUser
import cn.bmob.v3.datatype.BmobFile
import com.tsbridge.utils.Utils

/**
 * teacherName String 发布者名称
 * bulletinContent String 文本内容
 * bulletinImage BmobFile 图片内容
 * 注：在类外部有可能访问成员变量的，需要声明为 var/val，否则访问不到
 * 如 SendFragment 类中调用 Bmob 方法保存 Bulletin 对象(存入云数据库)时，要访问对应的列键-值
 */
data class User(val imageFile: BmobFile?) : BmobUser() {
    init {
        Utils.showLog("Create a User object")

        /** 如果类名与表名不一致，这里指定表名即可 */
        tableName = "_User"
    }
}
