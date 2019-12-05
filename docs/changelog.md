
# Changelog

The following sections describe major changes per version 
and can be helpful with version upgrades.

## 0.11

- Refactored to extract [kotlinpoet-utils](https://github.com/manosbatsis/kotlinpoet-utils)

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
