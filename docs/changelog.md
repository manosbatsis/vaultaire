
# Changelog

The following sections describe major changes per version 
and can be helpful with version upgrades.

## 0.26-27

- Bumped Corda to OS 4.5
- Refactored to support RPC connection pooling based on 
[Corda RPC PoolBoy](https://github.com/manosbatsis/corda-rpc-poolboy)

## 0.25

- Bumped Corda to OS 4.4
- Added support for "lite" DTO strategy
- Added Query DSL root `externalIds` property to  support querying the vault by Corda Account(s)

## 0.24

- Added default parameter value `false` to `VaultQueryCriteriaCondition.toCriteria(boolean)`, so that 
aggregates are __not__ ignored by default.

## 0.23

- Fixed corda API design issue, where a secondary `VaultCustomQueryCriteria`'s implicit/default status of 
UNCONSUMED (VS null) is applied, overriding the root DSL status

## 0.22

Bumped kotlin-utils, fixing issue with DTO generation including `companion object` members of the source type.

## 0.21

- Bumped KotlinPoet, util versions
- Added `@DefaultValue` example in the docs

## 0.20

- Added `CordaSerializable` annotation to generated DTOs
- Added `ignoreProperties` to `VaultaireGenerateDto` and `VaultaireGenerateDtoForDependency`, 
useful with derived/backed properties like `participants` 

## 0.19

- Added [NodeRpcConnection] interface and [StateServiceRpcConnectionDelegate]
for libraries in need of more flexible [StateServiceDelegate] integration 
e.g. Corbeans/Spring or other IoC containers 

## 0.18

Bumped deps i.e. Corda to OS 4.3

## 0.17

- Added support for sorting based on standard attributes and aliases of those, 
i.e. enum values of `Sort.CommonStateAttribute`, `Sort.VaultStateAttribute`, 
`Sort.LinearStateAttribute` and  `Sort.FungibleStateAttribute`.

## 0.16

- Added `copyAnnotationPackages` property to `VaultaireGenerateDto` and `VaultaireGenerateDtoForDependency`. 
The property can be used to define a list of base packages for matching annotations to be copied automatically 
from a source `ContractState` to it's generated DTO. This is useful when replication of e.g. Jackson or Bean Validation 
annotations can be useful.

## 0.15

- Replaced `com.github.manosbatsis.vaultaire.dto.Dto` 
and `com.github.manosbatsis.vaultaire.util.DtoInsufficientStateMappingException` with   
[kotlin-utils](https://github.com/manosbatsis/kotlin-utils) equivalents extracted there. 
You may need to update package/method names in your code if you make use of generated DTOs.

## 0.14

- Vaultaire's `*ForDependency` annotations now use the annotated element's package (suffixed with `.generated`) 
for output. This leaves the original (state) packages exclusive to their modules if needed, avoiding when 
cordapp package conflicts while testing. 

## 0.13

- Add support for `final` responder flow supertypes in `VaultaireGenerateResponder` 

## 0.12

- Fixed dependency scope for kotlin-utils

## 0.11

- Added `VaultaireGenerateDto`, `VaultaireGenerateDtoForDependency` annotations for generating DTOs for contract states
- Added `VaultaireGenerateResponder` annotation for generating responder flows extending common supertypes 
- Refactored to extract [kotlin-utils](https://github.com/manosbatsis/kotlin-utils)

## 0.10

- Added `@VaultaireGenerateForDependency` to support generation (of DSL, Services etc.) for states contributed to the 
classpath by project dependencies 
- Enhanced generated subclasses of `ExtendedStateService` with DSL-aware `buildQuery`, `queryBy` and `trackBy`
- Fixed typo from `VaultaireGenerate.constractStateType` to  `VaultaireGenerate.contractStateType`

## 0.9

- 0.8 re-release fix

## 0.8

- Allow annotation processing to generate code to another Gradle module or location
using the `kapt.kotlin.vaultaire.generated` kapt argument, see installation docs.

## 0.7

- Fixed, improved and added tests for `StateService` get/find by id/externalId methods
- `StateService` get by id/externalId will throw a `StateNotFoundException` when no match is found

## 0.6

- Added support for [aggregate functions](https://github.com/manosbatsis/vaultaire/issues/2). 
- Added `getByExternalId` and `findByExternalId` methods to `StateService`
- Added optional `Vault.RelevancyStatus` parameter to `StateService` `getByLinearId` and `findByLinearId` methods

## 0.5 

- Removed param from `String.asUniqueIdentifier()`

## 0.4 

- Fixed [handling of nullable fields](https://github.com/manosbatsis/vaultaire/issues/8)
- Added string input signatures for `getByLinearId`, `findByLinearId`

## 0.3 

- Refactored `StateService` to an interface

## 0.2 

- Renamed `VaultQueryDsl` annotation to `VaultaireGenerate`
- Renamed `StateService` to `BasicStateService`, added extended `StateService` type.
- The annotation processor will now generate a subclass of `StateService` per annotated `PersistentState`

## 0.1 

- Initial release
