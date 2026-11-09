#ifndef ICU_SHIM_H
#define ICU_SHIM_H

#include <stdint.h>

/*
 * Minimal declarations for the NFKC normalizer of the system ICU4C
 * (libicucore, part of the iOS SDK). The ICU headers are not shipped with
 * the SDK, so only the parts that are used are declared here.
 */
typedef int UErrorCode;
typedef uint16_t UChar;

void *unorm2_getNFKCInstance(UErrorCode *pErrorCode);

int32_t unorm2_normalize(const void *norm2,
                         const UChar *src,
                         int32_t srcLength,
                         UChar *dest,
                         int32_t destCapacity,
                         UErrorCode *pErrorCode);

#endif /* ICU_SHIM_H */
