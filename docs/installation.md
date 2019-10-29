
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

sourceSets {
    main {
        kotlin.srcDirs += project.file("build/generated/source/kaptKotlin/main")
    }
}
```