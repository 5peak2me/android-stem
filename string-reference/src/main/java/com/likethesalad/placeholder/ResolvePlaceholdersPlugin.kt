package com.likethesalad.placeholder

import com.likethesalad.android.string.resources.locator.StringResourceLocatorPlugin
import com.likethesalad.placeholder.di.AppInjector
import com.likethesalad.placeholder.locator.ResolvedXmlSourceFilterRule
import com.likethesalad.placeholder.locator.TemplateDirsXmlSourceFilterRule
import com.likethesalad.placeholder.modules.common.helpers.android.AndroidVariantContext
import com.likethesalad.placeholder.modules.common.helpers.dirs.VariantBuildResolvedDir
import com.likethesalad.placeholder.modules.resolveStrings.ResolvePlaceholdersTask
import com.likethesalad.placeholder.modules.resolveStrings.data.ResolvePlaceholdersArgs
import com.likethesalad.placeholder.modules.templateStrings.GatherTemplatesTask
import com.likethesalad.placeholder.modules.templateStrings.data.GatherTemplatesArgs
import com.likethesalad.placeholder.providers.AndroidExtensionProvider
import com.likethesalad.placeholder.providers.LanguageResourceFinderProvider
import com.likethesalad.placeholder.providers.ProjectDirsProvider
import com.likethesalad.placeholder.providers.TaskProvider
import com.likethesalad.placeholder.utils.TaskActionProviderHolder
import com.likethesalad.tools.android.plugin.data.AndroidExtension
import com.likethesalad.tools.android.plugin.extension.AndroidToolsPluginExtension
import com.likethesalad.tools.resource.locator.android.extension.ResourceLocatorExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.LogLevel
import java.io.File

@Suppress("UnstableApiUsage")
class ResolvePlaceholdersPlugin : Plugin<Project>, AndroidExtensionProvider, ProjectDirsProvider,
    TaskProvider {

    companion object {
        const val RESOLVE_PLACEHOLDERS_TASKS_GROUP_NAME = "resolver"
        const val EXTENSION_NAME = "stringXmlReference"
    }

    private lateinit var project: Project
    private lateinit var extension: PlaceholderExtension
    private lateinit var androidExtension: AndroidExtension
    private lateinit var stringsLocatorExtension: ResourceLocatorExtension

    override fun apply(project: Project) {
        if (!project.plugins.hasPlugin("com.android.application")) {
            throw IllegalStateException("The strings placeholder resolver can only be applied to Android Application projects")
        }
        this.project = project
        AppInjector.init(this)
        project.plugins.apply(StringResourceLocatorPlugin::class.java)
        androidExtension = project.extensions.getByType(AndroidToolsPluginExtension::class.java).androidExtension
        extension = project.extensions.create(EXTENSION_NAME, PlaceholderExtension::class.java)
        stringsLocatorExtension = project.extensions.getByType(ResourceLocatorExtension::class.java)
        val taskActionProviderHolder = AppInjector.getTaskActionProviderHolder()
        val androidVariantContextFactory = AppInjector.getAndroidVariantContextFactory()

        stringsLocatorExtension.onResourceLocatorTaskCreated { taskContainer ->
            val languageResourceFinderProvider = LanguageResourceFinderProvider(
                taskContainer.outputDirProvider,
                stringsLocatorExtension
            )
            createResolvePlaceholdersTaskForVariant(
                androidVariantContextFactory.create(
                    taskContainer.taskContext.variantTree,
                    stringsLocatorExtension.getResourceSerializer()
                ),
                taskActionProviderHolder, languageResourceFinderProvider,
                extension.resolveOnBuild.get()
            )
        }
        checkForDeprecatedConfigs()
    }

    private fun checkForDeprecatedConfigs() {
        if (extension.keepResolvedFiles != null) {
            showDeprecatedConfigurationWarning("keepResolvedFiles")
        }
        if (extension.useDependenciesRes != null) {
            showDeprecatedConfigurationWarning("useDependenciesRes")
        }
    }

    private fun createResolvePlaceholdersTaskForVariant(
        androidVariantContext: AndroidVariantContext,
        taskActionProviderHolder: TaskActionProviderHolder,
        languageResourceFinderProvider: LanguageResourceFinderProvider,
        resolveOnBuild: Boolean
    ) {
        val gatherTemplatesActionProvider = taskActionProviderHolder.gatherTemplatesActionProvider
        val resolvePlaceholdersActionProvider = taskActionProviderHolder.resolvePlaceholdersActionProvider
        addLocatorExclusionRules(androidVariantContext)

        val gatherTemplatesTask = project.tasks.register(
            androidVariantContext.tasksNames.gatherStringTemplatesName,
            GatherTemplatesTask::class.java,
            GatherTemplatesArgs(
                gatherTemplatesActionProvider.provide(androidVariantContext),
                languageResourceFinderProvider
            )
        )

        gatherTemplatesTask.configure {
            it.group = RESOLVE_PLACEHOLDERS_TASKS_GROUP_NAME
        }

        val resolvePlaceholdersTask = project.tasks.register(
            androidVariantContext.tasksNames.resolvePlaceholdersName,
            ResolvePlaceholdersTask::class.java,
            ResolvePlaceholdersArgs(resolvePlaceholdersActionProvider.provide(androidVariantContext))
        )

        resolvePlaceholdersTask.configure {
            it.group = RESOLVE_PLACEHOLDERS_TASKS_GROUP_NAME
            it.templatesDir.set(gatherTemplatesTask.flatMap { gatherTemplates -> gatherTemplates.outDir })
            it.outputDir.set(androidVariantContext.variantBuildResolvedDir.resolvedDir)
        }

        if (resolveOnBuild) {
            androidVariantContext.mergeResourcesTask.dependsOn(resolvePlaceholdersTask)
        }
    }

    private fun addLocatorExclusionRules(androidVariantContext: AndroidVariantContext) {
        val locatorConfiguration =
            stringsLocatorExtension.getConfiguration(androidVariantContext.androidVariantData.getVariantName())

        val templatesExclusionRule = TemplateDirsXmlSourceFilterRule(androidVariantContext)
        val resolvedStringsExclusionRule =
            ResolvedXmlSourceFilterRule("${project.buildDir}/${VariantBuildResolvedDir.RESOLVED_DIR_BUILD_RELATIVE_PATH}")

        locatorConfiguration.addSourceFilterRule(templatesExclusionRule)
        locatorConfiguration.addSourceFilterRule(resolvedStringsExclusionRule)
    }

    override fun getExtension(): AndroidExtension {
        return androidExtension
    }

    override fun getProjectDir(): File {
        return project.projectDir
    }

    override fun getBuildDir(): File {
        return project.buildDir
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Task> findTaskByName(name: String): T {
        return project.tasks.findByName(name) as T
    }

    private fun showDeprecatedConfigurationWarning(name: String) {
        project.logger.log(
            LogLevel.WARN,
            "'$name' is deprecated and will be removed in the next version"
        )
    }
}