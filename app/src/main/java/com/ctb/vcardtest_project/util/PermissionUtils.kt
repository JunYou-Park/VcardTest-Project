package com.ctb.vcardtest_project.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat


object PermissionUtils {
    private const val TAG = "PermissionUtils"
    const val PERMISSIONS_REQUEST_CODE = 1000
    const val CODE_WRITE_SETTINGS_PERMISSION = 999
    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    fun hasRequiredPermission(context: Context): Boolean{
        return hasPermissions(context, REQUIRED_PERMISSIONS)
    }

    fun hasBasicPermissions(context: Context, smsPermissions: Array<String>, extPermissions: Array<String>): Boolean {
        for (permission in smsPermissions) {
            if (!hasPermission(context, permission)) {
                return false
            }
        }
        for (permission in extPermissions) {
            if (!hasPermission(context, permission)) {
                return false
            }
        }
        return true
    }

    /**
     * Check whether have the permissions given
     *
     * @return true or false
     */
    fun hasPermissions(context: Context, strPermissions: Array<String>): Boolean {
        for (permission in strPermissions) {
            if (!hasPermission(context, permission)) {
                return false
            }
        }
        return true
    }

    /**
     * Check whether we have the runtime permission given, suppose api level is
     * 23 or higher
     *
     * @return true or false
     */
    fun hasPermission(context: Context, strPermission: String): Boolean {
        val state: Int = context.checkSelfPermission(strPermission)
        return state == PackageManager.PERMISSION_GRANTED
    }

    fun checkSelfPermission(context: Context, permission: String) : Boolean{
        if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return true
    }
    fun checkSelfPermissions(context: Context, permissions: Array<String>) : Boolean{
        for(permission in permissions){
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
}