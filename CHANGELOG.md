
# Changelog

The following sections describe major changes per version 
and can be helpful with version upgrades.

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
