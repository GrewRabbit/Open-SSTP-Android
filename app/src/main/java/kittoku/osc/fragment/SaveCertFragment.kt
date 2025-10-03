/**
 * 文件：app/src/main/java/kittoku/osc/fragment/SaveCertFragment.kt
 * 作用说明：
 * 本文件定义了 SaveCertFragment，用于将 VPN 证书以用户指定的文件名保存到本地存储。该 Fragment 会弹出系统文件保存对话框，用户选择保存位置后，将证书内容写入指定文件。
 * 主要功能包括：启动系统文档创建器、处理保存结果、将证书字节流写入文件、完成后关闭当前 Activity。
 * 在本软件项目中，证书导入与保存相关模块（如证书管理界面、配置导入流程）会调用该文件，用于实现证书的本地保存功能。
 * 本文件会调用 kittoku.osc.activity.EXTRA_KEY_CERT（获取证书内容）、kittoku.osc.activity.EXTRA_KEY_FILENAME（获取文件名）等常量。
 * 本文件依赖标准库 Activity、Intent、BufferedOutputStream 及 AndroidX Preference、ActivityResultContracts 库，不主动调用其他业务模块。
 */

package kittoku.osc.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.PreferenceFragmentCompat
import kittoku.osc.R
import kittoku.osc.activity.EXTRA_KEY_CERT
import kittoku.osc.activity.EXTRA_KEY_FILENAME
import java.io.BufferedOutputStream

/**
 * 证书保存 Fragment，负责弹出保存对话框并写入证书文件
 * @param givenIntent 包含证书内容和文件名的 Intent
 */
internal class SaveCertFragment(private val givenIntent: Intent) : PreferenceFragmentCompat() {
    /**
     * 启动系统文档创建器并处理保存结果
     */
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) result.data?.data?.also { uri ->
            // 写入证书内容到用户指定的文件
            BufferedOutputStream(requireContext().contentResolver.openOutputStream(uri, "w")).also {
                it.write(givenIntent.getByteArrayExtra(EXTRA_KEY_CERT))
                it.flush()
                it.close()
            }
        }
        // 保存完成后关闭当前 Activity
        requireActivity().finish()
    }

    /**
     * 初始化界面并弹出保存证书的系统对话框
     */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.blank_preference, rootKey)

        // 构造保存证书的 Intent，设置文件类型和默认文件名
        Intent(Intent.ACTION_CREATE_DOCUMENT).also {
            it.addCategory(Intent.CATEGORY_OPENABLE)
            it.setType("application/x-x509-ca-cert")
            it.putExtra(Intent.EXTRA_TITLE, givenIntent.getStringExtra(EXTRA_KEY_FILENAME))

            // 启动保存流程
            launcher.launch(it)
        }
    }
}