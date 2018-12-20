/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.config;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.SmartList;
import com.intellij.util.io.URLUtil;
import kotlin.Pair;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.DefaultBuiltIns;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.js.JsBuiltInsPackageFragmentProvider;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.descriptors.NotFoundClasses;
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider;
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.incremental.components.LookupTracker;
import org.jetbrains.kotlin.js.resolve.JsPlatform;
import org.jetbrains.kotlin.load.kotlin.MetadataFinderFactory;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration;
import org.jetbrains.kotlin.serialization.deserialization.KotlinMetadataFinder;
import org.jetbrains.kotlin.serialization.js.*;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.utils.JsMetadataVersion;
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadata;
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils;

import java.io.File;
import java.util.*;

public class JsConfig {
    public static final String UNKNOWN_EXTERNAL_MODULE_NAME = "<unknown>";

    private static final String KOTLIN_STDLIB_MODULE_NAME = "kotlin";

    private final Project project;
    private final CompilerConfiguration configuration;
    private final LockBasedStorageManager storageManager = new LockBasedStorageManager("JsConfig");

    private final List<File> libraries;

    private final List<KotlinJavascriptMetadata> metadata = new SmartList<>();
    private final Set<KotlinJavascriptMetadata> friends = new HashSet<>();

    private List<ModuleDescriptorImpl> moduleDescriptors;
    private List<ModuleDescriptorImpl> friendModuleDescriptors;

    private final boolean loadBuiltInsFromStdlib;

    private boolean initialized = false;

    @Nullable
    private final List<JsModuleDescriptor<KotlinJavaScriptLibraryParts>> metadataCache;

    @Nullable
    private final Set<String> librariesToSkip;

    public JsConfig(@NotNull Project project, @NotNull CompilerConfiguration configuration, @NotNull List<File> libraries) {
        this(project, configuration, libraries, null, null, true);
    }

    /**
     * @param loadBuiltInsFromStdlib whether descriptors for built-in declarations ([kotlin.Any], [kotlin.Unit], ...) should be loaded from
     * `.kotlin_builtins` files from the standard library, as opposed to from `.kotlin_builtins` files from the compiler itself (which may
     * not correspond to the rest of the standard library in cases when compiler/stdlib versions do not match). The standard library is
     * defined as the first dependency module with the name `<kotlin>`. If there's no standard library in dependencies, built-ins are loaded
     * from the compiler anyway, even if [loadBuiltInsFromStdlib] is true.
     */
    public JsConfig(
            @NotNull Project project,
            @NotNull CompilerConfiguration configuration,
            @NotNull List<File> libraries,
            @Nullable List<JsModuleDescriptor<KotlinJavaScriptLibraryParts>> metadataCache,
            @Nullable Set<String> librariesToSkip,
            boolean loadBuiltInsFromStdlib
    ) {
        this.project = project;
        this.configuration = configuration.copy();
        this.libraries = libraries;
        this.metadataCache = metadataCache;
        this.librariesToSkip = librariesToSkip;
        this.loadBuiltInsFromStdlib = loadBuiltInsFromStdlib;
    }

    @NotNull
    public CompilerConfiguration getConfiguration() {
        return configuration;
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    @NotNull
    public List<File> getLibraries() {
        return libraries;
    }

    @NotNull
    public String getModuleId() {
        return configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME);
    }

    @NotNull
    public ModuleKind getModuleKind() {
        return configuration.get(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN);
    }

    @NotNull
    public String getSourceMapPrefix() {
        return configuration.get(JSConfigurationKeys.SOURCE_MAP_PREFIX, "");
    }

    @NotNull
    public List<String> getSourceMapRoots() {
        return configuration.get(JSConfigurationKeys.SOURCE_MAP_SOURCE_ROOTS, Collections.emptyList());
    }

    public boolean shouldGenerateRelativePathsInSourceMap() {
        return getSourceMapPrefix().isEmpty() && getSourceMapRoots().isEmpty();
    }

    @NotNull
    public SourceMapSourceEmbedding getSourceMapContentEmbedding() {
        return configuration.get(JSConfigurationKeys.SOURCE_MAP_EMBED_SOURCES, SourceMapSourceEmbedding.INLINING);
    }

    @NotNull
    public List<String> getFriends() {
        if (getConfiguration().getBoolean(JSConfigurationKeys.FRIEND_PATHS_DISABLED)) return Collections.emptyList();
        return getConfiguration().getList(JSConfigurationKeys.FRIEND_PATHS);
    }

    @NotNull
    public LanguageVersionSettings getLanguageVersionSettings() {
        return CommonConfigurationKeysKt.getLanguageVersionSettings(configuration);
    }

    public boolean isAtLeast(@NotNull LanguageVersion expected) {
        LanguageVersion actual = CommonConfigurationKeysKt.getLanguageVersionSettings(configuration).getLanguageVersion();
        return actual.getMajor() > expected.getMajor() ||
               actual.getMajor() == expected.getMajor() && actual.getMinor() >= expected.getMinor();
    }


    public static abstract class Reporter {
        public void error(@NotNull String message) { /*Do nothing*/ }

        public void warning(@NotNull String message) { /*Do nothing*/ }
    }

    public boolean checkLibFilesAndReportErrors(@NotNull JsConfig.Reporter report) {
        if (libraries.isEmpty()) {
            return false;
        }

        VirtualFileSystem fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL);
        VirtualFileSystem jarFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.JAR_PROTOCOL);

        Set<String> modules = new HashSet<>();

        boolean skipMetadataVersionCheck = getLanguageVersionSettings().getFlag(AnalysisFlags.getSkipMetadataVersionCheck());

        for (File file : libraries) {
            String path = file.getPath();
            if (librariesToSkip != null && librariesToSkip.contains(path)) continue;

            if (!file.exists()) {
                report.error("Path '" + path + "' does not exist");
                return true;
            }

            VirtualFile virtualFile =
                    path.endsWith(".jar") || path.endsWith(".zip")
                    ? jarFileSystem.findFileByPath(path + URLUtil.JAR_SEPARATOR)
                    : fileSystem.findFileByPath(path);

            if (virtualFile == null) {
                report.error("File '" + path + "' does not exist or could not be read");
                return true;
            }

            List<KotlinJavascriptMetadata> metadataList = KotlinJavascriptMetadataUtils.loadMetadata(path);
            if (metadataList.isEmpty()) {
                report.warning("'" + path + "' is not a valid Kotlin Javascript library");
                continue;
            }

            Set<String> moduleNames = new LinkedHashSet<>();

            for (KotlinJavascriptMetadata metadata : metadataList) {
                if (!metadata.getVersion().isCompatible() && !skipMetadataVersionCheck) {
                    report.error("File '" + path + "' was compiled with an incompatible version of Kotlin. " +
                                 "The binary version of its metadata is " + metadata.getVersion() +
                                 ", expected version is " + JsMetadataVersion.INSTANCE);
                    return true;
                }

                moduleNames.add(metadata.getModuleName());
            }

            for (String moduleName : moduleNames) {
                if (!modules.add(moduleName)) {
                    report.warning("Module \"" + moduleName + "\" is defined in more than one file");
                }
            }

            if (modules.contains(getModuleId())) {
                report.warning("Module \"" + getModuleId() + "\" depends on module with the same name");
            }

            Set<String> friendLibsSet = new HashSet<>(getFriends());
            metadata.addAll(metadataList);
            if (friendLibsSet.contains(path)){
                friends.addAll(metadataList);
            }
        }

        initialized = true;
        return false;
    }

    @NotNull
    public List<ModuleDescriptorImpl> getModuleDescriptors() {
        init();
        return moduleDescriptors;
    }

    @NotNull
    private Pair<List<ModuleDescriptorImpl>, List<ModuleDescriptorImpl>> createModuleDescriptors() {
        LanguageVersionSettings languageVersionSettings = CommonConfigurationKeysKt.getLanguageVersionSettings(configuration);

        // TODO: in case loadBuiltInsFromStdlib is true and stdlibMetadataEntry is null, report error on any builtins usage
        KotlinJavascriptMetadata stdlibMetadataEntry =
                CollectionsKt.firstOrNull(metadata, entry -> entry.getModuleName().equals(KOTLIN_STDLIB_MODULE_NAME));
        KotlinBuiltIns builtIns =
                loadBuiltInsFromStdlib && stdlibMetadataEntry != null ? new DefaultBuiltIns(false) : JsPlatform.INSTANCE.getBuiltIns();

        List<ModuleDescriptorImpl> moduleDescriptors = new SmartList<>();
        List<ModuleDescriptorImpl> friendModuleDescriptors = new SmartList<>();
        for (KotlinJavascriptMetadata metadataEntry : metadata) {
            assert metadataEntry.getVersion().isCompatible() ||
                   languageVersionSettings.getFlag(AnalysisFlags.getSkipMetadataVersionCheck()) :
                    "Expected JS metadata version " + JsMetadataVersion.INSTANCE +
                    ", but actual metadata version is " + metadataEntry.getVersion();

            ModuleDescriptorImpl descriptor = createModuleDescriptor(
                    metadataEntry.getModuleName(),
                    KotlinJavascriptSerializationUtil.readModuleAsProto(metadataEntry.getBody(), metadataEntry.getVersion()),
                    builtIns
            );

            moduleDescriptors.add(descriptor);

            if (friends.contains(metadataEntry)) {
                friendModuleDescriptors.add(descriptor);
            }

            if (metadataEntry == stdlibMetadataEntry) {
                builtIns.setBuiltInsModule(descriptor);
            }
        }

        if (metadataCache != null) {
            for (JsModuleDescriptor<KotlinJavaScriptLibraryParts> cached : metadataCache) {
                moduleDescriptors.add(createModuleDescriptor(cached.getName(), cached.getData(), builtIns));
            }
        }

        List<ModuleDescriptorImpl> dependencies =
                loadBuiltInsFromStdlib && stdlibMetadataEntry != null
                ? moduleDescriptors
                : CollectionsKt.plus(moduleDescriptors, builtIns.getBuiltInsModule());
        for (ModuleDescriptorImpl module : moduleDescriptors) {
            module.setDependencies(dependencies);
        }

        return new Pair<>(Collections.unmodifiableList(moduleDescriptors), Collections.unmodifiableList(friendModuleDescriptors));
    }

    @NotNull
    public List<ModuleDescriptorImpl> getFriendModuleDescriptors() {
        init();
        return friendModuleDescriptors;
    }

    private void init() {
        if (!initialized) {
            JsConfig.Reporter reporter = new Reporter() {
                @Override
                public void error(@NotNull String message) {
                    throw new IllegalStateException(message);
                }
            };

            checkLibFilesAndReportErrors(reporter);
        }

        if (moduleDescriptors == null) {
            Pair<List<ModuleDescriptorImpl>, List<ModuleDescriptorImpl>> descriptors = createModuleDescriptors();
            moduleDescriptors = descriptors.getFirst();
            friendModuleDescriptors = descriptors.getSecond();
        }
    }

    @NotNull
    private ModuleDescriptorImpl createModuleDescriptor(
            @NotNull String moduleName,
            @NotNull KotlinJavaScriptLibraryParts parts,
            @NotNull KotlinBuiltIns builtIns
    ) {
        ModuleDescriptorImpl module = new ModuleDescriptorImpl(Name.special("<" + moduleName + ">"), storageManager, builtIns);

        LanguageVersionSettings languageVersionSettings = CommonConfigurationKeysKt.getLanguageVersionSettings(configuration);
        LookupTracker lookupTracker = configuration.get(CommonConfigurationKeys.LOOKUP_TRACKER, LookupTracker.DO_NOTHING.INSTANCE);
        PackageFragmentProvider provider = KotlinJavascriptPackageFragmentProviderKt.createKotlinJavascriptPackageFragmentProvider(
                storageManager, module, parts.getHeader(), parts.getBody(), parts.getMetadataVersion(),
                new CompilerDeserializationConfiguration(languageVersionSettings), lookupTracker
        );

        KotlinMetadataFinder finder =
                ServiceManager.getService(project, MetadataFinderFactory.class).create(GlobalSearchScope.allScope(project));

        if (moduleName.equals(KOTLIN_STDLIB_MODULE_NAME)) {
            module.initialize(new CompositePackageFragmentProvider(Arrays.asList(
                    provider,
                    new JsBuiltInsPackageFragmentProvider(
                            storageManager, finder, module, new NotFoundClasses(storageManager, module),
                            new CompilerDeserializationConfiguration(languageVersionSettings)
                    )
            )));
        } else {
            module.initialize(provider);
        }

        return module;
    }
}
