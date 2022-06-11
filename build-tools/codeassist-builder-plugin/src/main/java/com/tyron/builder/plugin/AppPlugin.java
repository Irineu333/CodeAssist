package com.tyron.builder.plugin;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Plugin;

public class AppPlugin implements Plugin<Project> {

    @Override
    public void apply(Project target) {
        String message = "CodeAssist does not yet support the Android Gradle Plugin." +
                         " It is currently being worked on but it is not yet available for use."  +
                         "The only supported plugin for now is the `java` plugin which ise used " +
                         "for building JVM applications";
        throw new GradleException(message);
    }
}
