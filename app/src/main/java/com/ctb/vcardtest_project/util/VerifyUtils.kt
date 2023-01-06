package com.ctb.vcardtest_project.util

import android.text.TextUtils
import android.util.Patterns
import java.util.regex.Matcher
import java.util.regex.Pattern


object VerifyUtils {
    var WAPPUSH = "Browser Information" // Wap push key
    private val NUMERIC_CHARS_SUGAR = arrayListOf<Char>(
        '-', '.', ',', '(', ')', ' ', '/', '\\', '*', '#', '+'
    )
    val NAME_ADDR_EMAIL_PATTERN: Pattern =
        Pattern.compile("\\s*(\"[^\"]*\"|[^<>\"]+)\\s*<([^<>]+)>\\s*")
    /**
     * Returns true if the address passed in is a Browser wap push MMS address.
     */
    fun isWapPushNumber(address: String): Boolean {
        return if (TextUtils.isEmpty(address)) {
            false
        } else {
            address.contains(WAPPUSH)
        }
    }

    fun isEnglishLetterString(name: String?): Boolean {
        return if (name != null && name.isNotEmpty()) {
            isEnglishLetter(name[0])
        } else false
    }

    private fun isEnglishLetter(c: Char): Boolean {
        return c in 'A'..'Z' || c in 'a'..'z'
    }

    fun isKoreanLetterString(name: String?): Boolean {
        return if (name != null && name.isNotEmpty()) {
            isKoreanLetter(name[0])
        } else false
    }

    private fun isKoreanLetter(c: Char): Boolean {
        return c in 'ㄱ'..'힇'
    }

    /**
     * Returns true if the address is an email address
     * @param address the input address to be tested
     * @return true if address is an email address
     */
    fun isEmailAddress(address: String): Boolean {
        if (TextUtils.isEmpty(address)) {
            return false
        }
        val s: String = extractAddrSpec(address) ?: address
        val match: Matcher = Patterns.EMAIL_ADDRESS.matcher(s)
        return match.matches()
    }


    fun extractAddrSpec(address: String): String? {
        val match: Matcher = NAME_ADDR_EMAIL_PATTERN.matcher(address)
        return if (match.matches()) { match.group(2)
        } else address
    }
}