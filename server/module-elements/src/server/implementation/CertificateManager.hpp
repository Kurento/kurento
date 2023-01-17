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


#ifndef __CERTIFICATE_MANAGER_HPP__
#define __CERTIFICATE_MANAGER_HPP__

#include <string>

namespace kurento
{
class CertificateManager
{
public:
  static std::string generateRSACertificate ();
  static std::string generateECDSACertificate ();
  static bool isCertificateValid (std::string certificate);

private:
  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;
};
}

#endif /* __CERTIFICATE_MANAGER_HPP__ */
