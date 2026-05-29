# Canvas API Key Security Controls

This document describes the controls implemented to reduce risk for Canvas API keys at rest and in transit through application components.

## Threat Model Focus

Canvas API keys are high-impact secrets. Disclosure could enable unauthorized access to student data (FERPA-sensitive records), roster information, and course operations. The primary control objective is to prevent plaintext persistence in the database while preserving legitimate operational use.

## Implemented Controls

### 1. Encryption at rest for stored Canvas API keys

- Canvas API keys are now encrypted before being written to the `COURSE.CANVAS_API_TOKEN` column.
- Encryption uses **AES-GCM** (`AES/GCM/NoPadding`) with:
  - per-token random 96-bit IV
  - 128-bit authentication tag
  - Base64 payload encoding and versioned prefix (`enc:v1:`)
- AES-GCM provides confidentiality and integrity protection; tampered ciphertext fails decryption.

Implementation:
- `CanvasApiTokenSecurityService.encrypt(...)`
- `CanvasApiTokenSecurityService.decrypt(...)`

### 2. Runtime key management via environment configuration

- Encryption key material is externalized through:
  - `CANVAS_API_TOKEN_ENCRYPTION_KEY` (Base64-encoded AES key, recommended 256-bit)
  - mapped in Spring as `app.canvas.api-token-encryption-key`
- No key is hardcoded in source control.
- Encryption attempts fail closed when key configuration is missing or malformed.

### 3. Secure handling in application flow

- Token write path:
  - `CoursesController.postCourse(...)`
  - `CoursesController.updateCourseWithCanvasToken(...)`
  - both now encrypt prior to repository save.
- Token use path:
  - `CanvasService` decrypts tokens only at request construction time before setting the `Authorization: ****** header for Canvas GraphQL calls.
- Token display path:
  - `CoursesController.getCourseCanvasInfo(...)` decrypts then returns a masked representation (not plaintext).

### 4. Backward compatibility for existing rows

- Decryption logic supports legacy plaintext values that predate encryption rollout.
- Legacy tokens are still readable for continuity, while all newly saved tokens use encrypted form.

### 5. Schema hardening for ciphertext storage

- `COURSE.CANVAS_API_TOKEN` column width increased from `VARCHAR(255)` to `VARCHAR(1024)` to safely store encrypted payloads and future format/version overhead.

## Validation and Assurance

- New unit tests validate:
  - encrypt/decrypt roundtrip correctness
  - key-required failure behavior
  - legacy plaintext read compatibility
- Controller tests verify encryption service integration on update paths.

## Operational Guidance

1. Generate a strong AES key (32 bytes recommended) and Base64-encode it.
2. Set `CANVAS_API_TOKEN_ENCRYPTION_KEY` in deployment secret management (not in repository files).
3. Restrict access to this key to application runtime principals only.
4. Rotate keys under institutional key management policy; if rotation with re-encryption is needed, perform via controlled migration.

## Residual Risk and Additional Recommendations

- Existing plaintext rows remain plaintext until they are updated and re-saved; consider a one-time migration job to re-encrypt all legacy values.
- Database administrators with direct write access could still alter ciphertext (detected at decrypt time) or replace values.
- Further hardening options:
  - key rotation framework with explicit key IDs
  - KMS/HSM-backed envelope encryption
  - audit logging for key-related configuration changes and decryption failures
