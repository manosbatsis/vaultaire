
# Installation

## For Cordapps

Add to your Cordapp's Gradle dependencies:

```groovy

repositories {
    // For SNAPSHOT versions if needed
    maven { url "http://oss.sonatype.org/content/repositories/snapshots/" }
}

// Apply the kapt plugin
apply plugin: 'kotlin-kapt'

dependencies{
    // Core dependency
    cordaCompile "com.github.manosbatsis.vaultaire:vaultaire:$vaultaire_version"
    // Annotation processing
    kapt "com.github.manosbatsis.vaultaire:vaultaire-processor:$vaultaire_version"

    // Corda dependencies etc.
    // ...

}
```

You may also need to add the generated sources folder to your project

```groovy
// Define an extra sources variable
def generatedSourcesDir = project.file("build/generated/source/kaptKotlin/main")
// Tell Gradle about the extra source set
sourceSets {
    main {
        kotlin.srcDirs += generatedSourcesDir
    }
}

// Optional: Tell IntelliJ about the extra source set
idea {
    module {
        sourceDirs += generatedSourcesDir
        generatedSourceDirs += generatedSourcesDir
    }
} 
```

## For Client Apps

The core module can also be useful outside a cordapp, e.g. in a Spring application
interacting with Corda nodes via RPC:

```groovy
    // Core dependency
    compile "com.github.manosbatsis.vaultaire:vaultaire:$vaultaire_version"

    // Corda dependencies etc.
    // ...
```
