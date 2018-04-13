/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_semux_crypto_Native */

#ifndef _Included_org_semux_crypto_Native
#define _Included_org_semux_crypto_Native
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_semux_crypto_Native
 * Method:    h256
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_semux_crypto_Native_h256
  (JNIEnv *, jclass, jbyteArray);

/*
 * Class:     org_semux_crypto_Native
 * Method:    h160
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_semux_crypto_Native_h160
  (JNIEnv *, jclass, jbyteArray);

/*
 * Class:     org_semux_crypto_Native
 * Method:    sign
 * Signature: ([B[B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_semux_crypto_Native_sign
  (JNIEnv *, jclass, jbyteArray, jbyteArray);

/*
 * Class:     org_semux_crypto_Native
 * Method:    verify
 * Signature: ([B[B[B)Z
 */
JNIEXPORT jboolean JNICALL Java_org_semux_crypto_Native_verify
  (JNIEnv *, jclass, jbyteArray, jbyteArray, jbyteArray);

#ifdef __cplusplus
}
#endif
#endif
