
# Changelog

The following sections describe major changes per version 
and can be helpful with version upgrades.


## 0.2 

- Renamed `VaultQueryDsl` annotation to `VaultaireGenerate`
- Renamed `StateService` to `BasicStateService`, added extended `StateService` type.
- The annotation processor will now generate a subclass of `StateService` per annotated `PersistentState`

## 0.1 

- Initial release
