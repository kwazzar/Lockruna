package com.kwazzar.lockruna.server.identity.domain

/**
 * Raised by the repository when the unique email index rejects a registration that raced another one.
 */
class EmailAlreadyRegisteredException : RuntimeException("Email already registered")
