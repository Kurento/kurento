/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

#include "CertificateManager.hpp"
#include <openssl/ssl.h>
#include <openssl/err.h>
#include <gst/gst.h>

#define GST_CAT_DEFAULT kurento_certificate_manager
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoCertificateManager"

namespace kurento
{

static std::string
parametersToPEMString (EC_GROUP *ec_group)
{
  BIO *temp_memory_bio = BIO_new (BIO_s_mem() );

  if (!temp_memory_bio) {
    GST_ERROR ("Failed to allocate temporary memory bio");
    return "";
  }

  if (!PEM_write_bio_ECPKParameters (temp_memory_bio, ec_group) ) {
    GST_ERROR ("Failed to write key parameters");
    BIO_free (temp_memory_bio);
    return "";
  }

  BIO_write (temp_memory_bio, "\0", 1);
  char *buffer;
  BIO_get_mem_data (temp_memory_bio, &buffer);
  std::string parameters_str = buffer;
  BIO_free (temp_memory_bio);

  return parameters_str;
}

static std::string
ECDSAKeyToPEMString (EC_KEY *pkey_)
{
  BIO *temp_memory_bio = BIO_new (BIO_s_mem() );

  if (!temp_memory_bio) {
    GST_ERROR ("Failed to allocate temporary memory bio");
    return "";
  }

  if (!PEM_write_bio_ECPrivateKey (
        temp_memory_bio, pkey_, nullptr, nullptr, 0, nullptr, nullptr) ) {
    GST_ERROR ("Failed to write ECDSA key");
    BIO_free (temp_memory_bio);
    return "";
  }

  BIO_write (temp_memory_bio, "\0", 1);
  char *buffer;
  BIO_get_mem_data (temp_memory_bio, &buffer);
  std::string priv_key_str = buffer;
  BIO_free (temp_memory_bio);

  return priv_key_str;
}

static std::string
privateKeyToPEMString (EVP_PKEY *pkey_)
{
  BIO *temp_memory_bio = BIO_new (BIO_s_mem() );

  if (!temp_memory_bio) {
    GST_ERROR ("Failed to allocate temporary memory bio");
    return "";
  }

  if (!PEM_write_bio_PrivateKey (
        temp_memory_bio, pkey_, nullptr, nullptr, 0, nullptr, nullptr) ) {
    GST_ERROR ("Failed to write private key");
    BIO_free (temp_memory_bio);
    return "";
  }

  BIO_write (temp_memory_bio, "\0", 1);
  char *buffer;
  BIO_get_mem_data (temp_memory_bio, &buffer);
  std::string priv_key_str = buffer;
  BIO_free (temp_memory_bio);

  return priv_key_str;
}

static gchar *
generateCertificate (EVP_PKEY *private_key)
{
  X509 *x509 = NULL;
  BIO *bio = NULL;
  X509_NAME *name = NULL;
  int rc = 0;
  unsigned long err = 0;
  BUF_MEM *mem = NULL;
  gchar *pem = NULL;

  x509 = X509_new ();

  if (x509 == NULL) {
    GST_ERROR ("X509 not created");
    goto end;
  }

  X509_set_version (x509, 2L);
  ASN1_INTEGER_set (X509_get_serialNumber (x509), 0);
  X509_gmtime_adj (X509_get_notBefore (x509), 0);
  X509_gmtime_adj (X509_get_notAfter (x509), 31536000L);  /* A year */
  X509_set_pubkey (x509, private_key);

  name = X509_get_subject_name (x509);
  X509_NAME_add_entry_by_txt (name, "C", MBSTRING_ASC, (unsigned char *) "SE",
                              -1, -1, 0);
  X509_NAME_add_entry_by_txt (name, "CN", MBSTRING_ASC,
                              (unsigned char *) "Kurento", -1, -1, 0);
  X509_set_issuer_name (x509, name);
  name = NULL;

  if (!X509_sign (x509, private_key, EVP_sha256 () ) ) {
    GST_ERROR ("Failed to sign certificate");
    goto end;
  }

  bio = BIO_new (BIO_s_mem () );

  if (bio == NULL) {
    GST_ERROR ("BIO not created");
    goto end;
  }

  rc = PEM_write_bio_X509 (bio, x509);

  if (rc != 1) {
    err = ERR_get_error();
    GST_ERROR ("PEM_write_bio_X509 failed, error %ld", err);
    goto end;
  }

  BIO_get_mem_ptr (bio, &mem);

  if (!mem || !mem->data || !mem->length) {
    err = ERR_get_error();
    GST_ERROR ("BIO_get_mem_ptr failed, error %ld", err);
    goto end;
  }

  pem = g_strndup (mem->data, mem->length);

end:

  if (x509 != NULL) {
    X509_free (x509);
  }

  if (bio != NULL) {
    BIO_free_all (bio);
  }

  return pem;
}

std::string
CertificateManager::generateRSACertificate ()
{
  RSA *rsa = NULL;
  EVP_PKEY *private_key = NULL;
  gchar *pem;
  std::string rsaKey;
  std::string certificateRSA;

  rsa = RSA_generate_key (2048, RSA_F4, NULL, NULL);

  if (rsa == NULL) {
    GST_ERROR ("RSA not created");
    goto end;
  }

  private_key = EVP_PKEY_new ();

  if (private_key == NULL) {
    GST_ERROR ("Private key not created");
    goto end;
  }

  if (EVP_PKEY_assign_RSA (private_key, rsa) == 0) {
    GST_ERROR ("Private key not assigned");
    goto end;
  }

  rsa = NULL;

  pem = generateCertificate (private_key);

  if (pem == NULL) {
    GST_WARNING ("Certificate not generated");
    goto end;
  }

  rsaKey = privateKeyToPEMString (private_key);
  certificateRSA = rsaKey + std::string (pem);
  g_free (pem);

end:

  if (rsa != NULL) {
    RSA_free (rsa);
  }

  if (private_key != NULL) {
    EVP_PKEY_free (private_key);
  }

  return certificateRSA;
}

std::string
CertificateManager::generateECDSACertificate ()
{
  EC_KEY *ec_key = NULL;
  EC_GROUP *group = NULL;
  EVP_PKEY *private_key = NULL;
  gchar *pem;
  std::string ecdsaParameters, ecdsaKey;
  std::string certificateECDSA;

  ec_key = EC_KEY_new ();

  if (ec_key == NULL) {
    GST_ERROR ("EC key not created");
    goto end;
  }

  group = EC_GROUP_new_by_curve_name (NID_X9_62_prime256v1);
  EC_GROUP_set_asn1_flag (group, OPENSSL_EC_NAMED_CURVE);

  if (ec_key == NULL) {
    GST_ERROR ("EC group not created");
    goto end;
  }

  if (EC_KEY_set_group (ec_key, group) == 0) {
    GST_ERROR ("Group not set to key");
    goto end;
  }

  if (EC_KEY_generate_key (ec_key) == 0) {
    GST_ERROR ("EC key not generated");
    goto end;
  }

  private_key = EVP_PKEY_new ();

  if (private_key == NULL) {
    GST_ERROR ("Private key not created");
    goto end;
  }

  if (EVP_PKEY_assign_EC_KEY (private_key, ec_key) == 0) {
    GST_ERROR ("Private key not assigned");
    goto end;
  }

  pem = generateCertificate (private_key);

  if (pem == NULL) {
    GST_WARNING ("Certificate not generated");
    goto end;
  }

  ecdsaKey = ECDSAKeyToPEMString (ec_key);
  ec_key = NULL;
  ecdsaParameters = parametersToPEMString (group);

  certificateECDSA = ecdsaParameters + ecdsaKey + std::string (pem);
  g_free (pem);

end:

  if (ec_key != NULL) {
    EC_KEY_free (ec_key);
  }

  if (private_key != NULL) {
    EVP_PKEY_free (private_key);
  }

  if (group != NULL) {
    EC_GROUP_free (group);
  }

  return certificateECDSA;
}

CertificateManager::StaticConstructor CertificateManager::staticConstructor;

CertificateManager::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

}
