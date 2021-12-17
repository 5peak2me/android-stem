package com.likethesalad.placeholder.modules.common.helpers.dirs

import com.likethesalad.placeholder.providers.AndroidExtensionProvider
import com.likethesalad.placeholder.providers.BuildDirProvider
import com.likethesalad.tools.android.plugin.data.AndroidVariantData
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.io.File

class VariantBuildResolvedDir @AssistedInject constructor(
    buildDirProvider: BuildDirProvider,
    androidExtensionProvider: AndroidExtensionProvider,
    @Assisted androidVariantData: AndroidVariantData
) {
    @AssistedFactory
    interface Factory {
        fun create(androidVariantData: AndroidVariantData): VariantBuildResolvedDir
    }

    companion object {
        const val RESOLVED_DIR_BUILD_RELATIVE_PATH = "generated/resolved"
    }

    private val androidExtension by lazy { androidExtensionProvider.getExtension() }
    private val variantName by lazy { androidVariantData.getVariantName() }

    val resolvedDir: File by lazy {
        val dir = File(buildDirProvider.getBuildDir(), "$RESOLVED_DIR_BUILD_RELATIVE_PATH/$variantName")
        addResolvedDirToSourceSets(dir)
        dir
    }


    private fun addResolvedDirToSourceSets(resolvedDir: File) {
        val variantSrcDirs = androidExtension.getVariantSrcDirs(variantName)
        androidExtension.setVariantSrcDirs(variantName, variantSrcDirs + resolvedDir)
    }
}
