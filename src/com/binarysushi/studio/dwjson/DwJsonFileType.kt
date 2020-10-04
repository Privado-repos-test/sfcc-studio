package com.binarysushi.studio.dwjson

import com.binarysushi.studio.StudioIcons
import com.intellij.json.JsonLanguage
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

class DwJsonFileType : LanguageFileType(JsonLanguage.INSTANCE, true) {
    companion object {
        val INSTANCE = DwJsonFileType()
    }

    override fun getName(): String {
        return "DW_JSON"
    }

    override fun getDescription(): String {
        return "dw.json configuration file"
    }

    override fun getDefaultExtension(): String {
        TODO("Not yet implemented")
    }

    override fun getIcon(): Icon? {
        return StudioIcons.STUDIO_ICON
    }
}