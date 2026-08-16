package io.github.khezyapp.dhttp.auth.oauth2;

/**
 * The supported OAuth2 grant flows ({@code R10}).
 */
public enum OAuth2Grant {
    AUTHORIZATION_CODE,
    CLIENT_CREDENTIALS,
    PASSWORD,
    REFRESH_TOKEN
}
