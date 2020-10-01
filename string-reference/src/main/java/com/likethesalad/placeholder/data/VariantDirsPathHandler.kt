package com.likethesalad.placeholder.data

import com.likethesalad.placeholder.utils.AppVariantHelper

class VariantDirsPathHandler(
    private val appVariantHelper: AppVariantHelper
) {

    val variantDirsPathResolver: VariantDirsPathResolver by lazy {
        VariantDirsPathResolver(appVariantHelper)//todo use factory
    }

    companion object {
        const val BASE_DIR_PATH = "main"
    }

    fun isBase(dirPath: String): Boolean {
        return dirPath == BASE_DIR_PATH
    }

    fun isVariantType(dirPath: String): Boolean {
        return dirPath == appVariantHelper.getVariantType()
    }

    fun getHighestFrom(dirPaths: Set<String>): String {
        var highest = ""

        for (dirPath in variantDirsPathResolver.pathList.reversed()) {
            if (dirPath in dirPaths) {
                highest = dirPath
                break
            }
        }

        return highest
    }

    fun getOutputFrom(offset: String): String {
        return if (!hasFlavors()) {
            offset
        } else {
            when {
                isBase(offset) -> variantDirsPathResolver.pathList[1]
                isVariantType(offset) -> variantDirsPathResolver.pathList.last()
                else -> offset
            }
        }
    }

    private fun hasFlavors(): Boolean {
        return variantDirsPathResolver.pathList.size > 2
    }
}
