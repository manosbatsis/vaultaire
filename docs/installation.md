
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
