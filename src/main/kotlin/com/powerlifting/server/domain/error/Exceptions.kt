package com.powerlifting.server.domain.error

/**
 * Thrown by repositories / use cases when a requested resource does not exist
 * (either at all, or not for the caller). Mapped to HTTP 404 in [Application.module]'s
 * StatusPages — the response body intentionally does not echo internal details
 * so it does not leak the existence of resources owned by other users.
 */
class NotFoundException(message: String) : RuntimeException(message)

/**
 * Thrown by the auth interceptor when the caller's Firebase token is valid but
 * their email is not yet verified. Mapped to HTTP 403.
 */
class EmailNotVerifiedException(message: String = "Email not verified") : RuntimeException(message)

/** Нет заголовка Authorization, он не Bearer, или Firebase отверг токен. */
class UnauthorizedException(message: String) : RuntimeException(message)
