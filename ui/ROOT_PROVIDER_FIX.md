Magisk / root detection could not be patched directly from the uploaded zip because the implementation file referenced by the frontend is missing from this archive:
- RootControlProvider
- RootControlFailureReason

Observed call sites:
- app/src/main/java/com/folklore25/ghosthand/RuntimeStateStore.kt
- app/src/main/java/com/folklore25/ghosthand/PermissionsActivity.kt
- app/src/main/java/com/folklore25/ghosthand/LocalApiServer.kt

Recommended fix:
1. Determine actual root availability by attempting `su -c id` or `su -c true`, not by Magisk app package presence.
2. Use package detection only as a secondary hint for opening the manager UI.
3. Distinguish these states:
   - available: su command succeeded
   - authorization_required: su exists but command failed / denied
   - unavailable: no su binary / no root path available
4. Check multiple common manager package names if you need UI detection:
   - com.topjohnwu.magisk
   - io.github.vvb2060.magisk
   - com.topjohnwu.magisk.debug

Minimal behavior contract expected by current frontend:
- availability().status returns one of: available, authorization_required, unavailable
- availability().available is true only when status == available
