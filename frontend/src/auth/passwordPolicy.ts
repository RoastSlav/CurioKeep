export const MIN_PASSWORD_LENGTH = 10
/** bcrypt, which the server hashes with, cannot use more than 72 bytes. */
export const MAX_PASSWORD_BYTES = 72

/** Why a new password would be rejected, or null when the server will accept it. Mirrors the server's rule. */
export function passwordProblem(password: string): string | null {
    if ([...password].length < MIN_PASSWORD_LENGTH) {
        return `Password must be at least ${MIN_PASSWORD_LENGTH} characters`
    }
    if (new TextEncoder().encode(password).length > MAX_PASSWORD_BYTES) {
        return `Password is too long (at most ${MAX_PASSWORD_BYTES} bytes)`
    }
    return null
}
