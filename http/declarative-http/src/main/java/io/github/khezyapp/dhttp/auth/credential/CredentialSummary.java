package io.github.khezyapp.dhttp.auth.credential;

/**
 * Listing view of a credential — id + type only, never secret values (§6.2).
 *
 * @param id   the credential id
 * @param type the credential type
 */
public record CredentialSummary(String id, String type) {
}
