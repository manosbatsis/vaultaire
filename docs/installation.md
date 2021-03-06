
# Installation

### For Cordapps

Step 1: Add to your Cordapp's Gradle dependencies:

```groovy
// apply the kapt plugin
apply plugin: 'kotlin-kapt'
// Optional: for IntelliJ users, see also sourceSets bellow
apply plugin: "idea"

dependencies{
    // Core dependency
    cordaCompile "com.github.manosbatsis.vaultaire:vaultaire:$vaultaire_version"
    // Annotation processing
    kapt "com.github.manosbatsis.vaultaire:vaultaire-processor:$vaultaire_version"

    // Corda dependencies etc.
    // ...

}    
```

Alternatively, you might want to add Vaultaire in the Cordapp's fat JAR, 
in which case use `compile` instead of `cordacompile` and skip step 2 bellow.

Step 2: Add Vaultaire and Kotlin Utils as Cordapps to your deployNodes task:

```groovy

// Use Vaultaire for query DSL, DTOs and services generation
cordapp "com.github.manosbatsis.vaultaire:vaultaire:$vaultaire_version"
cordapp("com.github.manosbatsis.kotlin-utils:kotlin-utils-api:$kotlinutils_version")
```

Step 3: You may also want to add the generated sources to your build's `sourceSets` 

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

> Note that setting `kotlin.incremental=true` in gradle.properties may break non-clean builds earlier versions
> of kotlin like 1.2.71 - if you enable incremental then you'll need to `./gradlew clean` when building.

## For Client Apps

The core module can also be useful outside a cordapp, e.g. in a Spring application
interacting with Corda nodes via RPC:

```groovy
    // Core dependency
    compile "com.github.manosbatsis.vaultaire:vaultaire:$vaultaire_version"

    // Corda dependencies etc.
    // ...
```
