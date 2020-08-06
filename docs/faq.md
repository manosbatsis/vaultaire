
# Frequently Asked Questions

Answers to common questions.

## License: Can I use Vaultaire with my project?

Yes, Vaultaire can be used as a library/dependency with no side-effect to your project.
Vaultaire is distributed under the GNU __Lesser__ General Public License or LGPL, 
the same license adopted by Corda dependencies like Hibernate. 

## Use with Corda Enterprise

To use Vaultaire with Corda Enterprise, you will have to update your build to 
[use the CE release]((https://docs.corda.r3.com/app-upgrade-notes-enterprise.html#re-compiling-for-release)).

After switching to the appropriate `corda_release_group` and `corda_release_version` in your `ext` section, you can 
instruct your build to substitute _transitive_ Corda OS dependencies with their CE equivalents: 

```groovy

allprojects {
    //...
    configurations {
        all {
            //...
            resolutionStrategy {
                // ...
                eachDependency { DependencyResolveDetails details ->
                    // Exclude from substitutions as appropriate, e.g.
                    def exclusions = ['corda-finance-contracts']
                    // Substitute the rest, assumes `ext.corda_release_group` and `ext.corda_release_version` are set
                    if (details.requested.group ==  "net.corda" && !exclusions.contains(details.requested.name)) {
                        // Force Corda Enterprise
                        details.useTarget  group:  corda_release_group, name: details.requested.name, version: corda_release_version
                    }
                }
            }
        }
    }
}
```

> __Note__: The above assumes `ext.corda_release_group` and `ext.corda_release_version` are already set, e.g. to 
>`com.r3.corda` and `4.2` respectively.