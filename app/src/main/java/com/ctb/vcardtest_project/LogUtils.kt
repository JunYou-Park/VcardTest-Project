package com.ctb.vcardtest_project

import android.util.Log

object LogUtils {
    fun logD(TAG: String, msg: String) {
        Log.d("확인", "$TAG -> $msg")
    }

    fun logV(TAG: String, msg: String){
        Log.v("확인", "$TAG -> $msg")
    }

    fun logI(TAG: String, msg: String){
        Log.i("확인", "$TAG -> $msg")
    }

    fun logW(TAG: String, msg: String) {
        Log.w("확인", "$TAG -> $msg")
    }

    fun logE(TAG: String, msg: String, e: Exception?) {
        Log.e("확인", "$TAG -> $msg", e)
    }
}