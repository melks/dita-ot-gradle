package com.github.eerohele

import org.gradle.api.Project
import org.gradle.api.file.FileCollection

import org.gradle.api.internal.ClassPathRegistry
import org.gradle.api.internal.DefaultClassPathProvider
import org.gradle.api.internal.DefaultClassPathRegistry
import org.gradle.api.internal.classpath.DefaultModuleRegistry
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.project.antbuilder.DefaultIsolatedAntBuilder
import org.gradle.api.internal.project.IsolatedAntBuilder
import org.gradle.internal.classloader.DefaultClassLoaderFactory
import org.gradle.internal.installation.CurrentGradleInstallation

import org.gradle.util.GradleVersion

import org.apache.tools.ant.BuildException

class AntBuilderAssistant {
    private static final ThreadLocal<IsolatedAntBuilder> THREAD_LOCAL_ANT_BUILDER = new ThreadLocal<IsolatedAntBuilder>()

    private static FileCollection getClasspath(Project project) {
        project.fileTree(dir: project.ditaOt.dir).matching {
            include(
                'resources/',
                'lib/**/*.jar',
                'plugins/org.dita.pdf2/lib/fo.jar',
                'plugins/org.dita.pdf2/build/libs/fo.jar'
            )

            exclude(
                'lib/ant-launcher.jar',
                'lib/ant.jar'
            )
        }
    }

    protected static IsolatedAntBuilder makeAntBuilder(Project project) {
        FileCollection classpath = getClasspath(project)
        ModuleRegistry moduleRegistry
        DefaultIsolatedAntBuilder antBuilder

        if (classpath == null) {
            throw new BuildException(DitaOtPlugin.MESSAGES.classpathError)
        }

        if (GradleVersion.current() >= GradleVersion.version('2.13')) {
            moduleRegistry = new DefaultModuleRegistry(CurrentGradleInstallation.get())
        } else {
            moduleRegistry = new DefaultModuleRegistry()
        }

        ClassPathRegistry classPathRegistry = new DefaultClassPathRegistry(new DefaultClassPathProvider(moduleRegistry))

        if (GradleVersion.current() > GradleVersion.version('2.13')) {
            antBuilder = new DefaultIsolatedAntBuilder(classPathRegistry, new DefaultClassLoaderFactory(), moduleRegistry)
        } else {
            antBuilder = new DefaultIsolatedAntBuilder(classPathRegistry, new DefaultClassLoaderFactory())
        }

        antBuilder.execute {
            classpath*.toURI()*.toURL()*.each {
                antProject.getClass().getClassLoader().addURL(it)
            }
        }

        antBuilder
    }

    protected static IsolatedAntBuilder getAntBuilder(Project project) {
        IsolatedAntBuilder antBuilder = THREAD_LOCAL_ANT_BUILDER.get()

        if (antBuilder == null) {
            antBuilder = makeAntBuilder(project)
            THREAD_LOCAL_ANT_BUILDER.set(antBuilder)
        }

        antBuilder
    }
}
