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
#include <gst/gst.h>
#include <openssl/bn.h>
#include <openssl/err.h>
#include <openssl/rsa.h>
#include <openssl/ssl.h>
#include <memory>

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

static std::string
generateCertificate (EVP_PKEY *private_key)
{
  std::shared_ptr <X509> x509;
  std::shared_ptr <BIO> bio;
  X509_NAME *name = nullptr;
  int rc = 0;
  unsigned long err = 0;
  BUF_MEM *mem;
  std::string pem;

  x509 = std::shared_ptr<X509> (X509_new (),
  [] (X509 * obj) {
    X509_free (obj);
  });

  if (x509 == nullptr) {
    GST_ERROR ("X509 not created");
    return pem;
  }

  X509_set_version (x509.get(), 2L);
  ASN1_INTEGER_set (X509_get_serialNumber (x509.get() ), 0);
  X509_gmtime_adj (X509_get_notBefore (x509.get() ), 0);
  X509_gmtime_adj (X509_get_notAfter (x509.get() ), 31536000L); /* A year */
  X509_set_pubkey (x509.get(), private_key);

  name = X509_get_subject_name (x509.get() );
  X509_NAME_add_entry_by_txt (name, "C", MBSTRING_ASC, (unsigned char *) "SE",
                              -1, -1, 0);
  X509_NAME_add_entry_by_txt (name, "CN", MBSTRING_ASC,
                              (unsigned char *) "Kurento", -1, -1, 0);
  X509_set_issuer_name (x509.get(), name);
  name = nullptr;

  if (!X509_sign (x509.get(), private_key, EVP_sha256 () ) ) {
    GST_ERROR ("Failed to sign certificate");
    return pem;
  }

  bio = std::shared_ptr<BIO> (BIO_new (BIO_s_mem () ),
  [] (BIO * obj) {
    BIO_free_all (obj);
  });

  if (bio == nullptr) {
    GST_ERROR ("BIO not created");
    return pem;
  }

  rc = PEM_write_bio_X509 (bio.get(), x509.get() );

  if (rc != 1) {
    err = ERR_get_error();
    GST_ERROR ("PEM_write_bio_X509 failed, error %ld", err);
    return pem;
  }

  BIO_get_mem_ptr (bio.get(), &mem);

  if (!mem || !mem->data || !mem->length) {
    err = ERR_get_error();
    GST_ERROR ("BIO_get_mem_ptr failed, error %ld", err);
    return pem;
  }

  pem = std::string (mem->data, mem->length);

  return pem;
}

std::string
CertificateManager::generateRSACertificate ()
{
  std::shared_ptr <EVP_PKEY> private_key;
  std::string pem;
  std::string rsaKey;
  std::string certificateRSA;

  BIGNUM *e = BN_new();
  BN_set_word(e, RSA_F4);
  RSA *rsa = RSA_new();

  // May need CRYPTO_cleanup_all_ex_data() at the end of the program
  int rc = RSA_generate_key_ex(rsa, 2048, e, NULL);
  BN_free(e);
  if (rc == 0) {
    GST_ERROR ("RSA_generate_key_ex(): %s",
        ERR_reason_error_string (ERR_get_error ()));
    return certificateRSA;
  }

  private_key = std::shared_ptr <EVP_PKEY> (EVP_PKEY_new (),
  [] (EVP_PKEY * obj) {
    EVP_PKEY_free (obj);
  });

  if (private_key == nullptr) {
    GST_ERROR ("Private key not created");
    return certificateRSA;
  }

  // Takes ownership of 'rsa'
  if (EVP_PKEY_assign_RSA (private_key.get(), rsa) == 0) {
    GST_ERROR ("Private key not assigned");
    return certificateRSA;
  }

  pem = generateCertificate (private_key.get() );

  if (pem.empty () ) {
    GST_WARNING ("Certificate not generated");
    return certificateRSA;
  }

  rsaKey = privateKeyToPEMString (private_key.get() );
  certificateRSA = rsaKey + pem;

  return certificateRSA;
}

std::string
CertificateManager::generateECDSACertificate ()
{
  EC_KEY *ec_key;
  std::shared_ptr <EC_GROUP> group;
  std::shared_ptr <EVP_PKEY> private_key;
  std::string pem;
  std::string ecdsaParameters, ecdsaKey;
  std::string certificateECDSA;

  ec_key = EC_KEY_new ();

  if (ec_key == nullptr) {
    GST_ERROR ("EC key not created");
    return certificateECDSA;
  }

  group = std::shared_ptr <EC_GROUP> (EC_GROUP_new_by_curve_name (
                                        NID_X9_62_prime256v1),
  [] (EC_GROUP * obj) {
    EC_GROUP_free (obj);
  });
  EC_GROUP_set_asn1_flag (group.get(), OPENSSL_EC_NAMED_CURVE);

  if (group == nullptr) {
    EC_KEY_free (ec_key);
    GST_ERROR ("EC group not created");
    return certificateECDSA;
  }

  if (EC_KEY_set_group (ec_key, group.get() ) == 0) {
    EC_KEY_free (ec_key);
    GST_ERROR ("Group not set to key");
    return certificateECDSA;
  }

  if (EC_KEY_generate_key (ec_key) == 0) {
    EC_KEY_free (ec_key);
    GST_ERROR ("EC key not generated");
    return certificateECDSA;
  }

  private_key = std::shared_ptr<EVP_PKEY> (EVP_PKEY_new (),
  [] (EVP_PKEY * obj) {
    EVP_PKEY_free (obj);
  });

  if (private_key == nullptr) {
    EC_KEY_free (ec_key);
    GST_ERROR ("Private key not created");
    return certificateECDSA;
  }

  if (EVP_PKEY_assign_EC_KEY (private_key.get(), ec_key) == 0) {
    EC_KEY_free (ec_key);
    GST_ERROR ("Private key not assigned");
    return certificateECDSA;
  }

  pem = generateCertificate (private_key.get() );

  if (pem.empty () ) {
    GST_WARNING ("Certificate not generated");
    return certificateECDSA;
  }

  ecdsaKey = ECDSAKeyToPEMString (ec_key);
  ec_key = nullptr;
  ecdsaParameters = parametersToPEMString (group.get() );

  certificateECDSA = ecdsaParameters + ecdsaKey + pem;

  return certificateECDSA;
}

bool
CertificateManager::isCertificateValid (std::string certificate)
{
  std::shared_ptr <BIO> bio;
  std::shared_ptr <X509> x509;
  std::shared_ptr <EVP_PKEY> private_key;

  bio = std::shared_ptr<BIO>
        (BIO_new_mem_buf ( (gpointer) certificate.c_str (), -1),
  [] (BIO * obj) {
    BIO_free_all (obj);
  });

  if (!bio) {
    return false;
  }

  x509 = std::shared_ptr<X509>(
      PEM_read_bio_X509(bio.get(), nullptr, nullptr, nullptr),
      [](X509 *obj) { X509_free(obj); });

  if (!x509) {
    return false;
  }

  (void) BIO_reset (bio.get() );
  private_key = std::shared_ptr<EVP_PKEY>(
      PEM_read_bio_PrivateKey(bio.get(), nullptr, nullptr, nullptr),
      [](EVP_PKEY *obj) { EVP_PKEY_free(obj); });

  if (!private_key) {
    return false;
  }

  return true;
}

CertificateManager::StaticConstructor CertificateManager::staticConstructor;

CertificateManager::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

}
