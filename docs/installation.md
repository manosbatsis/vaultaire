
# Installation


Add to your Cordapp's Gradle dependencies:

```groovy
// apply the kapt plugin
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

The core module can also be useful outside a cordapp, e.g. in a Spring application
interacting with Corda nodes via RPC:

```groovy
    // Core dependency
    compile "com.github.manosbatsis.vaultaire:vaultaire:$vaultaire_version"

    // Corda dependencies etc.
    // ...
```

## Customizing Output Location

In case you'd like to keep your contracts JAR light, you can have Vaultaire 
generate code to another Gradle module, e.g. the flows JAR. Here's a quick eample.

First, go to your contracts module and tell Vaultaire to generate code in the flows module:

```groovy
apply plugin: 'kotlin-kapt'
// ...

kapt{
    arguments {
        arg("kapt.kotlin.vaultaire.generated", 
        rootProject.file("flows-module/build/generated/source/kaptKotlin/main").absolutePath)
    }
}

```

Then, update your flows module to add the additional source set:

```groovy
// Optional: for IntelliJ users, see also the optioanl `idea` section bellow
apply plugin: "idea"
//...

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

That's it! The generated classes will now be included in your flows JAR.