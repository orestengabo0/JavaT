package com.spring.JavaT.common.validation;

/**
 * Marker interfaces for Bean Validation groups.
 *
 * <p>Groups let you apply different validation rules to the same DTO depending
 * on the operation. The most common use case is Create vs Update: on create,
 * all fields are required; on update, only the fields being changed need to be
 * present and valid.
 *
 * <p>Usage on a DTO field:
 * <pre>
 * {@literal @}NotBlank(groups = ValidationGroups.OnCreate.class, message = ValidationMessages.EMAIL_REQUIRED)
 * private String email;
 * </pre>
 *
 * <p>Usage on a controller method:
 * <pre>
 * // Triggers only OnCreate constraints
 * {@literal @}PostMapping
 * public ResponseEntity&lt;?&gt; create(
 *         {@literal @}Validated(ValidationGroups.OnCreate.class) {@literal @}RequestBody UserRequest body) { ... }
 *
 * // Triggers only OnUpdate constraints
 * {@literal @}PatchMapping("/{id}")
 * public ResponseEntity&lt;?&gt; update(
 *         {@literal @}Validated(ValidationGroups.OnUpdate.class) {@literal @}RequestBody UserRequest body) { ... }
 * </pre>
 */
public final class ValidationGroups {

    private ValidationGroups() {}

    /** Applied when creating a new resource (POST). All required fields must be present. */
    public interface OnCreate {}

    /** Applied when fully replacing a resource (PUT). Same strictness as create. */
    public interface OnUpdate {}

    /** Applied when partially updating a resource (PATCH). Only provided fields are validated. */
    public interface OnPatch {}

    /** Applied when deleting a resource, if any request body validation is needed. */
    public interface OnDelete {}
}
